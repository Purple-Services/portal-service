(ns portal.functional.test.users
  (:require [clojure.test :refer [deftest is testing use-fixtures run-tests]]
            [common.db :as db]
            [portal.accounts :as accounts]
            [portal.functional.test.cookies :as cookies]
            [portal.login :as login]
            [portal.test.db-tools :refer [setup-ebdb-test-for-conn-fixture
                                          clear-and-populate-test-database-fixture
                                          reset-db!]]
            [portal.test.login-test :as login-test]
            [portal.test.utils :as test-utils]
            [portal.users :as users]))

;; for manual testing:
;; (reset-db!) ; initial setup
;;
;; -- run tests --
;; (reset-db!) ; note: most tests will need this run between
;; -- run more tests

(use-fixtures :once setup-ebdb-test-for-conn-fixture)
(use-fixtures :each clear-and-populate-test-database-fixture)

(deftest users-email-address
  (let [conn (db/conn)
        email "foo@bar.com"
        password "foobar"
        full-name "Foo Bar"
        _ (login-test/register-user! {:db-conn conn
                                      :platform-id email
                                      :password password
                                      :full-name full-name})
        login-response (test-utils/get-uri-json :post
                                                "/login"
                                                {:json-body
                                                 {:email email
                                                  :password password}})
        token (cookies/get-cookie-token login-response)
        user-id (cookies/get-cookie-user-id login-response)
        auth-cookie {"cookie" (str "token=" token ";"
                                   " user-id=" user-id)}
        ;; second user
        second-email "baz@qux.com"
        second-password "bazqux"
        second-full-name "Baz Qux"
        _ (login-test/register-user! {:db-conn conn
                                      :platform-id second-email
                                      :password second-password
                                      :full-name second-full-name})
        second-user (login/get-user-by-email conn second-email)
        second-user-id (:id second-user)]
    (testing "A user can retrieve their own email address"
      (is (= "foo@bar.com"
             (-> (test-utils/get-uri-json :get
                                          (str "/user/" user-id "/email")
                                          {:headers auth-cookie})
                 (get-in [:body :email])))))
    (testing "A user can not access other people's email address"
      (is (= 403
             (-> (test-utils/get-uri-json :get
                                          (str "/user/" second-user-id "/email")
                                          {:headers auth-cookie})
                 (get-in [:status])))))
    (testing "A regular user does not have accounts associated with them")
    ))

(deftest managed-account-login
  (let [conn (db/conn)
        email "manager@bar.com"
        password "manager"
        full-name "Manager"
        ;; register a user
        _ (login-test/register-user! {:db-conn conn
                                      :platform-id email
                                      :password password
                                      :full-name full-name})
        manager (users/get-user-by-email conn email)]
    (testing "Account manager logs in, but they are not yet an account manager"
      (let [login-response (test-utils/get-uri-json :post "/login"
                                                    {:json-body
                                                     {:email email
                                                      :password password}})]
        (is (not (cookies/get-cookie-account-manager? login-response)))))
    (testing "Account is created and account manager is an account manager"
      (let [;; register an account
            _ (accounts/create-account! "FooBar.com")
            ;; retrieve the account
            account (accounts/get-account-by-name "FooBar.com")
            ;; associate manager with account
            _ (accounts/associate-account-manager! (:id manager) (:id account))
            ;; grab accounts user manages
            ]
        ))
    (testing "Account manager manages multiple accounts")
    ))
