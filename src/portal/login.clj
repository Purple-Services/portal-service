(ns portal.login
  (:require [clj-time.coerce :as c]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clojure.java.jdbc :as sql]
            [clojure.string :as string]
            [common.config :as config]
            [common.db :as db]
            [common.sendgrid :as sendgrid]
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

(defn get-user-by-reset-key
  "Gets a user from db by reset_key (for password reset)."
  [db-conn reset-key]
  (get-user db-conn :where {:reset_key reset-key}))

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

(defn forgot-password
  "Only for native accounts; platform-id is email address."
  [db-conn email]
  (let [user (get-user-by-email db-conn email)]
    (if user
      (let [reset-key (util/rand-str-alpha-num 22)]
        (db/!update db-conn
                 "users"
                 {:reset_key reset-key}
                 {:id (:id user)})
        (sendgrid/send-template-email
         email
         "Forgot Password?"
         (str "<h2 style=\"margin: 17px 0px 25px 0px; font-size: 2.5em; "
              "line-height: 1.1em; font-weight: 300; text-align: center; "
              "font-family: 'HelveticaNeue-Light','Helvetica Neue Light',"
              "Helvetica,Arial,sans-serif;\">"
              "Forgot Password?"
              "</h2>"
              "Hi " (:name user) ","
              "<br />"
              "<br />" "Please click the link below to change your password:"
              "<br />" config/base-url "reset-password/" reset-key
              "<br />"
              "<br />" "Thanks,"
              "<br />" "Purple"))
        {:success true
         :message (str "An email has been sent to "
                       email
                       ". Please click the link included in "
                       "that message to reset your password.")})
      {:success false
       :message (str "Sorry, we don't recognize that email address. Are you "
                     "sure you didn't use Facebook or Google to log in?")})))

(defn change-password
  "Only for native accounts."
  [db-conn reset-key password]
  (if-not (string/blank? reset-key) ;; <-- very important check, for security
    (if (valid-password? password)
      (db/!update db-conn
               "users"
               {:password_hash (bcrypt/encrypt password)
                :reset_key ""}
               {:reset_key reset-key})
      {:success false
       :message "Password must be at least 6 characters."})
    {:success false
     :message "Reset Key is blank."}))
