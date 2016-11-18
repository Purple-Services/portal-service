(ns portal.test.db-tools
  (:require [common.db :as db]
            [environ.core :refer [env]]
            [clojure.java.jdbc :refer [with-connection do-commands]]
            [clojure.test :refer [use-fixtures deftest is test-ns testing]]))

(def ebdb-test-config
  "Configuration map for connecting to the local test database."
  (let [db-host (env :test-db-host)
        db-port (env :test-db-port)
        db-name (env :test-db-name)
        db-password (env :test-db-password)
        db-user (env :test-db-user)
        db-sql "database/ebdb.sql"]
    {:classname "com.mysql.jdbc.Driver"
     :subprotocol "mysql"
     :subname (str "//" db-host ":" db-port "/" db-name
                   "?useLegacyDatetimeCode=false"
                   "&serverTimezone=UTC")
     :user db-user
     :password db-password
     :sql db-sql}))

(def ebdb-dev-config
  "Configuration map for connecting to the local test database."
  (let [db-host (env :test-db-host)
        db-port (env :test-db-port)
        db-name (env :test-db-name)
        db-password (env :test-db-password)
        db-user (env :test-db-user)
        db-sql "database/ebdb.sql"]
    {:classname "com.mysql.jdbc.Driver"
     :subprotocol "mysql"
     :subname (str "//" db-host ":" db-port "/" "ebdb_dev"
                   "?useLegacyDatetimeCode=false"
                   "&serverTimezone=UTC")
     :user db-user
     :password db-password
     :sql db-sql}))

(defn process-sql
  "Process a SQL file into statements that can be applied with do-commands"
  [filename]
  (let [sql-lines (->
                   (slurp filename) ; read in the sql file
                   (clojure.string/replace #"--.*\n" "") ; ignore sql comments
                   (clojure.string/split #";\n") ; sepereate chunks into
                                        ; statements
                   )]
    (->> sql-lines
         (map #(clojure.string/replace % #"\n" ""))
         (filter #(not (clojure.string/blank? %))))))

(defn create-tables-and-populate-database
  "Create tables and load test data for a datbase"
  [db-config]
  (let [ebdb-sql (process-sql (:sql db-config))]
    (with-connection db-config
      (apply do-commands ebdb-sql))))

(defn clear-test-database
  []
  ;; clear out all of the changes made to the ebdb_test database
  (with-connection ebdb-test-config
    (apply do-commands '("DROP DATABASE IF EXISTS ebdb_test"
                         "CREATE DATABASE IF NOT EXISTS ebdb_test"))))

(defn clear-and-populate-test-database
  []
  ;; start with a clean ebdb_test database
  (clear-test-database)
  ;; populate the tables
  (create-tables-and-populate-database ebdb-test-config))

(defn reset-db! []
  (clear-and-populate-test-database))

(defn clear-and-populate-test-database-fixture
  [t]
  (clear-and-populate-test-database)
  (t))

(defn setup-ebdb-test-pool!
  []
  (db/set-pooled-db! ebdb-test-config)
  (clear-and-populate-test-database))

(defn set-ebdb-dev-pool!
  []
  (db/set-pooled-db! ebdb-dev-config))

;; THIS FIXTURE REQUIRES A LOCAL MySQL DATABASE THAT HAS GIVEN PROPER
;; PERMISSIONS TO purplemaster FOR ebdb_test, OTHERWISE TESTS WILL FAIL!

(defn database-fixture
  "Remove all test data from the database"
  [test]
  (clear-and-populate-test-database)
  ;; run the test
  (test)
  (clear-test-database))

(defn setup-ebdb-test-for-conn-fixture
  [test]
  (setup-ebdb-test-pool!)
  (test)
  (clear-test-database)
  ;; close out the db connection
  (.close (:datasource (db/conn))))
