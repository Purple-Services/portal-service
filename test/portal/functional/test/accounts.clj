(ns portal.functional.test.accounts
  (:require [cheshire.core :as cheshire]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [common.db :as db]
            [common.util :as util]
            [portal.accounts :as accounts]
            [portal.functional.test.cookies :as cookies]
            [portal.functional.test.vehicles :as test-vehicles ]
            [portal.handler :refer [handler]]
            [portal.test.db-tools :refer
             [setup-ebdb-test-pool!
              setup-ebdb-test-for-conn-fixture
              clear-and-populate-test-database
              clear-and-populate-test-database-fixture
              reset-db!]]
            [portal.login :as login]
            [portal.test.login-test :as login-test]
            [portal.vehicles :as vehicles]
            [ring.mock.request :as mock]))

(use-fixtures :once setup-ebdb-test-for-conn-fixture)
(use-fixtures :each clear-and-populate-test-database-fixture)

(defn account-manager-context-uri
  [user-id]
  (str "/account-manager/" user-id))

(defn add-user-uri
  [user-id]
  (str (account-manager-context-uri user-id) "/add-user"))

(defn response-body-json
  [response]
  (cheshire/parse-string
   (:body response)
   true))

(defn get-uri-json
  "Given a request-type key, uri and optional json-body for post requests,
  return the response. If there is a json body, keywordize it."
  [request-type uri & [{:keys [json-body headers]
                        :or {json-body nil
                             headers nil}}]]
  (let [json-body (str (if (nil? json-body)
                         json-body
                         (cheshire/generate-string
                          json-body)))
        mock-request (-> (mock/request
                          request-type uri
                          json-body)
                         (assoc :headers headers)
                         (mock/content-type
                          "application/json"))
        response (portal.handler/handler
                  mock-request)]
    (assoc response :body (response-body-json response))))

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
          ;; associate manager with account
          _ (accounts/associate-account-manager! (:id manager) (:id account))
          manager-login-response (portal.handler/handler
                                  (-> (mock/request
                                       :post "/login"
                                       (cheshire/generate-string
                                        {:email manager-email
                                         :password manager-password}))
                                      (mock/content-type "application/json")))
          manager-token (cookies/get-cookie-token manager-login-response)
          manager-user-id (cookies/get-cookie-user-id manager-login-response)
          manager-auth-cookie {"cookie" (str "token=" manager-token ";"
                                             " user-id=" manager-user-id)}
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
          child-login-response (portal.handler/handler
                                (-> (mock/request
                                     :post "/login"
                                     (cheshire/generate-string
                                      {:email child-email
                                       :password child-password}))
                                    (mock/content-type "application/json")))
          child-token (cookies/get-cookie-token child-login-response)
          child-user-id (cookies/get-cookie-user-id child-login-response)
          child-auth-cookie {"cookie" (str "token=" child-token ";"
                                           " user-id=" child-user-id)}
          ;; register another account
          _ (accounts/create-account! "BazQux.com")
          ;; retrieve the account
          another-account (accounts/get-account-by-name "BaxQux.com")


          ;; second user
          ;; second-email "baz@qux.com"
          ;; second-password "bazqux"
          ;; second-full-name "Baz Qux"
          ;; _ (login-test/register-user! {:db-conn conn
          ;;                               :platform-id second-email
          ;;                               :password second-password
          ;;                               :full-name second-full-name})
          ;; second-user (login/get-user-by-email conn second-email)
          ;; second-user-id (:id second-user)
          ]
      (testing "Only account managers can add users"
        (let [second-child-email "baz@bar.com"
              second-child-full-name "Baz Bar"]
          ;; child user can't add a user
          (is (= "User does not manage that account"
                 (-> (get-uri-json :post (add-user-uri
                                          child-user-id)
                                   {:json-body
                                    {:email second-child-email
                                     :full-name second-child-full-name}
                                    :headers child-auth-cookie})
                     (get-in [:body :message]))))
          ;; account manager can
          (is (-> (get-uri-json :post (add-user-uri
                                       manager-user-id)
                                {:json-body
                                 {:email second-child-email
                                  :full-name second-child-full-name}
                                 :headers manager-auth-cookie})
                  (get-in [:body :success]))))
        (testing "Users can't see other users"
          ;; can't see their parent account's users
          ;; add another account, they can't see that either
          ))
      (testing "Only account managers can see all vehicles"
        ;; add some vehicles to manager account and child account
        ;; manager sees all vehicles
        )
      (testing "Child accounts can only see their own vehicles"
        ;; child can't get account-vehicles
        ;; child can't see another user's vehicle
        ;; child can't can't see another vehicle associated with account
        )
      (testing "Users can't see other user's vehicles"
        ;; add a vehicle by another user, not associated with account
        )
      (testing "Account managers can see all orders"
        ;; add orders for manager and child account
        )
      (testing "Users can see their own orders"
        )
      (testing "... but users can't see orders of other accounts")
      (testing "Child accounts can't add vehicles")
      (testing "A user can get their own vehicles"
        #_ (let [_ (vehicles/create-vehicle! conn (test-vehicles/vehicle-map {})
                                             {:id user-id})
                 vehicles-response (portal.handler/handler
                                    (-> (mock/request
                                         :get (str "/user/" user-id "/vehicles"))
                                        (assoc :headers auth-cookie)))
                 response-body-json (cheshire/parse-string
                                     (:body vehicles-response) true)]
             (is (= user-id
                    (-> response-body-json
                        first
                        :user_id)))))
      (testing "A user can not access other user's vehicles"
        #_ (let [_ (create-vehicle! conn (vehicle-map {}) {:id second-user-id})
                 vehicles-response (portal.handler/handler
                                    (-> (mock/request
                                         :get (str "/user/" second-user-id
                                                   "/vehicles"))
                                        (assoc :headers auth-cookie)))]
             (is (= 403
                    (:status vehicles-response))))))))


(deftest selenium-acccount-user
  ;; users not show for account-children
  ;; is shown for managers
  ;; users can be added
  ;; child account can login and change password
  ;; users can add vehicles
  ;; .. but not if they are child users
  ;; account managers can add vehicles
  ;; child accounts can't
  )
