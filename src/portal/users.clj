(ns portal.users
  (:require [common.db :as db]
            [common.util :as util]
            [crypto.password.bcrypt :as bcrypt]
            [portal.login :as login]))

(defn get-user-email
  "Given a user-id retrun the users email."
  [db-conn id]
  (:email (first (db/!select db-conn "users" [:email] {}))))

(defn get-user-by-email
  "Given an email address, return the user-id associated with that account"
  [db-conn email]
  (first (db/!select db-conn "users" [:id] {:email email})))

(defn is-account-manager?
  "Given an id, determine if the user is an account manager"
  [id]
  (boolean (db/!select (db/conn) "account_managers" [:id] {:user_id id})))

(defn register-user!
  "Create a user account of type native.
  optional keys
  :reset_key ; default is \"\"
  :phone_number ; default is \"\"
  :phone_number_verified ; default is 0 (false)"
  [{:keys [db-conn platform-id password full-name
           reset_key phone_number phone_number_verified]
    :or {reset_key "" phone_number "" phone_number_verified 0}}]
  (db/!insert db-conn "users"
              {:id (util/rand-str-alpha-num 20)
               :email platform-id
               :type "native"
               :password_hash (bcrypt/encrypt password)
               :reset_key reset_key
               :phone_number phone_number
               :phone_number_verified phone_number_verified
               :name full-name}))
