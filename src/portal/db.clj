(ns portal.db
  (:require [clojure.java.jdbc :as sql]))

(defn raw-sql-query
  "Given a raw query-vec, return the results"
  [db-conn query-vec]
  (sql/with-connection db-conn
    (sql/with-query-results results
      query-vec
      (doall results))))

(defn raw-sql-update
  "Given a raw update-vec, update the results"
  [db-conn query-vec]
  (sql/with-connection db-conn
    (sql/do-prepared
     query-vec)))
