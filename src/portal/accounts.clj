(ns portal.accounts
  (:require [bouncer.core :as b]
            [crypto.password.bcrypt :as bcrypt]
            [common.config :as config]
            [common.db :as db]
            [common.util :as util]
            [common.sendgrid :as sendgrid]
            [portal.db :refer [raw-sql-query]]
            [portal.orders :as orders]
            [portal.users :as users]
            [portal.vehicles :as vehicles]))

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

(defn account-can-view-user?
  [account-id user-id]
  (let [users (account-users account-id)]
    (boolean (not (empty?
                   (filter #(= user-id (:id %)) users))))))

(defn account-can-edit-user?
  [account-id user-id]
  (let [account-children (raw-sql-query
                          (db/conn)
                          [(account-children-sql account-id)])]
    (boolean (not (empty?
                   (filter #(= user-id (:id %)) account-children))))))

(defn get-account-by-id
  "Given an account id, return that account"
  [account-id]
  (first (db/!select (db/conn) "accounts" ["*"] {:id account-id})))

(defn get-account-by-name
  "Given an account name, return that account"
  [account-name]
  (first (db/!select (db/conn) "accounts" ["*"] {:name account-name})))

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
               :account_id account-id
               :active true}))

(defn activate-child-account!
  "Given a user-id and account-id, activate the user as a child of
  account"
  [user-id account-id]
  (db/!update (db/conn) "account_children"
              {:user_id user-id
               :account_id account-id
               :active true}
              {:user_id user-id
               :account_id account-id}))

(defn deactivate-child-account!
  "Given a user-id and account-id, deactivate the user as a child of
  account"
  [user-id account-id]
  (db/!update (db/conn) "account_children"
              {:user_id user-id
               :account_id account-id
               :active false}
              {:user_id user-id
               :account_id account-id}))

(defn create-child-account!
  "Create a new child account"
  [account-id new-user]
  ;; make sure this user actually manages the account
  (cond (not (b/valid? new-user users/new-child-account-validations))
        {:success false
         :validation (b/validate new-user users/new-child-account-validations)}
        :else
        (let [{:keys [email name phone_number]
               :or {phone_number ""}} new-user
              new-user-id (util/rand-str-alpha-num 20)
              reset-key (util/rand-str-alpha-num 22)
              account (get-account-by-id account-id)]
          ;; register a user with a blank password
          ;; will not be able to login without resetting
          ;; password
          (db/!insert (db/conn) "users"
                      (merge {:id new-user-id
                              :email email
                              :type "native"
                              :password_hash ""
                              :reset_key reset-key
                              :phone_number phone_number
                              :phone_number_verified 0
                              :name name}
                             (when (:auto_apply_subscription_id account)
                               {:subscription_id (:auto_apply_subscription_id account)
                                :subscription_period_start_time 1466196364
                                :subscription_expiration_time 2147483647
                                :subscription_auto_renew 0})))
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
                 "<br />" "https://purpledelivery.com/user/reset-password/"
                 reset-key)
            :%NAME% name})
          {:success true
           :id new-user-id})))

(defn edit-user!
  "Edit a user. Only child accounts can be edited at this point"
  [account-id user]
  (if (b/valid? user (users/child-account-validations (:id user)))
    (let [db-user (users/get-user (:id user))
          user-id (:id db-user)]
      (cond
        ;; we're not going to allow for the editing
        ;; AND activation of a user, if active has changed
        ;; that is all that will change
        (not= (:active user) (:active db-user))
        (do (if (:active user)
              (activate-child-account! user-id account-id)
              (deactivate-child-account! user-id account-id))
            {:success true
             :id user-id})
        :else
        (let [update-user-result (db/!update
                                  (db/conn)
                                  "users"
                                  (select-keys
                                   user [:name :phone_number])
                                  {:id user-id})]
          (if (:success update-user-result)
            (assoc update-user-result :id user-id)
            {:success false
             :message "There was an error when modifying this user"}))))
    ;; send error message
    {:success false
     :validation (b/validate user (users/child-account-validations (:id user)))}
    ))

(defn account-vehicles-sql
  [account-id]
  (str "SELECT " vehicles/vehicle-cols-select " FROM `vehicles` "
       "LEFT JOIN account_children ON "
       "account_children.user_id = vehicles.user_id "
       "LEFT JOIN account_managers ON "
       "account_managers.user_id = vehicles.user_id "
       "WHERE account_managers.account_id = '" account-id "' "
       "OR account_children.account_id = '" account-id "';"))

(defn account-vehicles
  "Given an account-id, return all vehicles associated with this account"
  [account-id]
  (let [account-vehicles (raw-sql-query
                          (db/conn)
                          [(account-vehicles-sql account-id)])]
    (map #(vehicles/process-vehicle %) account-vehicles)))

(defn account-can-view-vehicle?
  [account-id vehicle-id]
  (let [account-vehicles (raw-sql-query
                          (db/conn)
                          [(account-vehicles-sql account-id)])]
    (not (empty? (filter #(= (:id %)
                             vehicle-id) account-vehicles)))))

(defn account-orders-sql
  [account-id]
  (str "SELECT " orders/order-cols-select " FROM `orders` "
       "LEFT JOIN account_children ON "
       "account_children.user_id = orders.user_id "
       "LEFT JOIN account_managers ON "
       "account_managers.user_id = orders.user_id "
       "WHERE account_managers.account_id = '" account-id "' "
       "OR account_children.account_id = '" account-id "';"))

(defn orders
  "Given an account-id, return all orders associated with this account"
  [account-id]
  (let [account-orders (raw-sql-query
                        (db/conn)
                        [(account-orders-sql account-id)])
        vehicles (account-vehicles account-id)]
    (map (partial orders/process-order vehicles) account-orders)))
