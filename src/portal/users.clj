(ns portal.users
  (:require [bouncer.validators :as v]
            [common.db :as db]
            [common.util :as util]
            [crypto.password.bcrypt :as bcrypt]
            [portal.db :refer [raw-sql-query]]
            [portal.login :as login]))

(defn get-user-email
  "Given a user-id retrun the users email."
  [db-conn id]
  (:email (first (db/!select db-conn "users" [:email] {:id id}))))

(defn get-user-by-email
  "Given an email address, return the user-id associated with that account"
  [db-conn email]
  (first (db/!select db-conn "users" [:id] {:email email})))

(defn is-account-manager?
  "Given an id, determine if the user is an account manager"
  [id]
  (boolean (db/!select (db/conn) "account_managers" [:id] {:user_id id})))

(defn manages-account?
  "Given an user-id and account-id, determine if they really do manage that
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

(defn platform-id-available?
  [platform-id]
  (not (boolean (get-user-by-email (db/conn) platform-id))))

(def child-account-validations
  {:email [[platform-id-available?
            :message "Email address is already in use."]
           [v/required :message "Email can not be blank!"]]
   :full-name [[v/required :message "Name can not be blank!"]]})

(defn process-user
  "Process a user to be included as a JSON response"
  [user]
  (assoc user
         :timestamp_created
         (/ (.getTime
             (:timestamp_created user))
            1000)
         :pending
         (if (= (:pending user) 1)
           true
           false)
         :is-manager (is-account-manager? (:id user))))

(def users-select
  (str "users.name, users.email, users.phone_number, users.timestamp_created, "
       "users.id, IF(users.password_hash = '',true,false) AS pending"))

(defn user-account-sql
  "Given a user-id, return the sql for retrieving that user"
  [user-id]
  (str "SELECT " users-select " FROM `users` "
       "WHERE users.id = '" user-id "';" ))

(defn get-user
  "Given a user-id, return the user."
  [user-id]
  (let [user (first (raw-sql-query
                     (db/conn)
                     [(user-account-sql user-id)]))]
    (if-not (empty? user)
      (process-user user)
      {:success false
       :message "There is no user with that id"})))
