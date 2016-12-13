(ns portal.functional.test.accounts
  (:require [crypto.password.bcrypt :as bcrypt]
            [clj-time.format :as t]
            [clj-webdriver.taxi :refer :all]
            [clojure.string :as string]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [common.db :as db]
            [common.util :as util]
            [portal.accounts :as accounts]
            [portal.functional.test.cookies :as cookies]
            [portal.functional.test.orders :as test-orders]
            [portal.functional.test.portal :as portal]
            [portal.functional.test.selenium :as selenium]
            [portal.functional.test.vehicles :as test-vehicles]
            [portal.test.db-tools :refer
             [setup-ebdb-test-pool!
              setup-ebdb-test-for-conn-fixture
              clear-and-populate-test-database
              clear-and-populate-test-database-fixture
              reset-db!]]
            [portal.login :as login]
            [portal.test.login-test :as login-test]
            [portal.test.utils :as test-utils]
            [portal.users :as users]
            [portal.vehicles :as vehicles]))
;; for manual testing:
;; (selenium/startup-test-env!) ; make sure profiles.clj was loaded with
;;                   ; :base-url "http:localhost:5744/"
;; or if you are going to be doing ring mock tests
;; (setup-ebdb-test-pool!)
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

(defn user-edit-vehicle-uri
  [user-id]
  (str "/user/" user-id "/edit-vehicle"))

(defn manager-vehicle-uri
  [account-id manager-id vehicle-id]
  (str (account-manager-context-uri account-id manager-id) "/vehicle/"
       vehicle-id))

(defn manager-add-vehicle-uri
  [account-id manager-id]
  (str (account-manager-context-uri account-id manager-id) "/add-vehicle"))

(defn manager-edit-vehicle-uri
  [account-id manager-id]
  (str (account-manager-context-uri account-id manager-id) "/edit-vehicle"))

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
(def users-pending-tab
  {:xpath "//div[@id='users']//button[contains(text(),'Pending')]"})
(def users-active-tab
  {:xpath "//div[@id='users']//button[contains(text(),'Active')]"})
(def users-table
  {:xpath "//div[@id='users']//table"})

