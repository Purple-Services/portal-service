(ns portal.login
  (:require [common.db :as db]
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
  (first (db/!select db-conn "dashboard_users" ["*"] (merge {} where))))

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
                 :ip (or client-ip "")})
    {:success true
     :token token
     :user  (select-keys user safe-authd-user-keys)}))

(defn login
  "Given an email, password and client-ip, create a new session and return it"
  [db-conn email password client-ip]
  (let [user (get-user-by-email db-conn email)]
    (cond (nil? user)
          {:success false
           :message "Incorrect email / password combination."}
          (auth-native? user password)
          (init-session db-conn user client-ip)
          :else {:success false
                 :message "Incorrect email / password combination."})))
