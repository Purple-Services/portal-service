(ns portal.test.login
  (:require [clojure.test :refer [use-fixtures is]]
            [common.db :as db]
            [common.util :as util]
            [portal.login :as login]
            [crypto.password.bcrypt :as bcrypt]
            [portal.test.db-tools :refer [setup-ebdb-test-for-conn-fixture
                                          setup-ebdb-test-pool!]]))

(use-fixtures :each setup-ebdb-test-for-conn-fixture)


(defn register-user!
  "Create a user"
  [{:keys [db-conn platform-id password full-name]}]
  (is (:success (db/!insert db-conn "users"
                            {:id (util/rand-str-alpha-num 20)
                             :email platform-id
                             :type "native"
                             :password_hash (bcrypt/encrypt password)
                             :reset_key ""
                             :phone_number ""
                             :phone_number_verified 0
                             :name full-name}))))


