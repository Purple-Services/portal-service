(ns portal.users
  (:require [common.db :as db]
            [portal.login :as login]))

(defn get-user-email
  "Given a user-id retrun the users email."
  [db-conn id]
  (:email (first (db/!select db-conn "users" [:email] {}))))
