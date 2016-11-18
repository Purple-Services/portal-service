(ns portal.test.accounts
  (:require [bouncer.core :as b]
            [clojure.test :refer [use-fixtures is run-tests deftest testing]]
            [common.db :as db]
            [common.util :as util]
            [portal.accounts :as accounts]
            [portal.login :as login]
            [portal.users :as users]
            [portal.test.db-tools :as db-tools]
            [portal.test.login-test :as login-test]))

;; for manual testing:
;; (setup-ebdb-test-pool!) ; initialize
;;
;; -- run tests --
;; (db-tools/reset-db!) ; note: most tests will need this run between
;; -- run more tests

(use-fixtures :each db-tools/setup-ebdb-test-for-conn-fixture)

(defn manual-create-child-account!
  [account-map]
  (with-redefs [common.sendgrid/send-template-email
                (fn [to subject message
                     & {:keys [from template-id substitutions]}]
                  (println "No reset password email was actually sent"))]
    (let [{:keys [db-conn new-user manager-id account-id]} account-map
          create-results (accounts/create-child-account! account-map)]
      (if (:success create-results)
        (let [new-user (first (db/!select db-conn "users" [:id :reset_key]
                                          {:email (:email new-user)}))]
          (str "http://localhost:3002/reset-password/" (:reset_key new-user)))
        create-results))))

(defn get-bouncer-error
  [validation-map ks]
  (get-in (second validation-map)
          (vec (concat [:bouncer.core/errors] ks))))

(deftest child-account-validations
  (let [conn (db/conn)
        email "manager@bar.com"
        password "manager"
        full-name "Manager"
        ;; register a user
        _ (login-test/register-user! {:db-conn conn
                                      :platform-id email
                                      :password password
                                      :full-name full-name})
        manager (users/get-user-by-email conn email)
        ;; register an account
        _ (accounts/create-account! "FooBar.com")
        ;; retrieve the account
        account (accounts/get-account-by-name "FooBar.com")
        ;; associate manager with account
        _ (accounts/associate-account-manager! (:id manager) (:id account))]
    (testing "email validations work properly"
      (is (b/valid? {:email "foo@bar.com"
                     :full-name "Foo Bar"}
                    accounts/child-account-validations))
      (is (= '("Email can not be blank!")
             (get-bouncer-error (b/validate {:email ""
                                             :full-name "Foo Bar"}
                                            accounts/child-account-validations)
                                [:email])))
      (is (= '("Name can not be blank!")
             (get-bouncer-error (b/validate {:email "foo@bar.com"
                                             :full-name ""}
                                            accounts/child-account-validations)
                                [:full-name])))
      (let [child-email "foo@bar.com"
            child-password "child"
            child-full-name "Foo Bar"
            _ (login-test/register-user! {:db-conn conn
                                          :platform-id child-email
                                          :password child-password
                                          :full-name child-full-name})]
        (is (= '("Email address is already in use.")
               (get-bouncer-error
                (b/validate {:email child-email
                             :full-name child-full-name}
                            accounts/child-account-validations)
                [:email])))))))

(deftest create-child-account-tests
  (let [conn (db/conn)
        email "manager@bar.com"
        password "manager"
        full-name "Manager"
        ;; register a user
        _ (login-test/register-user! {:db-conn conn
                                      :platform-id email
                                      :password password
                                      :full-name full-name})
        manager (users/get-user-by-email conn email)
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
        child-full-name "Foo Bar"
        ]
    (with-redefs [common.sendgrid/send-template-email
                  (fn [to subject message
                       & {:keys [from template-id substitutions]}]
                    (println "No reset password email was actually sent"))]
      (testing "An account manager can add a user to an account they manage"
        (is (:success
             (accounts/create-child-account!
              {:db-conn conn
               :new-user {:email child-email
                          :full-name child-full-name}
               :manager-id (:id manager)
               :account-id (:id account)}))))
      (testing "..but they can NOT add a user to an account they don't manage"
        (is (= "User does not manage that account"
               (:message
                (accounts/create-child-account!
                 {:db-conn conn
                  :new-user {:email child-email
                             :full-name child-full-name}
                  :manager-id (:id manager)
                  :account-id (:id another-account)}))))))))
