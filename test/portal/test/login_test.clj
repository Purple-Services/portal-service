(ns portal.test.login-test
  (:require [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clojure.test :refer [use-fixtures is run-tests deftest testing]]
            [crypto.password.bcrypt :as bcrypt]
            [common.db :as db]
            [common.util :as util]
            [portal.login :as login]
            [portal.test.db-tools :refer [clear-and-populate-test-database-fixture
                                          setup-ebdb-test-for-conn-fixture]]))

(use-fixtures :once clear-and-populate-test-database-fixture)
(use-fixtures :each setup-ebdb-test-for-conn-fixture)

(defn register-user!
  "Create a user"
  [{:keys [platform-id password name]}]
  (is (:success (db/!insert (db/conn) "users"
                            {:id (util/rand-str-alpha-num 20)
                             :email platform-id
                             :type "native"
                             :password_hash (bcrypt/encrypt password)
                             :reset_key ""
                             :phone_number ""
                             :phone_number_verified 0
                             :name name}))))


(deftest user-can-login-and-logout
  (let [email "foo@bar.com"
        password "foobar"
        user {:platform-id email
              :password password
              :name "Foo Bar"}
        conn (db/conn)]
    (testing "A user can be logged in"
      (register-user! user)
      (is (:success
           (login/login email password "127.0.0.1"))))
    (testing "When a user is logged in, expired sessions are erased"
      (let [user (login/get-user-by-email email)
            token (util/new-auth-token)
            past-date (c/to-sql-time (c/to-long (t/minus (l/local-now)
                                                         (t/days 91))))
            _ (db/!insert conn "sessions" {:user_id (:id user)
                                           :token token
                                           :ip "127.0.0.1"
                                           :source "portal"
                                           :timestamp_created past-date})]
        (is (:success
             (login/login email password "127.0.0.1")))
        ;; two sessions, because two logins have occured
        (is (= 2 (count (db/!select conn "sessions" ["*"]
                                    {:user_id (:id user)
                                     :source "portal"}))))))))


(deftest user-can-reset-password
  (let [email "foo@bar.com"
        password "foobar"
        user {:platform-id email
              :password password
              :name "Foo Bar"}]
    (testing "A user can be logged in"
      (register-user! user)
      (is (:success
           (login/login email password "127.0.0.1"))))
    (testing "User forgot password"
      (with-redefs [common.sendgrid/send-template-email
                    (fn [to subject message]
                      (println "No reset password email was actually sent"))]
        (is (:success (login/forgot-password email)))))
    (let [user (login/get-user-by-email email)
          reset-key (:reset_key user)
          new-password "bazqux"]
      ;; password is changed
      (testing "User can reset password"
        (is (:success (login/change-password reset-key new-password))))
      (testing "User can login with new password"
        (is (:success
             (login/login email new-password "127.0.0.1")))))))
