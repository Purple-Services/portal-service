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

(def new-vehicle-validations
  {;; confirm that the user_id is either their own, or belongs to
   ;; their account
   :active [[v/required :message (str "You must designate this vehicle as "
                                      "either active or inactive!")]]
   ;; confirm that the year is between 19XX-20XX
   :year    [[(comp not s/blank?) :message "You must assign a year to this vehicle!"]]
   :make    [[(comp not s/blank?) :message "You must assign a make to this vehicle!"]]
   :model   [[(comp not s/blank?) :message "You must assign a model to this vehicle!"]]
   :color   [[(comp not s/blank?) :message "You must assign a color to this vehicle!"]]
   ;; confirm that this is 87 or 91
   :gas_type [[(comp not s/blank?) :message
               "You must assign an octane rating to this vehicle!"]]
   ;; confirm that this is 'yes' or 'no'
   :only_top_tier [[v/required :message
                    (str "You must select yes or no for only top tier to this "
                         "vehicle!")]]
   ;; confirm that this is < 15 chars or 'NOPLATES'
   :license_plate [[(comp not s/blank?) :message
                    (str "You must assign a license plate number to this"
                         " vehicle! If it does not have one, use \"NOPLATES\" ")
                    ]]})

(def vehicle-validations
  (assoc new-vehicle-validations
         :id
         [[(comp not s/blank?) :message "You must specify the id of the vehicle!"]]))

(defn create-vehicle!
  "Create a new vehicle"
  [new-vehicle]
  (if (b/valid? new-vehicle new-vehicle-validations)
    (let [new-vehicle-id (util/rand-str-alpha-num 20)
          create-vehicle-result (db/!insert
                                 (db/conn)
                                 "vehicles"
                                 (assoc
                                  (select-keys
                                   new-vehicle [:user_id :active :year
                                                :make :model :color :gas_type
                                                :only_top_tier :license_plate])
                                  :id new-vehicle-id))]
      (if (:success create-vehicle-result)
        (assoc create-vehicle-result :id new-vehicle-id)
        {:success false
         :message "There was an error when creating this vehicle"}))
    ;; send error message
    {:success false
     :validation (b/validate new-vehicle new-vehicle-validations)}))

(defn edit-vehicle!
  "Edit a vehicle"
  [vehicle]
  (if (b/valid? vehicle vehicle-validations)
    (let [vehicle-id (:id vehicle)
          update-vehicle-result (db/!update
                                 (db/conn)
                                 "vehicles"
                                 (select-keys
                                  vehicle [:user_id :active :year
                                           :make :model :color :gas_type
                                           :only_top_tier :license_plate])
                                 {:id (:id vehicle)})]
      (if (:success update-vehicle-result)
        (assoc update-vehicle-result :id (:id vehicle))
        {:success false
         :message "There was an error when modifying this vehicle"}))
    ;; send error message
    {:success false
     :validation (b/validate vehicle vehicle-validations)}))
