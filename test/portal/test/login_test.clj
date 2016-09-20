(ns portal.test.login-test
  (:require [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clojure.test :refer [use-fixtures is run-tests deftest testing]]
            [common.db :as db]
            [common.util :as util]
            [portal.login :as login]
            [crypto.password.bcrypt :as bcrypt]
            [portal.test.db-tools :refer [setup-ebdb-test-for-conn-fixture
                                          setup-ebdb-test-pool!]]))

(use-fixtures :each setup-ebdb-test-for-conn-fixture)


(defn register-user!
  "Create a user"
  [{:keys [db-conn platform-id password full-name]}]
  (is (:success (db/!insert db-conn "users"
                            {:id (util/rand-str-alpha-num 20)
                             :email platform-id
                             :type "native"
                             :password_hash (bcrypt/encrypt password)
                             :reset_key ""
                             :phone_number ""
                             :phone_number_verified 0
                             :name full-name}))))


(deftest user-can-login
  (let [email "foo@bar.com"
        password "foobar"
        user {:platform-id email
              :password password
              :full-name "Foo Bar"}
        conn (db/conn)]
    (testing "A user can be logged in"
      (register-user! (merge user                       
                             {:db-conn conn}))
      (is (:success
           (login/login conn email password "127.0.0.1"))))
    (testing "When a user is logged in, expired sessions are erased"
      (let [user (login/get-user-by-email conn email)
            token (util/new-auth-token)
            past-date (c/to-sql-time (c/to-long (t/minus (l/local-now)
                                                         (t/days 91))))
            _ (db/!insert conn "sessions" {:user_id (:id user)
                                           :token token
                                           :ip "127.0.0.1"
                                           :source "portal"
                                           :timestamp_created past-date})]
        (is (:success
             (login/login conn email password "127.0.0.1")))
        ;; two sessions, because two logins have occured
        (is (= 2 (count (db/!select conn "sessions" ["*"]
                                    {:user_id (:id user)
                                     :source "portal"}))))))))
