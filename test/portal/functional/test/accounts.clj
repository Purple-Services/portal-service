(ns portal.functional.test.accounts
  (:require [clj-webdriver.taxi :refer :all]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [common.db :as db]
            [common.util :as util]
            [portal.accounts :as accounts]
            [portal.functional.test.cookies :as cookies]
            [portal.functional.test.vehicles :as test-vehicles]
            [portal.functional.test.orders :as test-orders]
            [portal.functional.test.selenium :as selenium]
            [portal.test.db-tools :refer
             [setup-ebdb-test-pool!
              setup-ebdb-test-for-conn-fixture
              clear-and-populate-test-database
              clear-and-populate-test-database-fixture
              reset-db!]]
            [portal.login :as login]
            [portal.test.login-test :as login-test]
            [portal.test.utils :as test-utils]
            [portal.vehicles :as vehicles]))
;; for manual testing:
;; (selenium/startup-test-env!) ; make sure profiles.clj was loaded with
;;                   ; :base-url "http:localhost:5744/"
;; -- run tests --
;; (reset-db!) ; note: most tests will need this run between them anyhow
;; -- run more tests
;; (selenium/shutdown-test-env!


(use-fixtures :once selenium/with-server selenium/with-browser
  selenium/with-redefs-fixture  setup-ebdb-test-for-conn-fixture)

(use-fixtures :each clear-and-populate-test-database-fixture)

;; simplified routes
(defn account-manager-context-uri
  [account-id manager-id]
  (str "/account/" account-id "/manager/" manager-id))

(defn add-user-uri
  [account-id manager-id]
  (str (account-manager-context-uri account-id manager-id) "/add-user"))

(defn account-users-uri
  [account-id manager-id]
  (str (account-manager-context-uri account-id manager-id) "/users"))

(defn manager-get-user-uri
  [account-id manager-id user-id]
  (str (account-manager-context-uri account-id manager-id) "/user/" user-id))

(defn manager-get-vehicle-uri
  [account-id manager-id vehicle-id]
  (str (account-manager-context-uri account-id manager-id) "/vehicle/"
       vehicle-id))

(defn user-vehicle-uri
  [user-id vehicle-id]
  (str "/user/" user-id "/vehicle/" vehicle-id))

(defn user-vehicles-uri
  [user-id]
  (str "/user/" user-id "/vehicles"))

(defn user-orders-uri
  [user-id]
  (str "/user/" user-id "/orders"))

(defn user-add-vehicle-uri
  [user-id]
  (str "/user/" user-id "/add-vehicle"))

(defn manager-vehicle-uri
  [account-id manager-id vehicle-id]
  (str (account-manager-context-uri account-id manager-id) "/vehicle/"
       vehicle-id))

(defn manager-add-vehicle-uri
  [account-id manager-id]
  (str (account-manager-context-uri account-id manager-id) "/add-vehicle"))

(defn manager-vehicles-uri
  [account-id manager-id]
  (str (account-manager-context-uri account-id manager-id) "/vehicles"))

(defn manager-orders-uri
  [account-id manager-id]
  (str (account-manager-context-uri account-id manager-id) "/orders"))

;; common elements
(def users-link  {:xpath "//li/a/div[text()='USERS']"})
(def add-users-button {:xpath "//div[@id='users']//button[text()=' Add']"})
(def users-form-email-address {:xpath "//div[@id='users']//form//input[@placeholder='Email Address']"})
(def users-form-full-name {:xpath "//div[@id='users']//form//input[contains(@placeholder,'Full Name')]"})
(def users-form-save
  {:xpath "//div[@id='users']//form//button[text()='Save']"})
(def users-form-dismiss
  {:xpath "//div[@id='users']//form//button[text()='Dismiss']"})
(def users-form-yes
  {:xpath "//div[@id='users']//form//button[text()='Yes']"})
(def users-form-no
  {:xpath "//div[@id='users']//form//button[text()='No']"})

(deftest account-managers-security
  (with-redefs [common.sendgrid/send-template-email
                (fn [to subject message
                     & {:keys [from template-id substitutions]}]
                  (println "No reset password email was actually sent"))]
    (let [manager-email "manager@bar.com"
          manager-password "manager"
          manager-full-name "Manager"
          ;; register a manager
          _ (login-test/register-user! {:platform-id manager-email
                                        :password manager-password
                                        :full-name manager-full-name})
          manager (login/get-user-by-email manager-email)
          account-name "FooBar.com"
          ;; register an account
          _ (accounts/create-account! account-name)
          ;; retrieve the account
          account (accounts/get-account-by-name account-name)
          account-id (:id account)
          ;; associate manager with account
          _ (accounts/associate-account-manager! (:id manager) (:id account))
          manager-login-response (test-utils/get-uri-json
                                  :post "/login"
                                  {:json-body {:email manager-email
                                               :password manager-password}})
          manager-user-id (cookies/get-cookie-user-id manager-login-response)
          manager-auth-cookie (cookies/auth-cookie manager-login-response)
          ;; child account
          child-email "james@purpleapp.com"
          child-password "child"
          child-full-name "Foo Bar"
          _ (login-test/register-user! {:platform-id child-email
                                        :password child-password
                                        :full-name child-full-name})
          child (login/get-user-by-email child-email)
          ;; associate child-account with account
          _ (accounts/associate-child-account! (:id child) (:id account))
          ;; generate auth-cokkie
          child-login-response (test-utils/get-uri-json
                                :post "/login"
                                {:json-body
                                 {:email child-email
                                  :password child-password}})
          child-user-id (cookies/get-cookie-user-id child-login-response)
          child-auth-cookie (cookies/auth-cookie child-login-response)
          ;; register another account
          _ (accounts/create-account! "BazQux.com")
          ;; retrieve the account
          another-account (accounts/get-account-by-name "BaxQux.com")
          second-child-email "baz@bar.com"
          second-child-full-name "Baz Bar"
          ;; A regular user
          user-email "baz@qux.com"
          user-password "bazqux"
          user-full-name "Baz Qux"
          _ (login-test/register-user! {:platform-id user-email
                                        :password user-password
                                        :full-name user-full-name})
          user (login/get-user-by-email user-email)
          user-id (:id user)
          user-login-response (test-utils/get-uri-json
                               :post "/login"
                               {:json-body
                                {:email user-email
                                 :password user-password}})
          user-auth-cookie (cookies/auth-cookie user-login-response)]
      (testing "Only account managers can add users"
        ;; child user can't add a user
        (is (= 403
               (-> (test-utils/get-uri-json :post (add-user-uri
                                                   account-id
                                                   child-user-id)
                                            {:json-body
                                             {:email second-child-email
                                              :full-name second-child-full-name}
                                             :headers child-auth-cookie})
                   (get-in [:status]))))
        ;; a regular user can't add a user to another account
        (is (= 403
               (-> (test-utils/get-uri-json :post (add-user-uri
                                                   account-id
                                                   user-id)
                                            {:json-body
                                             {:email second-child-email
                                              :full-name second-child-full-name}
                                             :headers user-auth-cookie})
                   (get-in [:status]))))
        ;; account manager can
        (is (-> (test-utils/get-uri-json :post (add-user-uri
                                                account-id
                                                manager-user-id)
                                         {:json-body
                                          {:email second-child-email
                                           :full-name second-child-full-name}
                                          :headers manager-auth-cookie})
                (get-in [:body :success])))
        (testing "Users can't see other users"
          ;; can't see their parent account's users
          (is (= 403
                 (-> (test-utils/get-uri-json
                      :get (account-users-uri account-id child-user-id)
                      {:headers child-auth-cookie})
                     (get-in [:status]))))
          ;; can't see another child account user
          (is (= 403
                 (-> (test-utils/get-uri-json :get (manager-get-user-uri
                                                    account-id
                                                    child-user-id
                                                    (:id
                                                     (login/get-user-by-email
                                                      second-child-email)))
                                              {:headers child-auth-cookie})
                     (get-in [:status]))))
          ;; can't see manager account user
          (is (= 403
                 (-> (test-utils/get-uri-json :get (manager-get-user-uri
                                                    account-id
                                                    child-user-id
                                                    manager-user-id)
                                              {:headers child-auth-cookie})
                     (get-in [:status]))))
          ;; ...but the manager can see the child account user
          (is (= child-user-id
                 (-> (test-utils/get-uri-json :get (manager-get-user-uri
                                                    account-id
                                                    manager-user-id
                                                    child-user-id)
                                              {:headers manager-auth-cookie})
                     (get-in [:body :id]))))
          ;; manager can't see a user not associated with their account
          (is (= 403
                 (-> (test-utils/get-uri-json :get (manager-get-user-uri
                                                    account-id
                                                    manager-user-id
                                                    user-id)
                                              {:headers manager-auth-cookie})
                     (get-in [:status])))))
        (let [;; manager vehicles
              _ (test-vehicles/create-vehicle! (test-vehicles/vehicle-map {})
                                               {:id manager-user-id})
              manager-vehicle (vehicles/user-vehicles manager-user-id)
              manager-vehicle-id (:id (first manager-vehicle))
              _ (test-vehicles/create-vehicle! (test-vehicles/vehicle-map
                                                {:color "red"
                                                 :year "2006"})
                                               {:id manager-user-id})
              ;; child vehicle
              _ (test-vehicles/create-vehicle! (test-vehicles/vehicle-map
                                                {:make "Honda"
                                                 :model "Accord"
                                                 :color "Silver"})
                                               {:id child-user-id})
              child-vehicle (vehicles/user-vehicles child-user-id)
              child-vehicle-id (:id (first child-vehicle))
              ;; second child vehicle
              second-child-user-id (:id
                                    (login/get-user-by-email
                                     second-child-email))
              _ (test-vehicles/create-vehicle! (test-vehicles/vehicle-map
                                                {:make "Hyundai"
                                                 :model "Sonota"
                                                 :color "Orange"})
                                               {:id second-child-user-id})
              second-child-vehicle (vehicles/user-vehicles second-child-user-id)
              second-child-vehicle-id (:id (first second-child-vehicle))
              ;; user-vehicle
              _ (test-vehicles/create-vehicle! (test-vehicles/vehicle-map
                                                {:make "BMW"
                                                 :model "i8"
                                                 :color "Blue"})
                                               {:id user-id})
              user-vehicle (vehicles/user-vehicles user-id)
              user-vehicle-id (:id (first user-vehicle))]
          (testing "Only account managers can see all vehicles"
            ;; manager sees all vehicles
            (is (= 4
                   (-> (test-utils/get-uri-json :get (manager-vehicles-uri
                                                      account-id
                                                      manager-user-id)
                                                {:headers manager-auth-cookie})
                       (get-in [:body])
                       (count))))
            ;; child can not
            (is (= 403
                   (-> (test-utils/get-uri-json :get (manager-vehicles-uri
                                                      account-id
                                                      child-user-id)
                                                {:headers child-auth-cookie})
                       (get-in [:status])))))
          (testing "Child accounts can only see their own vehicles"
            ;; child account only sees their own vehicle
            (is (= 1
                   (-> (test-utils/get-uri-json :get (user-vehicles-uri
                                                      child-user-id)
                                                {:headers child-auth-cookie})
                       (get-in [:body])
                       (count))))
            ;; child can't see account vehicles
            (is (= 403
                   (-> (test-utils/get-uri-json :get (manager-vehicles-uri
                                                      account-id
                                                      child-user-id)
                                                {:headers child-auth-cookie})
                       (get-in [:status]))))
            ;; user can retrieve their own vehicle
            (is (= user-vehicle-id
                   (-> (test-utils/get-uri-json
                        :get
                        (user-vehicle-uri
                         user-id
                         user-vehicle-id)
                        {:headers user-auth-cookie})
                       (get-in [:body :id]))))
            ;; manager can see another vehicle associated with account
            (is (= child-vehicle-id)
                (-> (test-utils/get-uri-json :get (manager-vehicle-uri
                                                   account-id
                                                   manager-user-id
                                                   child-vehicle-id)
                                             {:headers manager-auth-cookie})
                    (get-in [:body :id]))))
          (testing "Users can't see other user's vehicles"
            ;; a child user can't view another child user's vehicles
            (is (= 403
                   (-> (test-utils/get-uri-json
                        :get
                        (user-vehicle-uri
                         child-user-id
                         second-child-vehicle-id)
                        {:headers child-auth-cookie})
                       (get-in [:status]))))
            ;; child user can't view a regular user's vehicles
            (is (= 403
                   (-> (test-utils/get-uri-json
                        :get
                        (user-vehicle-uri
                         child-user-id
                         user-vehicle-id)
                        {:headers child-auth-cookie})
                       (get-in [:status]))))
            ;; regular user not associated with account can't view another
            ;; user's vehicle
            (is (= 403
                   (-> (test-utils/get-uri-json
                        :get
                        (user-vehicle-uri
                         user-id
                         child-vehicle-id)
                        {:headers user-auth-cookie})
                       (get-in [:status]))))
            ;; account managers can't view vehicles that aren't associated
            ;; with their account
            (is (= 403
                   (-> (test-utils/get-uri-json
                        :get
                        (manager-vehicle-uri
                         account-id
                         manager-user-id
                         user-vehicle-id)
                        {:headers manager-auth-cookie})
                       (get-in [:status]))))
            (testing "Adding vehicles test"
              ;; a regular user can add a vehicle
              (is (-> (test-utils/get-uri-json
                       :post
                       (user-add-vehicle-uri
                        user-id)
                       {:json-body
                        (dissoc
                         (test-vehicles/vehicle-map
                          {:make "BMW"
                           :model "i8"
                           :color "Red"
                           :user_id user-id})
                         :id)
                        :headers user-auth-cookie})
                      (get-in [:body :success])))
              ;; a regular user can't add a vehicle to
              ;; another user-id
              (is (= 403
                     (-> (test-utils/get-uri-json
                          :post
                          (user-add-vehicle-uri
                           user-id)
                          {:json-body
                           (dissoc
                            (test-vehicles/vehicle-map
                             {:make "BMW"
                              :model "i8"
                              :color "Red"
                              :user_id child-user-id})
                            :id)
                           :headers user-auth-cookie})
                         (get-in [:status]))))
              ;; a regular user can't add a vehicle to
              ;; an account
              (is (= 403
                     (-> (test-utils/get-uri-json
                          :post
                          (manager-add-vehicle-uri
                           account-id
                           user-id)
                          {:json-body
                           (dissoc
                            (test-vehicles/vehicle-map
                             {:make "BMW"
                              :model "i8"
                              :color "Red"
                              :user_id user-id})
                            :id)
                           :headers user-auth-cookie})
                         (get-in [:status]))))
              ;; a manager can add a vehicle to an account
              ;; with their own user-id
              (is (-> (test-utils/get-uri-json
                       :post
                       (manager-add-vehicle-uri
                        account-id
                        manager-user-id)
                       {:json-body
                        (dissoc
                         (test-vehicles/vehicle-map
                          {:make "Honda"
                           :model "Civic"
                           :color "Red"
                           :user_id manager-user-id})
                         :id)
                        :headers manager-auth-cookie})
                      (get-in [:body :success])))
              ;; manager can view their own vehicle
              (is (= manager-vehicle-id
                     (-> (test-utils/get-uri-json
                          :get
                          (manager-get-vehicle-uri
                           account-id
                           manager-user-id
                           manager-vehicle-id)
                          {:headers manager-auth-cookie})
                         (get-in [:body :id]))))
              ;; manager can view child vehicle
              (is (= child-vehicle-id
                     (-> (test-utils/get-uri-json
                          :get
                          (manager-get-vehicle-uri
                           account-id
                           manager-user-id
                           child-vehicle-id)
                          {:headers manager-auth-cookie})
                         (get-in [:body :id]))))
              ;; a manager can add a vehicle to an account
              ;; with a child-user-id
              (is (-> (test-utils/get-uri-json
                       :post
                       (manager-add-vehicle-uri
                        account-id
                        manager-user-id)
                       {:json-body
                        (dissoc
                         (test-vehicles/vehicle-map
                          {:make "Ford"
                           :model "F150"
                           :color "White"
                           :user_id child-user-id})
                         :id)
                        :headers manager-auth-cookie})
                      (get-in [:body :success])))
              ;; a manager can't add a vehicle to an account
              ;; for a regular user
              (is (= 403
                     (-> (test-utils/get-uri-json
                          :post
                          (manager-add-vehicle-uri
                           account-id
                           manager-user-id)
                          {:json-body
                           (dissoc
                            (test-vehicles/vehicle-map
                             {:make "BMW"
                              :model "i8"
                              :color "Red"
                              :user_id user-id})
                            :id)
                           :headers manager-auth-cookie})
                         (get-in [:status]))))
              ;; a child user can't add a vehicle to the account
              (is (= 403
                     (-> (test-utils/get-uri-json
                          :post
                          (manager-add-vehicle-uri
                           account-id
                           child-user-id)
                          {:json-body
                           (dissoc
                            (test-vehicles/vehicle-map
                             {:make "Honda"
                              :model "CRV"
                              :color "Beige"
                              :user_id child-vehicle-id})
                            :id)
                           :headers child-auth-cookie})
                         (get-in [:status]))))
              ;; temporary, users should still be allowed to add vehicles
              ;; a child user can't add a vehicle, period
              (is (= 403
                     (-> (test-utils/get-uri-json
                          :post
                          (user-add-vehicle-uri
                           child-user-id)
                          {:json-body
                           (dissoc
                            (test-vehicles/vehicle-map
                             {:make "BMW"
                              :model "i8"
                              :color "Red"
                              :user_id child-user-id})
                            :id)
                           :headers child-auth-cookie})
                         (get-in [:status]))))))
          (let [child-order-1 (test-orders/order-map {:user_id child-user-id
                                                      :vehicle_id child-vehicle-id})
                _ (test-orders/create-order! child-order-1)
                child-order-2 (test-orders/order-map {:user_id child-user-id
                                                      :vehicle_id child-vehicle-id})
                _ (test-orders/create-order! child-order-2)
                manager-order-1 (test-orders/order-map
                                 {:user_id manager-user-id
                                  :vehicle_id manager-vehicle-id})
                _ (test-orders/create-order! manager-order-1)
                manager-order-2 (test-orders/order-map
                                 {:user_id manager-user-id
                                  :vehicle_id manager-vehicle-id})
                _ (test-orders/create-order! manager-order-2)
                user-order-1 (test-orders/order-map {:user_id user-id
                                                     :vehicle_id user-vehicle-id})
                _ (test-orders/create-order! user-order-1)
                user-order-2 (test-orders/order-map {:user_id user-id
                                                     :vehicle_id user-vehicle-id})
                _ (test-orders/create-order! user-order-2)
                user-order-3 (test-orders/order-map {:user_id user-id
                                                     :vehicle_id user-vehicle-id})
                _ (test-orders/create-order! user-order-3)]
            (testing "Account managers can see all orders"
              (is (= 4
                     (-> (test-utils/get-uri-json :get (manager-orders-uri
                                                        account-id
                                                        manager-user-id)
                                                  {:headers manager-auth-cookie})
                         (get-in [:body])
                         (count)))))
            (testing "Regular users can see their own orders"
              (is (= 3
                     (-> (test-utils/get-uri-json :get (user-orders-uri
                                                        user-id)
                                                  {:headers user-auth-cookie})
                         (get-in [:body])
                         (count)))))
            (testing "Child users can see their own orders"
              (is (= 2
                     (-> (test-utils/get-uri-json :get (user-orders-uri
                                                        child-user-id)
                                                  {:headers child-auth-cookie})
                         (get-in [:body])
                         (count)))))
            (testing ".. but child users can't see the account orders"
              (is (= 403
                     (-> (test-utils/get-uri-json :get (manager-orders-uri
                                                        account-id
                                                        child-user-id)
                                                  {:headers child-auth-cookie})
                         (get-in [:status])))))
            (testing "Regular users can't see orders of other accounts"
              (is (= 403
                     (-> (test-utils/get-uri-json :get (manager-orders-uri
                                                        account-id
                                                        manager-user-id)
                                                  {:headers user-auth-cookie})
                         (get-in [:status])))))))))))


(deftest selenium-account-user
  (let [manager-email "manager@bar.com"
        manager-password "manager"
        manager-full-name "Manager"
        ;; register a manager
        _ (login-test/register-user! {:platform-id manager-email
                                      :password manager-password
                                      :full-name manager-full-name})
        manager (login/get-user-by-email manager-email)
        account-name "FooBar.com"
        ;; register an account
        _ (accounts/create-account! account-name)
        ;; retrieve the account
        account (accounts/get-account-by-name account-name)
        account-id (:id account)
        ;; associate manager with account
        _ (accounts/associate-account-manager! (:id manager) (:id account))
        manager-login-response (test-utils/get-uri-json
                                :post "/login"
                                {:json-body {:email manager-email
                                             :password manager-password}})
        manager-user-id (cookies/get-cookie-user-id manager-login-response)
        manager-auth-cookie (cookies/auth-cookie manager-login-response)
        ;; child account
        child-email "james@purpleapp.com"
        child-password "child"
        child-full-name "Foo Bar"
        ;; register another account
        _ (accounts/create-account! "BazQux.com")
        second-child-email "baz@bar.com"
        second-child-full-name "Baz Bar"
        ;; A regular user
        user-email "baz@qux.com"
        user-password "bazqux"
        user-full-name "Baz Qux"]
    (testing "Users can be added"
      (selenium/go-to-uri "login")
      (selenium/login-portal manager-email manager-password)
      (wait-until #(exists? selenium/logout))
      (is (exists? (find-element selenium/logout)))
      (wait-until #(exists? users-link))
      (is (exists? (find-element users-link)))
      (click users-link)
      (wait-until #(exists? add-users-button))
      ;; check to see that a blank username is invalid
      (click add-users-button)
      (wait-until #(exists? users-form-email-address))
      (clear users-form-email-address)
      (input-text users-form-email-address child-email)
      (clear users-form-full-name)
      (click users-form-save)
      (wait-until #(exists? users-form-yes))
      (click users-form-yes)
      (is "Name can not be blank!"
          (selenium/get-error-alert))
      (wait-until #(exists? users-form-dismiss))
      (click users-form-dismiss)
      ;; create a user
      (wait-until #(exists? add-users-button))
      (click add-users-button)
      (wait-until #(exists? users-form-email-address))
      (clear users-form-email-address)
      (input-text users-form-email-address child-email)
      (clear users-form-full-name)
      (input-text users-form-full-name child-full-name)
      (click users-form-save)
      (wait-until #(exists? users-form-yes))
      (click users-form-yes))
    ;; users can be added
    ;; users not shown for account-children
    ;; is shown for managers
    ;; child account can login and change password
    ;; users can add vehicles
    ;; .. but not if they are child users
    ;; account managers can add vehicles
    ;; account manages can assign users that are active to vehicles
    ))
