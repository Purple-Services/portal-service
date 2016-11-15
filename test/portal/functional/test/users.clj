(ns portal.functional.test.users
  (:require [cheshire.core :as cheshire]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [common.db :as db]
            [portal.functional.test.cookies :as cookies]
            [portal.handler :as handler]
            [portal.login :as login]
            [portal.test.db-tools :as db-tools]
            [portal.test.login-test :as login-test]
            [ring.mock.request :as mock]))

;; for manual testing:
;; (db/tools/reset-db!) ; initial setup
;;
;; -- run tests --
;; (db-tools/reset-db!) ; note: most tests will need this run between
;; -- run more tests

(def handler handler/handler)
(use-fixtures :once db-tools/setup-ebdb-test-for-conn-fixture)
(use-fixtures :each db-tools/clear-and-populate-test-database-fixture)

(deftest users
  (let [conn (db/conn)
        email "foo@bar.com"
        password "foobar"
        full-name "Foo Bar"
        _ (login-test/register-user! {:db-conn conn
                                      :platform-id email
                                      :password password
                                      :full-name full-name})
        login-response (handler
                        (-> (mock/request
                             :post "/login"
                             (cheshire/generate-string {:email email
                                                        :password password}))
                            (mock/content-type "application/json")))
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
    (testing "A user can their own email address"
      (is (= "foo@bar.com"
             (:email (cheshire/parse-string
                      (:body (handler
                              (-> (mock/request
                                   :get (str "/user/" user-id "/email"))
                                  (assoc :headers auth-cookie))))
                      true)))))
    (testing "A user can not access other people's email address"
      (is (= 403
             (:status (handler
                       (-> (mock/request
                            :get (str "/user/" second-user-id "/email"))
                           (assoc :headers auth-cookie)))))))))
