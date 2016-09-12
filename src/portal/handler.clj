(ns portal.handler
  (:require [buddy.auth.accessrules :refer [wrap-access-rules]]
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
            [ring.util.response :refer [header set-cookie response redirect]]
            ))

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
   {:pattern #".*(/.*|$)"
    :handler valid-session-wrapper?
    :redirect "/login"}])

(defroutes portal-routes
  ;;!! main page
  (GET "/" {cookies :cookies}
       (let [user-id (get-in cookies ["user-id" :value])
             user (users/get-user (conn)
                                  :where {:id user-id})]
         (str "<h1>Hello " (:name user) "</h1>")))
  ;;!! login / logout
  (GET "/login" []
       (-> (pages/portal-login)
           response
           wrap-page))
  (POST "/login" {body :body
                  headers :headers
                  remote-addr :remote-addr}
        (response
         (let [b (keywordize-keys body)]
           (login/login (conn) (:email b) (:password b)
                        (or (get headers "x-forwarded-for")
                            remote-addr)))))
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
    (if (= config/db-user "purplemasterprod")
      (try
        (handler request)
        (catch Exception e
          {:status 500 :body "Server Error - 500 Placeholder"}))
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
   (wrap-fallback-exception)))
