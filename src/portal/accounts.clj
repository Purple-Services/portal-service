(ns portal.accounts
  (:require [bouncer.core :as b]
            [crypto.password.bcrypt :as bcrypt]
            [common.config :as config]
            [common.db :as db]
            [common.util :as util]
            [common.sendgrid :as sendgrid]
            [portal.db :refer [raw-sql-query]]
            [portal.users :as users]))

(defn manager-account
  "Given a user-id, return the account-id asociated with the account"
  [user-id]
  (:account_id (first (db/!select (db/conn) "account_managers" [:account_id]
                                  {:user_id user-id}))))

(defn account-name-exists?
  "Is there already an account with this name?"
  [account-name]
  (boolean (db/!select (db/conn) "accounts" [:id] {:name account-name})))

(defn account-children-sql
  [account-id]
  (str "SELECT " users/users-select " FROM `users` "
       "JOIN account_children ON "
       "account_children.user_id = users.id "
       "WHERE account_children.account_id = '" account-id "';"))

(defn account-managers-sql
  [account-id]
  (str "SELECT " users/users-select " FROM `users` "
       "JOIN account_managers ON "
       "account_managers.user_id = users.id "
       "WHERE account_managers.account_id = '" account-id "';"))

(defn account-users
  "Given an account-id, return all users associated with this account"
  [account-id]
  (let [account-children (->> (raw-sql-query
                               (db/conn)
                               [(account-children-sql account-id)])
                              (map #(assoc % :is-manager false)))
        account-managers (->> (raw-sql-query
                               (db/conn)
                               [(account-managers-sql account-id)])
                              (map #(assoc % :is-manager true)))]
    (->> (concat account-children account-managers)
         (map #(users/process-user %)))))

(defn get-user
  "Given a user-id and an account-id, return the user if the user-id
  is the account manager"
  [manager-id account-id user-id]
  (if (users/manages-account? manager-id account-id)
    (users/get-user user-id)
    {:success false
     :message "User does not manage that account"}))

(defn account-users-response
  "Given a user-id and an account-id, return all users associated with
  account. If user-id is not an account-manager of that account, return
  an error message"
  [account-id manager-id]
  (if (users/manages-account? manager-id account-id)
    (account-users account-id)
    {:success false
     :message "User does not manage that account"}))

(defn get-account-by-name
  "Given an account name, return the id associated with that account"
  [account-name]
  (first (db/!select (db/conn) "accounts" [:id :name] {:name account-name})))

(defn create-account!
  "Given an account-name, create it in the database"
  [account-name]
  ;; successful: {:success true}
  ;; unsucessful: {:success false, :message <message>}
  (db/!insert (db/conn) "accounts"
              {:id (util/rand-str-alpha-num 20)
               :name account-name}))

(defn associate-account-manager!
  "Given a user-id and account-id, associate user-id with account-id in
  account_managers table"
  [user-id account-id]
  (db/!insert (db/conn) "account_managers"
              {:user_id user-id
               :account_id account-id}))

(defn associate-child-account!
  "Given a user-id and account-id, associate user-id with account-id in
  account_children table"
  [user-id account-id]
  (db/!insert (db/conn) "account_children"
              {:user_id user-id
               :account_id account-id}))

(defn create-child-account!
  "Create a new child account"
  [{:keys [db-conn new-user manager-id account-id]}]
  ;; make sure this user actually manages the account
  (cond (not (users/manages-account? manager-id account-id))
        {:success false
         :message "User does not manage that account"}
        (not (b/valid? new-user users/child-account-validations))
        {:success false
         :validation (b/validate new-user users/child-account-validations)}
        :else
        (let [{:keys [email full-name]} new-user
              new-user-id (util/rand-str-alpha-num 20)
              reset-key (util/rand-str-alpha-num 22)]
          ;; register a user with a blank password
          ;; will not be able to login without resetting
          ;; password
          (db/!insert db-conn "users"
                      {:id new-user-id
                       :email email
                       :type "native"
                       :password_hash ""
                       :reset_key reset-key
                       :phone_number ""
                       :phone_number_verified 0
                       :name full-name})
          ;; add the user to account_children
          (associate-child-account! new-user-id account-id)
          ;; send an email to the user
          (sendgrid/send-template-email
           email
           "Welcome to Purple" ;; ignored by template
           "test" ;; ignored by template
           :template-id "e93dbfd4-ccca-4577-8de9-025fc67eff14"
           :substitutions
           {:%RESETLINK%
            (str "Please click the link below to set your password:"
                 "<br />" config/base-url "reset-password/" reset-key)})
          {:success true
           :id new-user-id})))
