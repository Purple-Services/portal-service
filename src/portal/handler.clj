(ns portal.handler
  (:require [compojure.core :refer :all]))

(defroutes portal-routes
  ;;!! main page
  (GET "/" []
       "<h1>Hello World</h1>"))

(def handler
  portal-routes)
