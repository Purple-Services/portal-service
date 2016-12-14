(ns portal.handler
  (:require [buddy.auth.accessrules :refer [wrap-access-rules]]
            [clj-time.core :as time]
            [clojure.walk :refer [keywordize-keys stringify-keys]]
            [common.config :as config]
            [common.db :refer [!select conn]]
            [common.users :refer [valid-session?]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [portal.accounts :as accounts]
            [portal.login :as login]
            [portal.orders :as orders]
            [portal.pages :as pages]
            [portal.users :as users]
            [portal.vehicles :as vehicles]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.stacktrace :refer [wrap-stacktrace-log]]
            [ring.util.response :refer [header set-cookie response redirect]]))

(defn wrap-page [resp]
  (header resp "content-type" "text/html; charset=utf-8"))

(defn valid-session-wrapper?
  "given a request, determine if the user-id has a valid session"
  [request]
  (let [cookies (keywordize-keys (:cookies request))
        user-id (get-in cookies [:user-id :value])
        token   (get-in cookies [:token :value])]
    (valid-session? (conn) user-id token)))

(defn user-id-matches-cookies?
  "Given a route of /user/<id>, check that the <id> matches the token
  and user-id of the cookies"
  [request]
  (let [cookies (keywordize-keys (:cookies request))
        cookie-user-id (get-in cookies [:user-id :value])
        uri (:uri request)
        request-user-id (second (re-matches #"/user/([a-zA-Z0-9]{20})/.*" uri))]
    (boolean (= cookie-user-id request-user-id))))

(defn vehicle-is-viewable-by-user?
  "Given a route of /user/<user-id>/vehicle/<vehicle-id>, check that the user
  can view the vehicle"
  [request]
  (let [uri (:uri request)
        reg-match (re-matches #"/user/([a-zA-Z0-9]{20})/vehicle/([a-zA-Z0-9]{20})"
                              uri)
        user-id (second reg-match)
        vehicle-id (nth reg-match 2)]
    (vehicles/user-can-view-vehicle? user-id vehicle-id)))

;; this should be temporary... all users should be able to add
;; vehicles
(defn user-can-add-vehicle?
  "Given a regex for the route and a request, check that the user can
  add vehicles"
  [regex request]
  (let [uri (:uri request)
        reg-match (re-matches regex uri)
        user-id (second reg-match)]
    (not (users/is-child-account? user-id))))

(defn vehicle-user-id-valid-for-user?
  "Given a regex for the route and a request,  check that the user can
  can add the vehicle with that user-id"
  [regex request]
  (let [uri (:uri request)
        body (:body request)
        json-body (keywordize-keys body)
        vehicle-user-id (:user_id json-body)
        reg-match (re-matches regex uri)
        user-id (second reg-match)]
    (boolean (= user-id vehicle-user-id))))

(defn manager-id-matches-cookies?
  "Given a route of /account/<account-id>/manager/<manager-id>, check that the
  <manager-id> matches the token and user-id of the cookies"
  [request]
  (let [cookies (keywordize-keys (:cookies request))
        cookie-user-id (get-in cookies [:user-id :value])
        uri (:uri request)
        request-user-id (second (re-matches #"/account/[a-zA-Z0-9]{20}/manager/([a-zA-Z0-9]{20})/.*"
                                            uri))]
    (boolean (= cookie-user-id request-user-id))))

(defn manager-id-manages-account?
  "Given a route of /account/<account-id>/manager/<manager-id>, check that the
  <manager-id> actually manages <account-id>"
  [request]
  (let [uri (:uri request)
        reg-match (re-matches #"/account/([a-zA-Z0-9]{20})/manager/([a-zA-Z0-9]{20})/.*"
                              uri)
        account-id (second reg-match)
        manager-id (nth reg-match 2)]
    (users/manages-account? manager-id account-id)))

(defn vehicle-user-id-valid-for-manager?
  "Given a route a regex and route, check
  that the manager can add the vehicle with user-id"
  [regex request]
  (let [uri (:uri request)
        body (:body request)
        json-body (keywordize-keys body)
        vehicle-user-id (:user_id json-body)
        reg-match (re-matches regex uri)
        account-id (second reg-match)
        manager-id (nth reg-match 2)]
    (accounts/account-can-view-user? account-id vehicle-user-id)))

(defn user-is-viewable-by-manager?
  "Given a route of
  /account/<account-id>/manager/<manager-id>/user/<user-id>, check
  that the manager can view the user"
  [request]
  (let [uri (:uri request)
        reg-match (re-matches
                   #"/account/([a-zA-Z0-9]{20})/manager/([a-zA-Z0-9]{20})/user/([a-zA-Z0-9]{20})"
                   uri)
        account-id (second reg-match)
        manager-id (nth reg-match 2)
        user-id (nth reg-match 3)]
    (accounts/account-can-view-user? account-id user-id)))

(defn vehicle-is-viewable-by-manager?
  "Given a route of
  /account/<account-id>/manager/<manager-id>/vehicle/<vehicle-id>, check
  that the manager can view vehicle"
  [request]
  (let [uri (:uri request)
        reg-match (re-matches
                   #"/account/([a-zA-Z0-9]{20})/manager/([a-zA-Z0-9]{20})/vehicle/([a-zA-Z0-9]{20})"
                   uri)
        account-id (second reg-match)
        manager-id (nth reg-match 2)
        vehicle-id (nth reg-match 3)]
    (accounts/account-can-view-vehicle? account-id vehicle-id)))

(defn user-is-editable-by-account?
  "Given a route of
  /account/<account-id>/manager/<manager-id>/edit-user, check
  that the manager can edit the user"
  [request]
  (let [uri (:uri request)
        body (:body request)
        json-body (keywordize-keys body)
        json-user-id (:id json-body)
        reg-match (re-matches
                   #"/account/([a-zA-Z0-9]{20})/manager/([a-zA-Z0-9]{20})/edit-user"
                   uri)
        account-id (second reg-match)
        manager-id (nth reg-match 2)]
    (accounts/account-can-edit-user? account-id json-user-id)))

(defn on-error
  [request value]
  (-> (response
       {:header {}
        :body {:message (str "you do not have permission to access "
                             (:uri request))}})
      (assoc :status 403)))

(def login-rules
  ;; all of these routes must always be allowed access
  [{:pattern #"/ok"
    :handler (constantly true)}
   {:pattern #".*/css/.*"
    :handler (constantly true)}
   {:pattern #".*/js/.*"
    :handler (constantly true)}
   {:pattern #".*/fonts/.*"
    :handler (constantly true)}
   {:pattern #".*/login"
    :handler (constantly true)}
   {:pattern #".*/logout"
    :handler (constantly true)}
   {:pattern #".*/images/.*"
    :handler (constantly true)}
   {:pattern #"/reset-password/.*"
    :handler (constantly true)}
   {:pattern #"/reset-password"
    :handler (constantly true)}
   {:pattern #"/forgot-password"
    :handler (constantly true)}
   {:pattern #"/user/.*/add-vehicle"
    :handler #(every? true?
                      ((juxt user-id-matches-cookies?
                             valid-session-wrapper?
                             (partial vehicle-user-id-valid-for-user?
                                      (re-pattern
                                       "/user/([a-zA-Z0-9]{20})/add-vehicle"))
                             (partial user-can-add-vehicle?
                                      (re-pattern
                                       "/user/([a-zA-Z0-9]{20})/add-vehicle")))
                       %))
    :on-error on-error}
   {:pattern #"/user/.*/edit-vehicle"
    :handler #(every? true?
                      ((juxt user-id-matches-cookies?
                             valid-session-wrapper?
                             (partial vehicle-user-id-valid-for-user?
                                      (re-pattern
                                       "/user/([a-zA-Z0-9]{20})/edit-vehicle"))
                             (partial user-can-add-vehicle?
                                      (re-pattern
                                       "/user/([a-zA-Z0-9]{20})/edit-vehicle")))
                       %))
    :on-error on-error}
   {:pattern #"/user/.*/vehicle/.*"
    :handler #(every? true?
                      ((juxt user-id-matches-cookies?
                             valid-session-wrapper?
                             vehicle-is-viewable-by-user?) %))
    :on-error on-error}
   {:pattern #"/user/.*/.*"
    :handler #(every? true?
                      ((juxt user-id-matches-cookies?
                             valid-session-wrapper?) %))
    :on-error on-error}
   {:pattern #"/account/.*/manager/.*/add-vehicle"
    :handler #(every? true?
                      ((juxt manager-id-matches-cookies?
                             valid-session-wrapper?
                             (partial
                              vehicle-user-id-valid-for-manager?
                              (re-pattern
                               "/account/([a-zA-Z0-9]{20})/manager/([a-zA-Z0-9]{20})/add-vehicle"))
                             manager-id-manages-account?) %))
    :on-error on-error}
   {:pattern #"/account/.*/manager/.*/edit-vehicle"
    :handler #(every? true?
                      ((juxt manager-id-matches-cookies?
                             valid-session-wrapper?
                             (partial
                              vehicle-user-id-valid-for-manager?
                              (re-pattern
                               "/account/([a-zA-Z0-9]{20})/manager/([a-zA-Z0-9]{20})/edit-vehicle"))
                             manager-id-manages-account?) %))
    :on-error on-error}
   {:pattern #"/account/.*/manager/.*/edit-user"
    :handler #(every? true?
                      ((juxt manager-id-matches-cookies?
                             valid-session-wrapper?
                             user-is-editable-by-account?
                             manager-id-manages-account?) %))
    :on-error on-error}
   {:pattern #"/account/.*/manager/.*/vehicle/.*"
    :handler #(every? true?
                      ((juxt manager-id-matches-cookies?
                             valid-session-wrapper?
                             vehicle-is-viewable-by-manager?
                             manager-id-manages-account?) %))
    :on-error on-error}
   {:pattern #"/account/.*/manager/.*/user/.*"
    :handler #(every? true?
                      ((juxt manager-id-matches-cookies?
                             valid-session-wrapper?
                             user-is-viewable-by-manager?
                             manager-id-manages-account?) %))
    :on-error on-error}
   {:pattern #"/account/.*/manager/.*"
    :handler #(every? true?
                      ((juxt manager-id-matches-cookies?
                             valid-session-wrapper?
                             manager-id-manages-account?) %))
    :on-error on-error}
   {:pattern #".*(/.*|$)"
    :handler valid-session-wrapper?
    :redirect "/login"}])

