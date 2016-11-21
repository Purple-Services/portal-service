(ns portal.test.utils)

(defn get-bouncer-error
  "Given a validation map, get the error for the ks vector
  ex: (get-bouncer-error (b/validate {:email \"\"
                                      :full-name \"Foo Bar\"}
                                     accounts/child-account-validations)
                         [:email])
  will get the errors associated with :email."
  [validation-map ks]
  (get-in (second validation-map)
          (vec (concat [:bouncer.core/errors] ks))))