(deftest account-managers-security
  (with-redefs [common.sendgrid/send-template-email
                (fn [to subject message
                     & {:keys [from template-id substitutions]}]
                  (println "No reset password email was actually sent"))]
    (let [manager-email "manager@bar.com"
          manager-password "manager"
          manager-name "Manager"
          ;; register a manager
          _ (login-test/register-user! {:platform-id manager-email
                                        :password manager-password
                                        :name manager-name})
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
          child-name "Foo Bar"
          _ (login-test/register-user! {:platform-id child-email
                                        :password child-password
                                        :name child-name})
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
          second-child-name "Baz Bar"
          ;; A regular user
          user-email "baz@qux.com"
          user-password "bazqux"
          user-name "Baz Qux"
          _ (login-test/register-user! {:platform-id user-email
                                        :password user-password
                                        :name user-name})
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
                                              :name second-child-name}
                                             :headers child-auth-cookie})
                   (get-in [:status]))))
        ;; a regular user can't add a user to another account
        (is (= 403
               (-> (test-utils/get-uri-json :post (add-user-uri
                                                   account-id
                                                   user-id)
                                            {:json-body
                                             {:email second-child-email
                                              :name second-child-name}
                                             :headers user-auth-cookie})
                   (get-in [:status]))))
        ;; account manager can
        (is (-> (test-utils/get-uri-json :post (add-user-uri
                                                account-id
                                                manager-user-id)
                                         {:json-body
                                          {:email second-child-email
                                           :name second-child-name}
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
              manager-vehicles (vehicles/user-vehicles manager-user-id)
              manager-vehicle (first manager-vehicles)
              manager-vehicle-id (:id manager-vehicle)
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
              child-vehicles (vehicles/user-vehicles child-user-id)
              child-vehicle (first child-vehicles)
              child-vehicle-id (:id child-vehicle)
              ;; second child vehicle
              second-child-user-id (:id
                                    (login/get-user-by-email
                                     second-child-email))
              _ (test-vehicles/create-vehicle! (test-vehicles/vehicle-map
                                                {:make "Hyundai"
                                                 :model "Sonota"
                                                 :color "Orange"})
                                               {:id second-child-user-id})
              second-child-vehicles (vehicles/user-vehicles
                                     second-child-user-id)
              second-child-vehicle (first second-child-vehicles)
              second-child-vehicle-id (:id second-child-vehicle)
              ;; user-vehicle
              _ (test-vehicles/create-vehicle! (test-vehicles/vehicle-map
                                                {:make "BMW"
                                                 :model "i8"
                                                 :color "Blue"})
                                               {:id user-id})
              user-vehicles (vehicles/user-vehicles user-id)
              user-vehicle (first user-vehicles)
              user-vehicle-id (:id user-vehicle)]
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
            (testing "Adding and editing vehicles test"
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
              ;; a regular user can edit their own vehicle
              (is (-> (test-utils/get-uri-json
                       :put
                       (user-edit-vehicle-uri
                        user-id)
                       {:json-body
                        (assoc user-vehicle
                               :color "Black")
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
              ;; a regular user can't edit a vehicle of
              ;; another user-id
              (is (= 403
                     (-> (test-utils/get-uri-json
                          :put
                          (user-edit-vehicle-uri
                           user-id)
                          {:json-body
                           (assoc child-vehicle
                                  :color "Green")
                           :headers user-auth-cookie})
                         (get-in [:status]))))
              ;; a regular user can't change the user_id to
              ;; another user
              (is (= 403
                     (-> (test-utils/get-uri-json
                          :put
                          (user-edit-vehicle-uri
                           user-id)
                          {:json-body
                           (assoc user-vehicle
                                  :user_id manager-user-id)
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
              ;; a manager can edit a vehicle associated with an account
              (is (-> (test-utils/get-uri-json
                       :put
                       (manager-edit-vehicle-uri
                        account-id
                        manager-user-id)
                       {:json-body
                        (assoc child-vehicle
                               :color "White")
                        :headers manager-auth-cookie})
                      (get-in [:body :success])))
              ;; a manager can change the user to themselves
              (is (-> (test-utils/get-uri-json
                       :put
                       (manager-edit-vehicle-uri
                        account-id
                        manager-user-id)
                       {:json-body
                        (assoc child-vehicle
                               :user_id manager-user-id)
                        :headers manager-auth-cookie})
                      (get-in [:body :success])))
              ;; a manager can change the user to that of a child user
              (is (-> (test-utils/get-uri-json
                       :put
                       (manager-edit-vehicle-uri
                        account-id
                        manager-user-id)
                       {:json-body
                        (assoc manager-vehicle
                               :user_id child-user-id)
                        :headers manager-auth-cookie})
                      (get-in [:body :success])))
              ;; manager can't change a vehicle user-id to one they
              ;; don't manage
              (is (= 403
                     (-> (test-utils/get-uri-json
                          :put
                          (manager-edit-vehicle-uri
                           account-id
                           manager-user-id)
                          {:json-body
                           (assoc manager-vehicle
                                  :user_id user-id)
                           :headers manager-auth-cookie})
                         (get-in [:status]))))
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
                         (get-in [:status]))))
              ;; a child user can't edit a vehicle associated with the account
              (is (= 403
                     (-> (test-utils/get-uri-json
                          :put
                          (manager-edit-vehicle-uri
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
              ;; temporary, users should still be allowed to edit vehicles
              ;; a child user can't edit a vehicle, period
              (is (= 403
                     (-> (test-utils/get-uri-json
                          :put
                          (user-edit-vehicle-uri
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

(defn user-creation-date
  [email]
  (-> email
      (login/get-user-by-email)
      :id
      (users/get-user)
      :timestamp_created
      (util/unix->format (t/formatter "M/d/yyyy"))))

(defn user-map->user-table-row
  [{:keys [name email phone-number manager? created]
    :or {created (util/unix->format (util/now-unix)
                                    (t/formatter "M/d/yyyy"))}}]
  (string/join " " (filterv (comp not string/blank?)
                            [name email phone-number (if manager?
                                                            "Yes"
                                                            "No") created])))

(defn create-user
  [{:keys [email name]}]
  (wait-until #(exists? add-users-button))
  (click add-users-button)
  (wait-until #(exists? users-form-email-address))
  (clear users-form-email-address)
  (input-text users-form-email-address email)
  (clear users-form-full-name)
  (input-text users-form-full-name name)
  (click users-form-save)
  (wait-until #(exists? users-form-yes))
  (click users-form-yes))

(deftest selenium-account-user
  (let [manager-email "manager@bar.com"
        manager-password "manager"
        manager-name "Manager"
        ;; register a manager
        _ (login-test/register-user! {:platform-id manager-email
                                      :password manager-password
                                      :name manager-name})
        manager (login/get-user-by-email manager-email)
        account-name "FooBar.com"
        ;; register an account
        _ (accounts/create-account! account-name)
        ;; retrieve the account
        account (accounts/get-account-by-name account-name)
        account-id (:id account)
        ;; associate manager with account
        _ (accounts/associate-account-manager! (:id manager) (:id account))
        ;; child account
        child-email "james@purpleapp.com"
        child-password "child"
        child-name "Foo Bar"
        ;; child account 2
        second-child-email "baz@bar.com"
        second-child-name "Baz Bar"
        ;; child account 3
        third-child-email "qux@quux.com"
        third-child-name "Qux Quux"
        ;; register another account
        _ (accounts/create-account! "BazQux.com")
        ;; A regular user
        user-email "baz@qux.com"
        user-password "bazqux"
        user-name "Baz Qux"
        ;; vehicles
        manager-vehicle {:make "Nissan"
                         :model "Altima"
                         :year "2006"
                         :color "Blue"
                         :license-plate "FOOBAR"
                         :fuel-type "91 Octane"
                         :only-top-tier-gas? false
                         :user manager-name}
        first-child-vehicle {:make "Honda"
                             :model "Accord"
                             :year "2009"
                             :color "Black"
                             :license-plate "BAZQUX"
                             :fuel-type "87 Octane"
                             :only-top-tier-gas? true
                             :user child-name}
        second-child-vehicle {:make "Ford"
                              :model "F150"
                              :year "1995"
                              :color "White"
                              :license-plate "QUUXCORGE"
                              :fuel-type "91 Octane"
                              :only-top-tier-gas? true
                              :user second-child-name}]
    (testing "Users can be added"
      (selenium/go-to-uri "login")
      (selenium/login-portal manager-email manager-password)
      (wait-until #(exists? selenium/logout))
      (is (exists? (find-element selenium/logout)))
      (wait-until #(exists? users-link))
      (is (exists? (find-element users-link)))
      (click users-link)
      ;; check that the manager exists in the table
      (wait-until #(exists? users-active-tab))
      (click users-active-tab)
      (is (= (user-map->user-table-row
              {:name manager-name
               :email manager-email
               :manager? true
               :created (user-creation-date manager-email)})
             (text
              {:xpath "//div[@id='users']//table/tbody/tr[position()=1]"})))
      ;; check to see that a blank username is invalid
      (wait-until #(exists? add-users-button))
      (click add-users-button)
      (wait-until #(exists? users-form-email-address))
      (clear users-form-email-address)
      (input-text users-form-email-address child-email)
      (clear users-form-full-name)
      (click users-form-save)
      (wait-until #(exists? users-form-yes))
      (click users-form-yes)
      (wait-until #(exists? users-form-save))
      (is (= "Name can not be blank!"
             (selenium/get-error-alert)))
      (wait-until #(exists? users-form-dismiss))
      (click users-form-dismiss)
      ;; create a user
      (create-user {:email child-email
                    :name child-name})
      ;; check that the user shows up in the pending table
      (wait-until #(exists? users-pending-tab))
      (click users-pending-tab)
      (wait-until #(exists?
                    {:xpath
                     "//div[@id='users']//table/tbody/tr[position()=1]"}))
      (is (= (user-map->user-table-row
              {:name child-name
               :email child-email
               :manager? false
               :created (user-creation-date child-email)})
             (text {:xpath "//div[@id='users']//table/tbody/tr[position()=1]"})))
      ;; add a second child user
      (create-user {:email second-child-email
                    :name second-child-name})
      ;; check that the user shows up in the pending table
      (wait-until #(exists? users-pending-tab))
      (click users-pending-tab)
      (wait-until #(exists?
                    {:xpath
                     "//div[@id='users']//table/tbody/tr[position()=2]"}))
      (is (= (user-map->user-table-row
              {:name second-child-name
               :email second-child-email
               :manager? false
               :created (user-creation-date second-child-email)})
             (text {:xpath
                    "//div[@id='users']//table/tbody/tr[position()=2]"})))
      ;; add a third child user
      (create-user {:email third-child-email
                    :name third-child-name})
      ;; check that the user shows up in the pending table
      (wait-until #(exists? users-pending-tab))
      (click users-pending-tab)
      (wait-until #(exists?
                    {:xpath
                     "//div[@id='users']//table/tbody/tr[position()=3]"}))
      (is (= (user-map->user-table-row
              {:name third-child-name
               :email third-child-email
               :manager? false
               :created (user-creation-date third-child-email)})
             (text {:xpath
                    "//div[@id='users']//table/tbody/tr[position()=3]"}))))
    (testing "Manager adds vehicles"
      (click portal/vehicles-link)
      (wait-until #(exists? portal/no-vehicles-message))
      ;; account managers can add vehicles
      (portal/create-vehicle manager-vehicle)
      (wait-until #(exists?
                    {:xpath
                     "//div[@id='vehicles']//table/tbody/tr[position()=1]"}))
      (is (= (portal/vehicle-map->vehicle-str manager-vehicle)
             (portal/vehicle-table-row->vehicle-str 1)))
      ;; add another vehicle
      (portal/create-vehicle first-child-vehicle)
      (wait-until #(exists?
                    {:xpath
                     "//div[@id='vehicles']//table/tbody/tr[position()=2]"}))
      (is (= (portal/vehicle-map->vehicle-str first-child-vehicle)
             (portal/vehicle-table-row->vehicle-str 2)))
      ;; add the third vehicle
      (portal/create-vehicle second-child-vehicle)
      (wait-until #(exists?
                    {:xpath
                     "//div[@id='vehicles']//table/tbody/tr[position()=3]"}))
      (is (= (portal/vehicle-map->vehicle-str second-child-vehicle)
             (portal/vehicle-table-row->vehicle-str 3))))
    (testing "Manager can edit vehicle"
      ;; account managers edit vehicles error check
      (wait-until
       #(exists?
         {:xpath
          "//div[@id='vehicles']//table/tbody/tr[position()=3]/td[last()]/div/a[position()=1]"}))
      (click
       {:xpath
        "//div[@id='vehicles']//table/tbody/tr[position()=3]/td[last()]/div/a[position()=1]"})
      (wait-until #(exists? portal/vehicle-form-make))
      (portal/fill-vehicle-form (assoc second-child-vehicle
                                       :make " "
                                       :model " "))
      (click portal/vehicle-form-save)
      (wait-until #(exists? portal/vehicle-form-yes))
      (click portal/vehicle-form-yes)
      (wait-until #(exists? portal/vehicle-form-save))
      (is (= "You must assign a make to this vehicle!"
             (selenium/get-error-alert)))
      ;; corectly fill in the form, should be updated
      (wait-until #(exists? portal/vehicle-form-save))
      (portal/fill-vehicle-form (assoc second-child-vehicle
                                       :model "Escort"))
      (click portal/vehicle-form-save)
      (wait-until #(exists? portal/vehicle-form-yes))
      (click portal/vehicle-form-yes)
      (wait-until
       #(exists?
         portal/add-vehicle-button))
      (is (= (portal/vehicle-map->vehicle-str
              (assoc second-child-vehicle
                     :model "Escort"))
             (portal/vehicle-table-row->vehicle-str 3)))
      ;; account managers can assign users to vehicles
      (wait-until
       #(exists?
         {:xpath
          "//div[@id='vehicles']//table/tbody/tr[position()=3]/td[last()]/div/a[position()=1]"}))
      (click
       {:xpath
        "//div[@id='vehicles']//table/tbody/tr[position()=3]/td[last()]/div/a[position()=1]"})
      (wait-until #(exists? portal/vehicle-form-save))
      (portal/fill-vehicle-form (assoc second-child-vehicle
                                       :user "Foo Bar"))
      (click portal/vehicle-form-save)
      (wait-until #(exists? portal/vehicle-form-yes))
      (click portal/vehicle-form-yes)
      (wait-until
       #(exists?
         portal/add-vehicle-button))
      (is (= (portal/vehicle-map->vehicle-str
              (assoc second-child-vehicle
                     :user "Foo Bar"))
             (portal/vehicle-table-row->vehicle-str 3))))
    (testing "Child account can't add or edit vehicles"
      ;; users not shown for account-children
      (portal/logout-portal)
      (selenium/go-to-uri "login")
      ;; set the password for the child user
      (is (:success (db/!update
                     (db/conn) "users"
                     {:reset_key ""
                      :password_hash (bcrypt/encrypt child-password)}
                     {:id (:id (login/get-user-by-email child-email))})))
      (selenium/login-portal child-email child-password)
      (wait-until #(exists? selenium/logout))
      ;; child users don't have an add vehicles button
      (click portal/vehicles-link)
      (wait-until #(exists? portal/vehicles-table))
      (is (not (exists? portal/add-vehicle-button)))
      ;; child user don't have an edit vehicle button
      (is
       (not
        (exists?
         {:xpath
          "//div[@id='vehicles']//table/tbody/tr[position()=3]/td[last()]/div/a[position()=1]"})))
      ;; child users can see the vehicle they are assigned to
      (wait-until #(exists?
                    {:xpath
                     "//div[@id='vehicles']//table/tbody/tr[position()=1]"}))
      (is (= (portal/vehicle-map->vehicle-str (dissoc first-child-vehicle
                                                      :user))
             (portal/vehicle-table-row->vehicle-str 1))))
    (testing "Account managers can activate and reactivate vehicles"
      ;; login manager
      (selenium/go-to-uri "login")
      (selenium/login-portal manager-email manager-password)
      (wait-until #(exists? portal/vehicles-link))
      ;; click on the vehicle tab
      (click portal/vehicles-link)
      (wait-until #(exists? portal/add-vehicle-button))
      ;; check that row count of the active table matches the count in the
      ;; active filter
      (portal/compare-active-vehicles-table-and-filter-buttons)
      ;; check that row count of the deactivated table matches count in the
      ;; deactivated filter
      (portal/compare-deactivated-vehicles-table-and-filter-buttons)
      ;; deactivate the first vehicle
      (portal/deactivate-vehicle-and-check 1 2)
      ;; deactivate the second vehicle
      (portal/deactivate-vehicle-and-check 1 1)
      ;; deactivate the third vehicle
      (portal/deactivate-vehicle-and-check 1 0)
      ;; reactivate the first vehicle
      (portal/activate-vehicle-and-check 1 2)
      ;; reactivate the second vehicle
      (portal/activate-vehicle-and-check 1 1)
      ;; reactive the third vehicle
      (portal/activate-vehicle-and-check 1 0))))
