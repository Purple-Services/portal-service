(ns portal.test.utils
  (:require [cheshire.core :as cheshire]
            [ring.mock.request :as mock]))

(defn get-bouncer-error
  "Given a validation map, get the error for the ks vector
  ex: (get-bouncer-error (b/validate {:email \"\"
                                      :name \"Foo Bar\"}
                                     accounts/child-account-validations)
                         [:email])
  will get the errors associated with :email."
  [validation-map ks]
  (get-in (second validation-map)
          (vec (concat [:bouncer.core/errors] ks))))

(defn response-body-json
  [response]
  (cheshire/parse-string
   (:body response)
   true))

(defn get-uri-json
  "Given a request-type key, uri and optional json-body for post requests,
  return the response. If there is a json body, keywordize it."
  [request-type uri & [{:keys [json-body headers]
                        :or {json-body nil
                             headers nil}}]]
  (let [json-body (str (if (nil? json-body)
                         json-body
                         (cheshire/generate-string
                          json-body)))
        mock-request (-> (mock/request
                          request-type uri
                          json-body)
                         (assoc :headers headers)
                         (mock/content-type
                          "application/json"))
        response (portal.handler/handler
                  mock-request)]
    (assoc response :body (response-body-json response))))
