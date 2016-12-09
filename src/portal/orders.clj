(ns portal.orders
  (:require [clojure.string :as s]
            [common.db :as db]
            [portal.vehicles :as vehicles]))

(def order-cols
  [:id :status :target_time_start :target_time_end :vehicle_id :license_plate
   :address_street :tire_pressure_check :gas_price :gallons :service_fee
   :total_price :lat :lng :user_id])

(def order-cols-select
  (s/join "," (map #(str "orders." (name %)) order-cols)))

(defn process-order
  "Given a list of vehicles, process an order for returning as json to the
  client"
  [vehicles order]
  (let [order-vehicle (first
                       (filter #(= (:vehicle_id order) (:id %))
                               vehicles))]
    (assoc order
           :vehicle_description
           (vehicles/vehicle-description order-vehicle)
           :vehicle_id (:id order-vehicle)
           :license_plate (:license_plate order-vehicle))))

(defn user-orders
  "Return all orders which match user-id"
  [user-id]
  (let [orders (db/!select (db/conn) "orders"
                           order-cols
                           {:user_id user-id})
        vehicles (vehicles/user-vehicles user-id)]
    (map (partial process-order vehicles) orders)))
