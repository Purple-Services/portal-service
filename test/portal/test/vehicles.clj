(ns portal.test.vehicles
  (:require [bouncer.core :as b]
            [clojure.test :refer [use-fixtures is run-tests deftest testing]]
            [common.db :as db]
            [common.util :as util]
            [portal.test.utils :refer [get-bouncer-error]]
            [portal.functional.test.vehicles :as test-vehicles]
            [portal.test.db-tools :as db-tools]
            [portal.vehicles :as vehicles]))

;; for manual testing:
;; (db-tools/setup-ebdb-test-pool!) ; initialize
;;
;; -- run tests --
;; (db-tools/reset-db!) ; note: most tests will need this run between
;; -- run more tests

(deftest vehicle-validations-test
  (let [base-vehicle (test-vehicles/vehicle-map {})]
    (testing "A valid vehicle passes"
      (is (b/valid? base-vehicle)))
    (testing "active validations work correctly"
      (is (= (list (str "You must designate this vehicle as "
                        "either active or inactive!"))
             (get-bouncer-error (b/validate (dissoc base-vehicle :active)
                                            vehicles/vehicle-validations)
                                [:active]))))
    (testing "year validations work correctly"
      (is (= (list "You must assign a year to this vehicle!")
             (get-bouncer-error (b/validate (dissoc base-vehicle :year)
                                            vehicles/vehicle-validations)
                                [:year]))))
    (testing "make validations work correctly"
      (is (= (list "You must assign a make to this vehicle!")
             (get-bouncer-error (b/validate (dissoc base-vehicle :make)
                                            vehicles/vehicle-validations)
                                [:make]))))
    (testing "model validations work correctly"
      (is (= (list "You must assign a model to this vehicle!")
             (get-bouncer-error (b/validate (dissoc base-vehicle :model)
                                            vehicles/vehicle-validations)
                                [:model]))))
    (testing "color validations work correctly"
      (is (= (list "You must assign a color to this vehicle!")
             (get-bouncer-error (b/validate (dissoc base-vehicle :color)
                                            vehicles/vehicle-validations)
                                [:color]))))
    (testing "gas type validations work correctly"
      (is (= (list "You must assign an octane rating to this vehicle!")
             (get-bouncer-error (b/validate (dissoc base-vehicle :gas_type)
                                            vehicles/vehicle-validations)
                                [:gas_type]))))
    (testing "only-top-tier validations work correctly"
      (is (= (list (str "You must select yes or no for only top tier to this "
                        "vehicle!"))
             (get-bouncer-error (b/validate (dissoc base-vehicle :only_top_tier)
                                            vehicles/vehicle-validations)
                                [:only_top_tier]))))
    (testing "licenst plate validations work correctly"
      (is (= (list (str "You must assign a license plate number to this"
                        " vehicle! If it does not have one, use \"NOPLATES\" "))
             (get-bouncer-error (b/validate (dissoc base-vehicle :license_plate)
                                            vehicles/vehicle-validations)
                                [:license_plate]))))))

