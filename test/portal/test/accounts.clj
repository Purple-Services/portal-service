(ns portal.test.accounts
  (:require [bouncer.core :as b]
            [clojure.test :refer [use-fixtures is run-tests deftest testing]]
            [common.db :as db]
            [common.util :as util]
            [portal.accounts :as accounts]
            [portal.login :as login]
            [portal.users :as users]
            [portal.test.db-tools :refer [reset-db!
                                          setup-ebdb-test-for-conn-fixture
                                          clear-and-populate-test-database-fixture]]
            [portal.test.login-test :as login-test]
            [portal.test.utils :refer [get-bouncer-error]]))

;; for manual testing:
;; (setup-ebdb-test-pool!) ; initialize
;;
;; -- run tests --
;; (reset-db!) ; note: most tests will need this run between
;; -- run more tests

(use-fixtures :once setup-ebdb-test-for-conn-fixture)
(use-fixtures :each clear-and-populate-test-database-fixture)

(defn manually-create-child-account!
  [account-id new-user]
  (with-redefs [common.sendgrid/send-template-email
                (fn [to subject message
                     & {:keys [from template-id substitutions]}]
                  (println "No reset password email was actually sent"))]
    (let [create-results (accounts/create-child-account! account-id new-user)]
      (if (:success create-results)
        (let [new-user (first (db/!select (db/conn) "users" [:id :reset_key]
                                          {:email (:email new-user)}))]
          (str "http://localhost:3002/reset-password/" (:reset_key new-user)))
        create-results))))

(defn manually-create-manager-account!
  [{:keys [email password name account-name]}]
  (let [register-result (login-test/register-user! {:platform-id email
                                                    :password password
                                                    :name name})]
    (if register-result
      ;; manager was registered
      (let [new-manager (users/get-user-by-email email)
            new-account-result (accounts/create-account! account-name)]
        (if (:success new-account-result)
          ;; new account was created
          (let [new-account (accounts/get-account-by-name account-name)]
            ;; result of associating account with account manager
            (accounts/associate-account-manager! (:id new-manager)
                                                 (:id new-account)))
          ;; new account was not created
          new-account-result))
      ;; manager was not registered
      register-result)))

(deftest child-account-validations
  (let [email "manager@bar.com"
        password "manager"
        name "Manager"
        ;; register a user
        _ (login-test/register-user! {:platform-id email
                                      :password password
                                      :name name})
        manager (users/get-user-by-email email)
        ;; register an account
        _ (accounts/create-account! "FooBar.com")
        ;; retrieve the account
        account (accounts/get-account-by-name "FooBar.com")
        ;; associate manager with account
        _ (accounts/associate-account-manager! (:id manager) (:id account))]
    (testing "email validations work properly"
      (is (b/valid? {:email "foo@bar.com"
                     :name "Foo Bar"}
                    users/new-child-account-validations))
      (is (= '("Email can not be blank!")
             (get-bouncer-error (b/validate {:email ""
                                             :name "Foo Bar"}
                                            users/new-child-account-validations)
                                [:email])))
      (is (= '("Name can not be blank!")
             (get-bouncer-error (b/validate {:email "foo@bar.com"
                                             :name ""}
                                            users/new-child-account-validations)
                                [:name])))
      (let [child-email "foo@bar.com"
            child-password "child"
            child-name "Foo Bar"
            _ (login-test/register-user! {:platform-id child-email
                                          :password child-password
                                          :name child-name})]
        (is (= '("Email address is already in use.")
               (get-bouncer-error
                (b/validate {:email child-email
                             :name child-name}
                            users/new-child-account-validations)
                [:email])))))))

(deftest create-child-account-tests
  (let [email "manager@bar.com"
        password "manager"
        name "Manager"
        ;; register a manager
        _ (login-test/register-user! {:platform-id email
                                      :password password
                                      :name name})
        manager (users/get-user-by-email email)
        ;; register an account
        _ (accounts/create-account! "FooBar.com")
        ;; retrieve the account
        account (accounts/get-account-by-name "FooBar.com")
        ;; register another account
        _ (accounts/create-account! "BazQux.com")
        ;; retrieve the account
        another-account (accounts/get-account-by-name "BaxQux.com")
        ;; associate manager with account
        _ (accounts/associate-account-manager! (:id manager) (:id account))
        child-email "james@purpleapp.com"
        child-password "child"
        child-name "Foo Bar"]
    (with-redefs [common.sendgrid/send-template-email
                  (fn [to subject message
                       & {:keys [from template-id substitutions]}]
                    (println "No reset password email was actually sent"))]
      (testing "An account manager can add a user to an account they manage"
        (is (:success
             (accounts/create-child-account!
              (:id account)
              {:email child-email
               :name child-name})))))))