(defroutes portal-routes
  ;;!! main page
  (GET "/" []
       (-> (pages/portal-app)
           response
           wrap-page))
  ;;!! login / logout
  (GET "/login" []
       (-> (pages/portal-login)
           response
           wrap-page))
  (POST "/login" {body :body
                  headers :headers
                  remote-addr :remote-addr}
        (let [b (keywordize-keys body)
              login-result (login/login (:email b) (:password b)
                                        (or (get headers "x-forwarded-for")
                                            remote-addr))]
          (if (:success login-result)
            (let [user-id (get-in login-result [:user :id])]
              (-> (response {:success true})
                  (merge
                   {:cookies
                    {"token" {:value (:token login-result)
                              :http-only true
                              :max-age 7776000}
                     "user-id" {:value user-id
                                :max-age 7776000}}})))
            (response login-result))))
  (GET "/logout" []
       (-> (redirect "/login")
           (set-cookie "token" "null" {:max-age -1})
           (set-cookie "user-id" "null" {:max-age -1})))
  ;; Reset password routes
  (POST "/forgot-password" {body :body}
        (response
         (let [b (keywordize-keys body)]
           (login/forgot-password ;; 'platform_id' is email address
            (:email b)))))
  (GET "/reset-password/:reset-key" [reset-key]
       (-> (pages/reset-password reset-key)
           response
           wrap-page))
  (POST "/reset-password" {body :body}
        (response
         (let [b (keywordize-keys body)]
           (login/change-password (:reset-key b) (:password b)))))
  (context "/user/:user-id" [user-id]
           ;; this route is insecure! need to check if user really
           ;; can view this vehicle!
           (GET "/vehicle/:vehicle-id" [vehicle-id]
                (response
                 (vehicles/get-vehicle vehicle-id)))
           (POST "/add-vehicle" {body :body}
                 (response
                  (let [new-vehicle (keywordize-keys body)]
                    (vehicles/create-vehicle! new-vehicle))))
           (PUT "/edit-vehicle" {body :body}
                (response
                 (let [vehicle (keywordize-keys body)]
                   (vehicles/edit-vehicle! vehicle))))
           (GET "/vehicles" []
                (response
                 (vehicles/user-vehicles user-id)))
           (PUT "/vehicles" {body :body}
                (response
                 (let [vehicle (keywordize-keys body)]
                   (vehicles/edit-vehicle! vehicle))))
           (GET "/orders" []
                (response
                 (orders/user-orders user-id)))
           (GET "/email" []
                (response
                 {:email (users/get-user-email user-id)}))
           (GET "/accounts" []
                (response
                 (users/user-accounts user-id)))
           (GET "/is-child-user" []
                (response
                 {:is-child-user? (users/is-child-account? user-id)})))
  (context "/account/:account-id/manager/:manager-id" [account-id manager-id]
           (GET "/user/:user-id" [user-id]
                (response
                 (users/get-user user-id)))
           (PUT "/edit-user" {body :body}
                (response
                 (let [user (keywordize-keys body)]
                   (accounts/edit-user! account-id user))))
           (GET "/users" []
                (response
                 (accounts/account-users account-id)))
           (GET "/orders" []
                (response
                 (accounts/orders account-id)))
           (POST "/add-user" {body :body}
                 (response
                  (let [new-user (keywordize-keys body)]
                    (accounts/create-child-account! account-id new-user))))
           (POST "/add-vehicle" {body :body}
                 (response
                  (let [new-vehicle (keywordize-keys body)]
                    (vehicles/create-vehicle! new-vehicle))))
           (PUT "/edit-vehicle" {body :body}
                (response
                 (let [vehicle (keywordize-keys body)]
                   (vehicles/edit-vehicle! vehicle))))
           (GET "/vehicles" []
                (response
                 (accounts/account-vehicles account-id)))
           (GET "/vehicle/:vehicle-id" [vehicle-id]
                (response
                 (vehicles/get-vehicle vehicle-id))))
  ;; for aws webservices
  (GET "/ok" [] (response {:success true}))
  ;; resources
  (route/resources "/")
  (route/not-found
   {:status 404
    :body (pages/portal-not-found)
    :title "Page not found"}))

(defn wrap-fallback-exception
  "Catch exceptions and present a server error message with handler, h"
  [h]
  (fn [request]
    (if (= config/db-user "purplemasterprod")
      (try
        (h request)
        (catch Exception e
          (do
            (.println *err* (str (time/now) " " e))
            {:status 500 :body "Server Error - 500 Placeholder"})))
      (h request))))

(def handler
  (->
   portal-routes
   (wrap-cors :access-control-allow-origin [#".*"]
              :access-control-allow-methods [:get :put :post :delete])
   (wrap-access-rules {:rules login-rules})
   (wrap-cookies)
   (wrap-json-body)
   (wrap-json-response)
   (wrap-stacktrace-log)
   (wrap-fallback-exception)))
