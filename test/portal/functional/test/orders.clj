(ns portal.functional.test.orders
  (:require [cheshire.core :as cheshire]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clj-time.local :as l]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [common.db :as db]
            [common.util :as util]
            [portal.functional.test.cookies :as cookies]
            [portal.functional.test.vehicles
             :refer [vehicle-map create-vehicle!]]
            [portal.handler :refer [handler]]
            [portal.login :as login]
            [portal.test.db-tools :as db-tools]
            [portal.test.login-test :as login-test]
            [portal.vehicles :as vehicles]
            [ring.mock.request :as mock]))

;; for manual testing:
;; -- run tests --
;; (db-tools/reset-db!) ; note: most tests will need this run between
;; -- run more tests

(use-fixtures :once db-tools/setup-ebdb-test-for-conn-fixture)
(use-fixtures :each db-tools/clear-and-populate-test-database-fixture)

(defn order-map
  "Given an order's information, create an order map for it"
  [{:keys [id status user_id courier_id vehicle_id license_plate
           target_time_start target_time_end gallons gas_type is_top_tier
           tire_pressure_check special_instructions lat lng address_street
           address_city address_state address_zip referral_gallons_used
           coupon_code subscription_id subscription_discount gas_price
           service_fee total_price paid stripe_charge_id stripe_refund_id
           stripe_balance_transaction_id time_paid payment_info number_rating
           text_rating event_log admin_event_log notes]
    :or {id (util/rand-str-alpha-num 20)
         status "unassigned"
         user_id (util/rand-str-alpha-num 20)
         courier_id (util/rand-str-alpha-num 20)
         vehicle_id (util/rand-str-alpha-num 20)
         license_plate "FOOBAR"
         target_time_start (quot (c/to-long (l/local-now))
                                 1000)
         target_time_end (quot (c/to-long (t/plus (l/local-now)
                                                  (t/hours 3)))
                               1000)
         gallons 10
         gas_type "87"
         is_top_tier 1
         tire_pressure_check 0
         special_instructions ""
         lat (str "34.0" (rand-int 9))
         lng (str "-118.4" (rand-int 9))
         address_street "123 Foo Br"
         address_city "Los Angeles"
         address_zip "90210"
         referral_gallons_used 0
         coupon_code ""
         subscription_id 0
         subscription_discount 0
         gas_price 250
         service_fee 399
         paid 0
         stripe_charge_id "no charge id"
         stripe_refund_id "no refund id"
         stripe_balance_transaction_id "no transaction id"}}]
  {:id id
   :status status
   :user_id user_id
   :courier_id courier_id
   :vehicle_id vehicle_id
   :license_plate license_plate
   :target_time_start target_time_start
   :target_time_end target_time_end
   :gallons gallons
   :gas_type gas_type
   :is_top_tier is_top_tier
   :tire_pressure_check tire_pressure_check
   :special_instructions special_instructions
   :lat lat
   :lng lng
   :address_street address_street
   :address_city address_city
   :address_zip address_zip
   :referral_gallons_used referral_gallons_used
   :coupon_code coupon_code
   :subscription_id subscription_id
   :subscription_discount subscription_discount
   :gas_price gas_price
   :service_fee service_fee
   :total_price (+ (* gas_price gallons) service_fee)
   :paid paid
   :stripe_charge_id stripe_charge_id
   :stripe_refund_id stripe_refund_id
   :stripe_balance_transaction_id stripe_balance_transaction_id})

(defn create-order!
  "Given an order, create it in the database"
  [order]
  (is (:success (db/!insert (db/conn) "orders"
                            order))))

(deftest orders
  (let [email "foo@bar.com"
        password "foobar"
        full-name "Foo Bar"
        _ (login-test/register-user! {:platform-id email
                                      :password password
                                      :full-name full-name})
        login-response (portal.handler/handler
                        (-> (mock/request
                             :post "/login"
                             (cheshire/generate-string {:email email
                                                        :password password}))
                            (mock/content-type "application/json")))
        token (cookies/get-cookie-token login-response)
        user-id (cookies/get-cookie-user-id login-response)
        auth-cookie {"cookie" (str "token=" token ";"
                                   " user-id=" user-id)}
        ;; second user
        second-email "baz@qux.com"
        second-password "bazqux"
        second-full-name "Baz Qux"
        _ (login-test/register-user! {:platform-id second-email
                                      :password second-password
                                      :full-name second-full-name})
        second-user (login/get-user-by-email second-email)
        second-user-id (:id second-user)]
    (testing "A user can get their own orders"
      (let [;; create a vehicle for user
            user-vehicle-map (vehicle-map {})
            _ (create-vehicle! user-vehicle-map {:id user-id})
            user-vehicle (first (vehicles/user-vehicles user-id))
            ;; create an order
            user-order (order-map {:user_id user-id
                                   :vehicle_id (:id user-vehicle)})
            _ (create-order! user-order)
            ;; response
            orders-response  (handler
                              (-> (mock/request
                                   :get (str "/user/" user-id "/orders"))
                                  (assoc :headers auth-cookie)))
            response-body-json (first (cheshire/parse-string
                                       (:body orders-response)
                                       true))]
        (is (= (:id user-vehicle)
               (:vehicle_id response-body-json)))
        (is (= (vehicles/vehicle-description user-vehicle-map)
               (:vehicle_description response-body-json))))
      (testing "A user can not access other user's orders"
        (let [;; create a vehicle for second-user
              user-vehicle-map (vehicle-map {})
              _ (create-vehicle! user-vehicle-map {:id second-user-id})
              user-vehicle (first (vehicles/user-vehicles second-user-id))
              ;; create an order for second user
              user-order (order-map {:user_id second-user-id
                                     :vehicle_id (:id user-vehicle)})
              _ (create-order! user-order)
              ;; response
              orders-response  (handler
                                (-> (mock/request
                                     :get
                                     (str "/user/" second-user-id "/orders"))
                                    (assoc :headers auth-cookie)))]
          (is (= 403
                 (:status orders-response))))))))

