(ns portal.vehicles
  (:require [bouncer.core :as b]
            [bouncer.validators :as v]
            [clojure.string :as s]
            [common.db :as db]
            [common.util :as util]
            [portal.db :refer [raw-sql-query]]))

(def vehicle-cols
  [:id :user_id :year :make :model :color :only_top_tier
   :license_plate :active :gas_type :timestamp_created])

(def vehicle-cols-select
  (s/join "," (map #(str "vehicles." (name %)) vehicle-cols)))

(defn vehicle-description
  "Given a vehicle return a str describing that vehicle as
  \"<year> <make> <model>\""
  [{:keys [year make model]}]
  (str year " " make " " model))

(defn user-vehicles
  "Return all vehicles which match user-id"
  [user-id]
  (into [] (db/!select (db/conn) "vehicles"
                       vehicle-cols
                       {:user_id user-id})))

(defn user-can-view-vehicle?
  "Is the user-id authorize to view this vehicle-id?"
  [user-id vehicle-id]
  (let [vehicles (user-vehicles user-id)]
    (boolean (not (empty?
                   (filter #(= vehicle-id (:id %)) vehicles))))))

(defn manager-can-view-vehicle-sql
  [user-id vehicle-id]
  (str "SELECT vehicles.id, vehicles.user_id "
       "FROM `vehicles` "
       "JOIN account_managers ON "
       "account_managers.id = '" user-id "' "
       "WHERE vehicles.id = '" vehicle-id "';"))

(defn manager-can-view-vehicle?
  "Is the user-id authorized to view this vehicle-id as a manager?"
  [user-id vehicle-id]
  (let [vehicles (raw-sql-query
                  (db/conn)
                  [(manager-can-view-vehicle-sql user-id vehicle-id)])]
    (not (empty? vehicles))))


(defn process-vehicle
  "Process a vehicle to included as a JSON response"
  [vehicle]
  (assoc vehicle :timestamp_created
         (/ (.getTime
             (:timestamp_created vehicle))
            1000)))

;; this is insecure! must check later if user can view the vehicle!
(defn get-vehicle
  "Given a vehicle id, retrieve it"
  [vehicle-id]
  (first (db/!select (db/conn) "vehicles"
                     vehicle-cols
                     {:id vehicle-id})))

(def vehicle-validations
  {;; confirm that the user_id is either their own, or belongs to
   ;; their account
   :user_id [[v/required :message "You must assign a user to this vehicle!"]]
   :active [[v/required :message (str "You must designate this vehicle as "
                                      "either active or inactive!")]]
   ;; confirm that the year is between 19XX-20XX
   :year    [[v/required :message "You must assign a year to this vehicle!"]]
   :make    [[v/required :message "You must assign a make to this vehicle!"]]
   :model   [[v/required :message "You must assign a model to this vehicle!"]]
   :color   [[v/required :message "You must assign a color to this vehicle!"]]
   ;; confirm that this is 87 or 91
   :gas_type [[v/required :message
               "You must assign an octane rating to this vehicle!"]]
   ;; confirm that this is 'yes' or 'no'
   :only_top_tier [[v/required :message
                    (str "You must select yes or no for only top tier to this "
                         "vehicle!")]]
   ;; confirm that this is < 15 chars or 'NOPLATES'
   :license_plate [[v/required :message
                    (str "You must assign a license plate number to this"
                         " vehicle! If it does not have one, use \"NOPLATES\" ")
                    ]]})

(defn create-vehicle!
  "Create a new vehicle"
  [new-vehicle]
  (if (b/valid? new-vehicle vehicle-validations)
    (let [new-vehicle-id (util/rand-str-alpha-num 20)
          create-vehicle-result (db/!insert (db/conn) "vehicles"
                                            (assoc new-vehicle
                                                   :id new-vehicle-id))]
      (if (:success create-vehicle-result)
        (assoc create-vehicle-result :id new-vehicle-id)
        {:success false
         :message "There was an error when creating this vehicle"}))
    ;; send error message
    {:success false
     :validation (b/validate new-vehicle vehicle-validations)}))
