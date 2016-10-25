(ns portal.users
  (:require [common.db :as db]))

(defn vehicles-of-user-id
  "Return all vehicles which match user-id"
  [user-id]
  (db/!select (db/conn) "vehicles"
              [:id :user_id :year :make :model :color :gas_type :only_top_tier
               :license_plate]
              {:user_id user-id}))

