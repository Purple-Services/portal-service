(ns portal.handler
  (:require [buddy.auth.accessrules :refer [wrap-access-rules]]
            [clj-time.core :as time]
            [clojure.walk :refer [keywordize-keys stringify-keys]]
            [common.config :as config]
            [common.db :refer [!select conn]]
            [common.users :as users]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [portal.login :as login]
            [portal.pages :as pages]
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
    (users/valid-session? (conn) user-id token)))

(def login-rules
  [{:pattern #"/ok" ; this route must always be allowed access
    :handler (constantly true)}
   {:pattern #".*/css/.*" ; this route must always be allowed access
    :handler (constantly true)}
   {:pattern #".*/js/.*" ; this route must always be allowed access
    :handler (constantly true)}
   {:pattern #".*/fonts/.*" ; this route must always be allowed access
    :handler (constantly true)}
   {:pattern #".*/login" ; this route must always be allowed access
    :handler (constantly true)}
   {:pattern #".*/logout" ; this route must always be allowed access
    :handler (constantly true)}
   {:pattern #".*/images/.*" ; this route must always be allowed access
    :handler (constantly true)}
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
                                  :max-age 7776000}
                         "user-id" {:value (get-in login-result [:user :id])
                                    :max-age 7776000}}}))
            (response login-result))))
  (GET "/exception" []
       (throw (Exception. "I should ALWAYS throw an exception")))
  (GET "/logout" []
       (-> (redirect "/login")
           (set-cookie "token" "null" {:max-age -1})
           (set-cookie "user-id" "null" {:max-age -1})))
  ;; for aws webservices
  (GET "/ok" [] (response {:success true}))
  ;; resources
  (route/resources "/")
  (route/not-found
   {:status 404
    :title "page not found"
    :body "Page not found - 404 Placeholder"}))

(defn wrap-fallback-exception
  "Catch exceptions and present a server error message"
  [handler]
  (fn [request]
    (if (= config/db-user "purplemaster")
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
