(ns portal.login
  (:require [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clojure.java.jdbc :as sql]
            [clojure.string :as string]
            [common.db :as db]
            [common.users :refer [valid-email? valid-password?
                                  auth-native?]]
            [common.util :as util]
            [crypto.password.bcrypt :as bcrypt]))

(def safe-authd-user-keys
  "The keys of a user map that are safe to send out to auth'd user."
  [:id :email :permissions])

(defn get-user
  "Gets a user from db. Optionally add WHERE constraints."
  [db-conn & {:keys [where]}]
  (first (db/!select db-conn "users" ["*"] (merge {} where))))

(defn get-user-by-email
  "Gets a user from db by email address"
  [db-conn email]
  (get-user db-conn
            :where {:email email}))

(defn init-session
  [db-conn user client-ip]
  (let [token (util/new-auth-token)]
    (db/!insert db-conn
                "sessions"
                {:user_id (:id user)
                 :token token
                 :ip (or client-ip "")
                 :source "portal"})
    {:success true
     :token token
     :user  (select-keys user safe-authd-user-keys)}))

(defn clear-portal-sessions
  "Clear out all portal sessions that are older than date where date is the
  time in unix-epoch seconds"
  [db-conn user date]
  (let [user-sessions (db/!select db-conn "sessions" ["*"]
                                  {:user_id (:id user)
                                   :source "portal"})
        expired-sessions (filter #(< (-> % :timestamp_created .getTime) date)
                                 user-sessions)
        expired-sessions-id-string (str "id in ("
                                        (string/join
                                         ","
                                         (map :id expired-sessions)) ")")]
    (when-not (empty? expired-sessions)
      (sql/with-connection (db/conn) (sql/delete-rows
                                      "sessions"
                                      [expired-sessions-id-string])))))

(defn login
  "Given an email, password and client-ip, create a new session and return it"
  [db-conn email password client-ip]
  (let [user (get-user-by-email db-conn email)
        session-expiration (c/to-long (t/minus (l/local-now)
                                               (t/days 90)))]
    (cond (nil? user)
          {:success false
           :message "Incorrect email / password combination."}
          (auth-native? user password)
          (do
            (clear-portal-sessions db-conn user session-expiration)
            (init-session db-conn user client-ip))
          :else {:success false
                 :message "Incorrect email / password combination."})))
