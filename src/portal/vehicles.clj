(ns portal.vehicles
  (:require [common.db :as db]))

(defn vehicles
  "Return all vehicles which match user-id"
  [user-id]
  (db/!select (db/conn) "vehicles"
              [:id :user_id :year :make :model :color :gas_type :only_top_tier
               :license_plate :active]
              {:user_id user-id}))
