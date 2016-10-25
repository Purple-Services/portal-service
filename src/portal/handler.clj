(ns portal.handler
  (:require [buddy.auth.accessrules :refer [wrap-access-rules]]
            [clj-time.core :as time]
            [clojure.walk :refer [keywordize-keys stringify-keys]]
            [common.config :as config]
            [common.db :refer [!select conn]]
            [common.users :refer [valid-session?]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [portal.login :as login]
            [portal.pages :as pages]
            [portal.users :as users]
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
  "Given a route or /user/<id>, check that the <id> matches the token
  and user-id of the cookies"
  [request]
  (let [cookies (keywordize-keys (:cookies request))
        cookie-user-id (get-in cookies [:user-id :value])
        uri (:uri request)
        request-user-id (second (re-matches #"/user/([a-zA-Z0-9]{20})/.*" uri))]
    (boolean (= cookie-user-id request-user-id))))

(defn on-error
  [request value]
  {:status 403
   :header {}
   :body (str "you do not have permission to access " (:uri request))})

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
   {:pattern #"/user/.*/.*"
    :handler #(every? true?
                      ((juxt user-id-matches-cookies?
                             valid-session-wrapper?) %))
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
              login-result (login/login (conn) (:email b) (:password b)
                                        (or (get headers "x-forwarded-for")
                                            remote-addr))]
          (if (:success login-result)
            (-> (response {:success true})
                (merge {:cookies
                        {"token" {:value (:token login-result)
                                  :http-only true
                                  :path config/base-url
                                  :max-age 7776000}
                         "user-id" {:value (get-in login-result [:user :id])
                                    :max-age 7776000}
                         }}))
            (response login-result))))
  (GET "/logout" []
       (-> (redirect "/login")
           (set-cookie "token" "null" {:max-age -1})
           (set-cookie "user-id" "null" {:max-age -1})))
  ;; Reset password routes
  (POST "/forgot-password" {body :body}
        (response
         (let [b (keywordize-keys body)]
           (login/forgot-password (conn)
                                  ;; 'platform_id' is email address
                                  (:email b)))))
  (GET "/reset-password/:reset-key" [reset-key]
       (-> (pages/reset-password (conn) reset-key)
           response
           wrap-page))
  (POST "/reset-password" {body :body}
        (response
         (let [b (keywordize-keys body)]
           (login/change-password (conn) (:reset-key b) (:password b)))))
  (context "/user/:user-id" [user-id]
           (GET "/vehicles" []
                (response
                 (users/vehicles-of-user-id user-id))))
  ;; for aws webservices
  (GET "/ok" [] (response {:success true}))
  ;; resources
  (route/resources "/")
  (route/not-found
   {:status 404
    :body (pages/portal-not-found)
    :title "Page not found"}))

(defn wrap-fallback-exception
  "Catch exceptions and present a server error message"
  [handler]
  (fn [request]
    (if (= config/db-user "purplemasterprod")
      (try
        (handler request)
        (catch Exception e
          (do
            (.println *err* (str (time/now) " " e))
            {:status 500 :body "Server Error - 500 Placeholder"})))
      (handler request))))

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
