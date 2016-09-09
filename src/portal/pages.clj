(ns portal.pages
  (:require [net.cgrand.enlive-html :refer [after append html do-> unwrap content
                                            set-attr deftemplate]]
            [common.config :as config]))


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

  [:#map] (set-attr :id "login")

  [:#map-init]  (fn [node] (html [:script "portal_cljs.core.login();"])))

(defn portal-login
  []
  (apply str (portal-login-template
              {:base-url
               (str config/base-url)})))
