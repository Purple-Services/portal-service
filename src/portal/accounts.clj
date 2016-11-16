(ns portal.accounts
  (:require [bouncer.core :as b]
            [bouncer.validators :as v]
            [crypto.password.bcrypt :as bcrypt]
            [common.config :as config]
            [common.db :as db]
            [common.util :as util]
            [common.sendgrid :as sendgrid]
            [portal.users :as users]))

(defn account-name-exists?
  "Is there already an account with this name?"
  [account-name]
  (boolean (db/!select (db/conn) "accounts" [:id] {:name account-name})))

(defn manages-account?
  "Given an user-id and account-id, determine if they really do manager that
  account"
  [user-id account-id]
  (->>
   (db/!select (db/conn)
               "account_managers" [:user_id :account_id] {:user_id user-id})
   (filter #(= (:account_id %)
               account-id))
   first
   ((fn [account-manager]
      (boolean (and (= (:account_id account-manager)
                       account-id)
                    (= (:user_id account-manager)
                       user-id)))))))

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

(def child-account-validations
  {:email [[users/platform-id-available?
            :message "Email address is already in use."]
           [v/required :message "Email can not be blank!"]]
   :full-name [[v/required :message "Name can not be blank!"]]})

(defn create-child-account!
  "Create a new child account"
  [{:keys [db-conn new-user manager-id account-id]}]
  ;; make sure this user actually manages the account
  (cond (not (manages-account? manager-id account-id))
        {:success false
         :message "User does not manage that account"}
        (not (b/valid? new-user child-account-validations))
        {:success false
         :validation (b/validate new-user child-account-validations)}
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
          {:success true})))
