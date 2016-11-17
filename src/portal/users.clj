(ns portal.users
  (:require [common.db :as db]
            [common.util :as util]
            [crypto.password.bcrypt :as bcrypt]
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

(defn platform-id-available?
  [platform-id]
  (not (boolean (get-user-by-email (db/conn) platform-id))))
