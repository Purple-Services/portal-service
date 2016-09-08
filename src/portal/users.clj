(ns portal.users
  (:require [clojure.spec :as s]))

(s/def ::type (s/and string?
                     #{"native"
                       "facebook"
                       "google"}))

(s/def ::id string?)

(def error-messages
  {::id   [{:pred "string?"
            :message "User ID must be a string!"}
           {:pred "contains?"
            :message "User ID must not be blank!"}
           ]
   ::type [{:pred "contains?"
            :message "User type must not be blank!"}
           {:pred  "string?"
            :message "Type of user must be a string!"}
           {:pred (str #{"native" "facebook" "google"})
            :message "Type of user must be 'native','facebook', or 'google'"}
           ]})

(defn generate-error-messages
  "Given a m returned by s/explain-data and a error-map, 
  generate a list of human readable error messages"
  [m error-map]
  (let [problems (:clojure.spec/problems m)]
    (map (fn [problem]
           (if-let [path (first (:path problem))]
             (let [pred (:pred problem)
                   pred-message  (first (filter #(= (str pred) (:pred %)) (path error-map)))
                   message (:message pred-message)]
               ;;message
               ;;path
               ;;pred
               ;;pred-message
               ;;(path error-map)
               message
               )
             (let [pred (:pred problem)
                   path (last pred)
                   pred-message  (first (filter #(= (str (first pred)) (:pred %)) (path error-map)))
                   message (:message pred-message)]
               ;;message
               ;;path
               ;;pred
               ;;pred-message
               message
               ;;(path error-map)
               )))
         problems)))

(s/def ::user (s/keys :req [::type ::id]))
