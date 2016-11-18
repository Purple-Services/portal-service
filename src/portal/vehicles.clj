(ns portal.vehicles
  (:require [common.db :as db]))

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
