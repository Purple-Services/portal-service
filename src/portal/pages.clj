(ns portal.pages
  (:require [common.config :as config]
            [compojure.route :as route]
            [net.cgrand.enlive-html :refer [after append html do-> unwrap
                                            content set-attr deftemplate]]
            [portal.login :as login]))

(deftemplate portal-not-found-template "templates/404.html"
  []
  [:title] (content "Page not found"))

(defn portal-not-found
  []
  (apply str (portal-not-found-template)))

(deftemplate portal-login-template "templates/index.html"
  [x]
  [:title] (content "Purple App Portal")

  [:#pikaday-css] unwrap

  [:head] (do->
           (append (html [:meta
                          {:name "viewport"
                           :content "width=device-width, initial-scale=1"}]))
           (append (html [:link
                          {:rel "stylesheet"
                           :type "text/css"
                           :href
                           (str config/base-url "css/portal.css")}]))
           (append (html [:link
                          {:rel "stylesheet"
                           :type "text/css"
                           :href
                           (str config/base-url "css/bootstrap.min.css")}]))
           (append (html
                    [:link {:rel "stylesheet"
                            :type "text/css"
                            :href (str config/base-url
                                       "css/font-awesome.min.css")}])))

  [:#base-url] (set-attr :value (str (:base-url x)))

  [:#app] (set-attr :id "login")

  [:#init-cljs]  (fn [node] (html [:script "portal_cljs.core.login();"])))

(defn portal-login
  []
  (apply str (portal-login-template
              {:base-url
               (str config/base-url)})))

(deftemplate portal-app-template "templates/index.html"
  [x]
  [:title] (content "Purple App Portal")

  [:#pikaday-css] unwrap

  [:head] (do->
           (append (html [:meta
                          {:name "viewport"
                           :content "width=device-width, initial-scale=1"}]))
           (append (html
                    [:link {:rel "stylesheet"
                            :type "text/css"
                            :href
                            (str config/base-url "css/bootstrap.min.css")}]))
           (append (html
                    [:link {:rel "stylesheet"
                            :type "text/css"
                            :href (str config/base-url "css/sb-admin.css")}]))
           (append (html
                    [:link {:rel "stylesheet"
                            :type "text/css"
                            :href (str config/base-url "css/portal.css")}]))
           (append (html
                    [:link {:rel "stylesheet"
                            :type "text/css"
                            :href (str config/base-url
                                       "css/font-awesome.min.css")}])))

  [:#base-url] (set-attr :value (str (:base-url x)))

  [:#app] (set-attr :id "app")

  [:#init-cljs]  (fn [node]
                   (html [:script "portal_cljs.core.init_app();"])))

(defn portal-app
  []
  (apply str (portal-app-template
              {:base-url
               (str config/base-url)})))

(deftemplate reset-password-template "templates/index.html"
  [x]
  [:title] (content "Purple App - Reset Password")

  [:#pikaday-css] unwrap

  [:head] (do->
           (append (html [:meta
                          {:name "viewport"
                           :content "width=device-width, initial-scale=1"}]))
           (append (html [:link
                          {:rel "stylesheet"
                           :type "text/css"
                           :href
                           (str config/base-url "css/portal.css")}]))
           (append (html [:link
                          {:rel "stylesheet"
                           :type "text/css"
                           :href
                           (str config/base-url "css/bootstrap.min.css")}]))
           (append (html
                    [:link {:rel "stylesheet"
                            :type "text/css"
                            :href (str config/base-url
                                       "css/font-awesome.min.css")}])))

  [:#base-url] (set-attr :value (str (:base-url x)))

  [:#app] (append (html
                   [:div
                    [:div {:id "reset-password"}]
                    [:div {:id "email"
                           :data-email (:email x)}]
                    [:div {:id "reset-key"
                           :data-reset-key (:reset-key x)}]]))

  [:#init-cljs]  (fn [node]
                   (html [:script "portal_cljs.core.reset_password();"])))

(defn reset-password
  [db-conn reset-key]
  (let [user (login/get-user-by-reset-key db-conn reset-key)
        template-map {:base-url config/base-url
                      :email (:email user)
                      :reset-key reset-key}]
    (if user
      (apply str (reset-password-template template-map))
      (portal-not-found))))
