(ns portal.vehicles
  (:require [bouncer.core :as b]
            [bouncer.validators :as v]
            [common.db :as db]
            [common.util :as util]
            [portal.db :refer [raw-sql-query]]))

(def vehicle-cols
  [:id :user_id :year :make :model :color :gas_type :only_top_tier
   :license_plate :active])

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

;; where going to leave this for later!
(defn user-can-view-vehicle?
  "Is the user-id authorized to view this vehicle-id?"
  [user-id vehicle-id]
  (str "SELECT vehicles.id, vehicles.user_id "
       "FROM `vehicles` "
       "JOIN account_managers ON "
       "account_manager.id = '" user-id "' "
       "WHERE vehicle.id = '" vehicle-id "';"))

(def vehicles-select
  (str "vehicles.id, vehicles.active, vehicles.user_id, vehicles.year, "
       "vehicles.make, vehicles.model, vehicles.color, vehicles.gas_type, "
       "vehicles.only_top_tier, vehicles.license_plate, "
       "vehicles.timestamp_created"))

(defn account-vehicles-sql
  [account-id]
  (str "SELECT " vehicles-select " FROM `vehicles` "
       "JOIN account_children ON "
       "account_children.user_id = vehicles.user_id "
       "JOIN account_managers ON "
       "account_managers.user_id = vehicles.user_id "
       "WHERE account_managers.account_id = '" account-id "' "
       "AND account_children.account_id = '" account-id "';"))

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

(defn account-vehicles
  "Given an account-id, return all vehicles associated with this account"
  [account-id]
  (let [vehicles (raw-sql-query
                  (db/conn)
                  [(account-vehicles-sql account-id)])]
    (map #(process-vehicle %) vehicles)))

(def vehicle-validations
  {;; confirm that the user_id is either their own, or belongs to
   ;; their account
   :user_id [[v/required :message "You must assign a user to this vehicle!"]]
   :active [[v/required :message (str "You must designate this vehicle as "
                                      "either active or inactive! ")]]
   ;; confirm that the year is between 19XX-20XX
   :year    [[v/required :message "You must assign a year to this vehicle!"]]
   :make    [[v/required :message "You must assign a make to this vehicle!"]]
   :model   [[v/required :message "You must assign a model to this vehicle!"]]
   :color   [[v/required :message "You must assign a color to this vehicle!"]]
   ;; confirm that this is 87 or 91
   :gas_type [[v/required :message
               "You must assign an octange type to this vehicle!"]]
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
