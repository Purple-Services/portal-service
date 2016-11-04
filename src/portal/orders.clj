(ns portal.orders
  (:require [common.db :as db]
            [portal.vehicles :as vehicles]))

(def orders-cols
  [:id :status :target_time_start :target_time_end :vehicle_id :license_plate
   :address_street :tire_pressure_check :gas_price :gallons :service_fee
   :total_price])

(defn user-orders
  "Return all orders which match user-id"
  [user-id]
  (let [orders (db/!select (db/conn) "orders"
                           orders-cols
                           {:user_id user-id})
        vehicles (vehicles/user-vehicles user-id)]
    (map (fn [order]
           (let [order-vehicle (first
                                (filter #(= (:vehicle_id order) (:id %))
                                        vehicles))]
             (assoc order
                    :vehicle_description
                    (vehicles/vehicle-description order-vehicle)
                    :vehicle_id (:id order-vehicle)
                    :license_plate (:license_plate order-vehicle))))
         orders)))
