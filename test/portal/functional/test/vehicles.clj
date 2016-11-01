(ns portal.functional.test.vehicles
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [common.db :as db]
            [common.util :as util]
            [portal.handler :refer [handler]]
            [portal.test.db-tools :refer
             [setup-ebdb-test-pool!
              setup-ebdb-test-for-conn-fixture
              clear-and-populate-test-database
              clear-and-populate-test-database-fixture]]
            [portal.login :as login]
            [portal.test.login-test :refer [register-user!]]
            [ring.mock.request :as mock]))

;; for manual testing:
;; -- run tests --
;; (reset-db!) ; note: most tests will need this run between them anyhow
;; -- run more tests
;; (stop-server server)

(use-fixtures :once setup-ebdb-test-for-conn-fixture)
(use-fixtures :each clear-and-populate-test-database-fixture)

(defn reset-db! []
  (clear-and-populate-test-database))

(defn get-cookie-token
  "Given a response map, return the token"
  [response]
  (second (re-find #"token=([a-zA-Z0-9]*);"
                   (first (filter (partial re-matches #"token.*")
                                  (get-in response [:headers "Set-Cookie"]))))))

(defn get-cookie-user-id
  [response]
  (second (re-find #"user-id=([a-zA-Z0-9]*);"
                   (first (filter (partial re-matches #"user-id.*")
                                  (get-in response [:headers "Set-Cookie"]))))))

(defn vehicle-map
  "Given vehicle information, create a vehicle map for it"
  [{:keys [active user_id year make model color gas_type only_top_tier
           license_plate]
    :or {active 1
         user_id (util/rand-str-alpha-num 20)
         year "2016"
         make "Nissan"
         model "Altima"
         color "Blue"
         gas_type "87"
         only_top_tier 0
         license_plate "FOOBAR"}}]
  {:id (util/rand-str-alpha-num 20)
   :active 1
   :user_id user_id
   :year year
   :make make
   :model model
   :color color
   :gas_type gas_type
   :only_top_tier only_top_tier
   :license_plate license_plate})

(defn create-vehicle!
  "Add vehicle to user"
  [db-conn vehicle user]
  (db/!insert db-conn
              "vehicles"
              (merge vehicle
                     {:user_id (:id user)})))

(deftest vehicles
  (let [conn (db/conn)
        email "foo@bar.com"
        password "foobar"
        full-name "Foo Bar"
        _ (register-user! {:db-conn conn
                           :platform-id email
                           :password password
                           :full-name full-name})
        login-response (portal.handler/handler
                        (-> (mock/request
                             :post "/login"
                             (generate-string {:email email
                                               :password password}))
                            (mock/content-type "application/json")))
        token (get-cookie-token login-response)
        user-id (get-cookie-user-id login-response)
        auth-cookie {"cookie" (str "token=" token ";"
                                   " user-id=" user-id)}
        ;; second user
        second-email "baz@qux.com"
        second-password "bazqux"
        second-full-name "Baz Qux"
        _ (register-user! {:db-conn conn
                           :platform-id email
                           :password password
                           :full-name full-name})
        second-user (login/get-user-by-email conn second-email)
        second-user-id (:id second-user)]
    (testing "A user can get their own vehicles"
      (let [_ (create-vehicle! conn (vehicle-map {}) {:id user-id})
            vehicles-response (portal.handler/handler
                               (-> (mock/request
                                    :get (str "/user/" user-id "/vehicles"))
                                   (assoc :headers auth-cookie)))
            response-body-json (parse-string (:body vehicles-response) true)]
        (is (= user-id
               (-> response-body-json
                   first
                   :user_id)))))
    (testing "A user can not access other user's vehicles"
      (let [_ (create-vehicle! conn (vehicle-map {}) {:id second-user-id})
            vehicles-response (portal.handler/handler
                               (-> (mock/request
                                    :get (str "/user/" second-user-id
                                              "/vehicles"))
                                   (assoc :headers auth-cookie)))]
        (is (= 403
               (:status vehicles-response)))))))
