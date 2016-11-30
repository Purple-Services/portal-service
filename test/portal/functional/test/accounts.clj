(ns portal.functional.test.accounts
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [common.db :as db]
            [common.util :as util]
            [portal.accounts :as accounts]
            [portal.functional.test.cookies :as cookies]
            [portal.functional.test.vehicles :as test-vehicles]
            [portal.functional.test.orders :as test-orders]
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

(use-fixtures :once setup-ebdb-test-for-conn-fixture)
(use-fixtures :each clear-and-populate-test-database-fixture)

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

(deftest account-managers-security
  (with-redefs [common.sendgrid/send-template-email
                (fn [to subject message
                     & {:keys [from template-id substitutions]}]
                  (println "No reset password email was actually sent"))]
    (let [conn (db/conn)
          manager-email "manager@bar.com"
          manager-password "manager"
          manager-full-name "Manager"
          ;; register a manager
          _ (login-test/register-user! {:db-conn conn
                                        :platform-id manager-email
                                        :password manager-password
                                        :full-name manager-full-name})
          manager (login/get-user-by-email conn manager-email)
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
          _ (login-test/register-user! {:db-conn conn
                                        :platform-id child-email
                                        :password child-password
                                        :full-name child-full-name})
          child (login/get-user-by-email conn child-email)
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
          _ (login-test/register-user! {:db-conn conn
                                        :platform-id user-email
                                        :password user-password
                                        :full-name user-full-name})
          user (login/get-user-by-email conn user-email)
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
                                                      conn
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
              _ (test-vehicles/create-vehicle! conn
                                               (test-vehicles/vehicle-map {})
                                               {:id manager-user-id})
              manager-vehicle (vehicles/user-vehicles manager-user-id)
              manager-vehicle-id (:id (first manager-vehicle))
              _ (test-vehicles/create-vehicle! conn
                                               (test-vehicles/vehicle-map
                                                {:color "red"
                                                 :year "2006"})
                                               {:id manager-user-id})
              ;; child vehicle
              _ (test-vehicles/create-vehicle! conn
                                               (test-vehicles/vehicle-map
                                                {:make "Honda"
                                                 :model "Accord"
                                                 :color "Silver"})
                                               {:id child-user-id})
              child-vehicle (vehicles/user-vehicles child-user-id)
              child-vehicle-id (:id (first child-vehicle))
              ;; second child vehicle
              second-child-user-id (:id
                                    (login/get-user-by-email
                                     conn
                                     second-child-email))
              _ (test-vehicles/create-vehicle! conn
                                               (test-vehicles/vehicle-map
                                                {:make "Hyundai"
                                                 :model "Sonota"
                                                 :color "Orange"})
                                               {:id second-child-user-id})
              second-child-vehicle (vehicles/user-vehicles second-child-user-id)
              second-child-vehicle-id (:id (first second-child-vehicle))
              ;; user-vehicle
              _ (test-vehicles/create-vehicle! conn
                                               (test-vehicles/vehicle-map
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
                         (get-in [:status]))))))
          (let [child-order-1 (test-orders/order-map {:user_id child-user-id
                                                      :vehicle_id child-vehicle-id})
                _ (test-orders/create-order! conn child-order-1)
                child-order-2 (test-orders/order-map {:user_id child-user-id
                                                      :vehicle_id child-vehicle-id})
                _ (test-orders/create-order! conn child-order-2)
                manager-order-1 (test-orders/order-map
                                 {:user_id manager-user-id
                                  :vehicle_id manager-vehicle-id})
                _ (test-orders/create-order! conn manager-order-1)
                manager-order-2 (test-orders/order-map
                                 {:user_id manager-user-id
                                  :vehicle_id manager-vehicle-id})
                _ (test-orders/create-order! conn manager-order-2)
                user-order-1 (test-orders/order-map {:user_id user-id
                                                     :vehicle_id user-vehicle-id})
                _ (test-orders/create-order! conn user-order-1)
                user-order-2 (test-orders/order-map {:user_id user-id
                                                     :vehicle_id user-vehicle-id})
                _ (test-orders/create-order! conn user-order-2)]
            (testing "Account managers can see all orders"
              (is (= 4
                     (-> (test-utils/get-uri-json :get (manager-orders-uri
                                                        account-id
                                                        manager-user-id)
                                                  {:headers manager-auth-cookie})
                         (get-in [:body])
                         (count)))))
            (testing "Regular users can see their own orders"
              )
            (testing "Child users can see their own orders")
            (testing ".. but child users can't see the account orders")
            (testing "Regular users can't see orders of other accounts")))))))


(deftest selenium-acccount-user
  ;; users not shown for account-children
  ;; is shown for managers
  ;; users can be added
  ;; child account can login and change password
  ;; users can add vehicles
  ;; .. but not if they are child users
  ;; account managers can add vehicles
  ;; child accounts can't
  )
