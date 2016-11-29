(ns portal.functional.test.cookies)

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


(defn get-cookie-account-manager?
  "Given a response map, return whether or not the user is an account-manager"
  [response]
  (read-string
   (second
    (re-find #"account-manager=([a-zA-Z0-9]*);"
             (first (filter (partial re-matches #"account-manager.*")
                            (get-in response [:headers "Set-Cookie"])))))))

(defn auth-cookie
  "Given a response from /login, create an auth cookie"
  [response]
  (let [token (get-cookie-token response)
        user-id (get-cookie-user-id response)]
    {"cookie" (str "token=" token ";"
                   " user-id=" user-id)}))
