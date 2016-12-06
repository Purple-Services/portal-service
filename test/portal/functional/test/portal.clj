(ns portal.functional.test.portal
  (:require [clj-webdriver.taxi :refer :all]
            [clojure.string :as string]
            [clojure.test :refer [deftest is testing use-fixtures run-tests]]
            [common.db :as db]
            [portal.functional.test.selenium :as selenium]
            [portal.test.db-tools :refer [setup-ebdb-test-pool!
                                          clear-test-database
                                          setup-ebdb-test-for-conn-fixture
                                          clear-and-populate-test-database
                                          clear-and-populate-test-database-fixture
                                          reset-db!]]
            [portal.login :as login]
            [portal.users :as users]
            [portal.test.login-test :refer [register-user!]]))

;; for manual testing:
;; (selenium/startup-test-env!) ; make sure profiles.clj was loaded with
;;                   ; :base-url "http:localhost:5744/"
;; -- run tests --
;; (reset-db!) ; note: most tests will need this run between them anyhow
;; -- run more tests
;; (selenium/shutdown-test-env!


;; common elements
(def logout-lg-xpath
  {:xpath "//ul/li[contains(@class,'hidden-lg')]//a[text()='LOG OUT']"})
(def logout-sm-xpath
  {:xpath "//ul[contains(@class,'hidden-xs')]/li//a[text()='LOG OUT']"})

(def logout {:xpath "//a[text()='LOG OUT']"})

(def login-button {:xpath "//button[text()='LOGIN']"})

(def login-email-input
  {:xpath "//input[@type='text' and @placeholder='email']"})
(def forgot-password-link {:xpath "//a[@class='forgot-password']"})

(def vehicles-link {:xpath "//li/a/div[text()='VEHICLES']"})
(def add-vehicle-button {:xpath "//div[@id='vehicles']//button[text()=' Add']"})
;; vehicle form
(def vehicle-form-make {:xpath "//div[@id='vehicles']//form//input[@placeholder='Make']"})
(def vehicle-form-model {:xpath "//div[@id='vehicles']//form//input[@placeholder='Model']"})
(def vehicle-form-year {:xpath "//div[@id='vehicles']//form//input[@placeholder='Year']"})
(def vehicle-form-color {:xpath "//div[@id='vehicles']//form//input[@placeholder='Color']"})
(def vehicle-form-license-plate
  {:xpath "//div[@id='vehicles']//form//input[@placeholder='License Plate']"})
(def vehicle-form-fuel-type
  {:xpath "//div[@id='vehicles']//form//select"})
(def vehicle-form-only-top-tier-gas?
  {:xpath "//div[@id='vehicles']//form//p[text()='Only Top Tier Gas?']/ancestor::div[@class='row']//input[@type='checkbox']"})
(def vehicle-form-dismiss
  {:xpath "//div[@id='vehicles']//form//button[text()='Dismiss']"})
(def vehicle-form-save
  {:xpath "//div[@id='vehicles']//form//button[text()='Save']"})
(def vehicle-form-yes
  {:xpath "//div[@id='vehicles']//form//button[text()='Yes']"})
(def vehicle-form-no
  {:xpath "//div[@id='vehicles']//form//button[text()='No']"})
(def vehicles-table
  {:xpath "//div[@id='vehicles']//table"})
(def no-vehicles-message
  {:xpath "//h3[text()='No orders currently associated with account']"})

(def no-orders-message
  {:xpath "//h3[text()='No orders currently associated with account']"})

(defn select-checkbox
  "If checked? is false, the checkbox is unchecked. Otherwise, the check is
  checked."
  [checkbox checked?]
  (when (or
         (and (not checked?)
              (selected? checkbox))
         (and checked?
              (not (selected? checkbox))))
    (click checkbox)))

(defn fill-vehicle-form
  [{:keys [make model year color license-plate fuel-type only-top-tier-gas?]
    :or {make "Nissan"
         model "Altima"
         year "2006"
         color "Blue"
         license-plate "FOOBAR"
         fuel-type "87 Octane"
         only-top-tier-gas? true}}]
  (clear vehicle-form-make)
  (input-text vehicle-form-make make)
  (clear vehicle-form-model)
  (input-text vehicle-form-model model)
  (clear vehicle-form-year)
  (input-text vehicle-form-year year)
  (clear vehicle-form-color)
  (input-text vehicle-form-color color)
  (clear vehicle-form-license-plate)
  (input-text vehicle-form-license-plate license-plate)
  (select-by-text vehicle-form-fuel-type fuel-type)
  (select-checkbox vehicle-form-only-top-tier-gas? only-top-tier-gas?))

(use-fixtures :once selenium/with-server selenium/with-browser
  selenium/with-redefs-fixture)
(use-fixtures :each clear-and-populate-test-database-fixture)

;; this function is used to slow down clojure so the browser has time to catch
;; up. If you are having problems with tests passing, particuarly if they appear
;; to randomly fail, try increasing the amount of sleep time before the call
;; that is failing
(defn sleep
  "Sleep for ms."
  [& [ms]]
  (let [default-ms 700
        time (or ms default-ms)]
    (Thread/sleep time)))

(defn go-to-login-page
  "Navigate to the portal"
  []
  (to (str selenium/base-url "login")))

(defn go-to-uri
  "Given an uri, go to it"
  [uri]
  (to (str selenium/base-url uri)))

(defn login-portal
  "Login with the client using email and password as credentials"
  [email password]
  (go-to-uri "login")
  (let [email-input    (find-element login-email-input)
        password-input (find-element {:xpath "//input[@type='password']"})]
    (input-text email-input email)
    (input-text password-input password)
    (click (find-element login-button))))

(defn logout-portal
  "Logout, assuming the portal has already been logged into"
  []
  (click (if (visible? (find-element logout-lg-xpath))
           (find-element logout-lg-xpath)
           (find-element logout-sm-xpath))))

(defn check-error-alert
  "Wait for an error alert to appear and test that it says msg"
  [msg]
  (let [alert-danger {:xpath "//div[contains(@class,'alert-danger')]"}]
    (wait-until #(exists?
                  alert-danger))
    (is (= msg
           (text (find-element
                  alert-danger))))))

(defn check-success-alert
  "Wait for an error alert to appear and test that it says msg"
  [msg]
  (let [alert-danger {:xpath "//div[contains(@class,'alert-success')]"}]
    (wait-until #(exists?
                  alert-danger))
    (is (= msg
           (text (find-element
                  alert-danger))))))

(deftest login-tests
  (let [email "foo@bar.com"
        password "foobar"
        logout {:xpath "//a[text()='LOG OUT']"}
        full-name "Foo Bar"]
    (testing "Login with a username and password that doesn't exist"
      (go-to-uri "login")
      (login-portal email password)
      (check-error-alert "Error: Incorrect email / password combination."))
    (testing "Create a user, login with credentials"
      (register-user! {:platform-id email
                       :password password
                       :full-name full-name})
      (go-to-uri "login")
      (login-portal email password)
      (wait-until #(exists? logout))
      (is (exists? (find-element logout))))
    (testing "Log back out."
      (logout-portal)
      (is (exists? (find-element login-button))))))

(deftest forgot-password
  (let [email "james@purpleapp.com"
        password "foobar"
        full-name "James Borden"
        reset-button-xpath {:xpath "//button[text()='RESET PASSWORD']"}]
    (testing "User tries to reset key without reset key"
      (go-to-uri "reset-password/vs5YI50YZptjyONSoIofm7")
      (is (= (text (find-element {:xpath "//h1[text()='Page Not Found']"}))
             "Page Not Found")))
    (testing "User tries to reset password without having registed an account"
      (go-to-uri "login")
      (input-text (find-element login-email-input) email)
      (click (find-element forgot-password-link))
      (check-error-alert "Error: Sorry, we don't recognize that email address. Are you sure you didn't use Facebook or Google to log in?"))
    (testing "User resets password"
      (with-redefs [common.sendgrid/send-template-email
                    (fn [to subject message]
                      (println "No reset password email was actually sent"))]
        (register-user! {:platform-id email
                         :password password
                         :full-name full-name})
        (go-to-uri "login")
        (input-text (find-element  login-email-input) email)
        (click (find-element forgot-password-link))
        (check-success-alert "An email has been sent to james@purpleapp.com. Please click the link included in that message to reset your password.")
        (let [reset-key (:reset_key (login/get-user-by-email email))
              reset-url (str "reset-password/" reset-key)
              password-xpath {:xpath "//input[@type='password' and @placeholder='password']"}
              confirm-password-xpath {:xpath "//input[@type='password' and @placeholder='confirm password']"}
              new-password "bazqux"
              wrong-confirm-password "quxxcorgi"
              too-short "foo"]
          (go-to-uri reset-url)
          (testing "User tries to reset the password with non-matching passwords"
            (input-text (find-element password-xpath) new-password)
            (input-text (find-element confirm-password-xpath)
                        wrong-confirm-password)
            (click (find-element reset-button-xpath))
            (check-error-alert "Error: Passwords do not match."))
          (testing "User tries to reset the password with a password that is too short"
            (clear (find-element password-xpath))
            (wait-until #(string/blank?
                          (value (find-element password-xpath))))
            (is (string/blank?
                 (value (find-element password-xpath))))
            (input-text (find-element password-xpath) too-short)
            (clear (find-element confirm-password-xpath))
            (wait-until #(string/blank?
                          (value (find-element confirm-password-xpath))))
            (is (string/blank?
                 (value (find-element confirm-password-xpath))))
            (input-text (find-element confirm-password-xpath) too-short)
            (click (find-element reset-button-xpath))
            (check-error-alert "Error: Password must be at least 6 characters."))
          (testing "User can still login, even though there is a reset key"
            (go-to-uri "login")
            (login-portal email password))
          (testing "User can reset password"
            (go-to-uri "logout")
            (go-to-uri reset-url)
            (input-text (find-element password-xpath) new-password)
            (input-text (find-element confirm-password-xpath)
                        new-password)
            (click (find-element reset-button-xpath))
            ;; log in with new password
            (testing "...and login with new password"
              (wait-until #(exists? login-email-input))
              (input-text login-email-input email)
              (input-text password-xpath new-password)
              (click (find-element login-button))
              (wait-until #(exists? {:xpath "//a[text()='LOG OUT']"}))
              (is (exists? {:xpath "//a[text()='LOG OUT']"})))))))))

(deftest account-manager-tests
  (let [email "manager@bar.com"
        password "manager"
        full-name "Manager"
        ;; register a user
        _ (register-user! {:platform-id email
                           :password password
                           :full-name full-name})
        manager (users/get-user-by-email email)]
    (testing "Account manager logs in, but they are not yet an account manager"
      (go-to-uri "login")
      (login-portal email password)
      (wait-until #(exists? logout))
      (is (exists? (find-element logout))))))

(defn create-vehicle
  [vehicle-map]
  (click add-vehicle-button)
  (wait-until #(exists? vehicle-form-make))
  ;; check to make sure defaults are turned off
  (fill-vehicle-form vehicle-map)
  (click vehicle-form-save)
  (wait-until #(exists? vehicle-form-yes))
  (click vehicle-form-yes))

(defn vehicle-map->vehicle-table-row
  [{:keys [make model year color license-plate fuel-type only-top-tier-gas?]}]
  (str make " " model " " color " " year " " license-plate " " fuel-type " "
       (if only-top-tier-gas?
         "Yes"
         "No")))

(deftest selenium-regular-user
  (let [email "foo@bar.com"
        password "foobar"
        full-name "Foo Bar"
        ;; register a user
        _ (register-user! {:platform-id email
                           :password password
                           :full-name full-name})
        first-vehicle {:make "Nissan"
                       :model "Altima"
                       :year "2006"
                       :color "Blue"
                       :license-plate "FOOBAR"
                       :fuel-type "91 Octane"
                       :only-top-tier-gas? false}
        second-vehicle {:make "Honda"
                        :model "Accord"
                        :year "2009"
                        :color "Black"
                        :license-plate "BAZQUX"
                        :fuel-type "87 Octane"
                        :only-top-tier-gas? true}
        third-vehicle {:make "Ford"
                       :model "F150"
                       :year "1995"
                       :color "White"
                       :license-plate "QUUXCORGE"
                       :fuel-type "91 Octane"
                       :only-top-tier-gas? true}]
    (testing "A normal user can login"
      (go-to-uri "login")
      (login-portal email password)
      (wait-until #(exists? logout))
      (is (exists? (find-element logout))))
    (testing "user can add vehicle"
      (wait-until #(exists? no-orders-message))
      ;; there shouldn't be an orders table
      (is (exists? no-orders-message))
      ;; click on the vehicles link
      (wait-until #(exists? vehicles-link))
      (click vehicles-link)
      ;; there shouldn't be a table
      (is (exists? no-vehicles-message))
      (create-vehicle first-vehicle)
      (wait-until #(exists? vehicles-table))
      ;; there should only be one vehicle
      (is (= 1
             (count
              (find-elements
               {:xpath "//div[@id='vehicles']//table/tbody/tr"}))))
      ;; the first row should correspond to the first-vehicle
      (is (= (vehicle-map->vehicle-table-row
              first-vehicle)
             (text
              {:xpath "//div[@id='vehicles']//table/tbody/tr[position()=1]"})))
      ;; add another vehicle
      (create-vehicle second-vehicle)
      (wait-until #(exists? vehicles-table))
      ;; now there should be two vehicles
      (wait-until #(= 2
                      (count
                       (find-elements
                        {:xpath "//div[@id='vehicles']//table/tbody/tr"}))))
      (is (= 2
             (count
              (find-elements
               {:xpath "//div[@id='vehicles']//table/tbody/tr"}))))
      ;; the second row should correspond to the second-vehicle
      (is (= (vehicle-map->vehicle-table-row
              second-vehicle)
             (text
              {:xpath "//div[@id='vehicles']//table/tbody/tr[position()=2]"})))
      ;; 1 and 2 items is trivial, check that 3 work
      ;; add another vehicle
      (create-vehicle third-vehicle)
      (wait-until #(exists? vehicles-table))
      ;; now there should be two vehicles
      (wait-until #(= 3
                      (count
                       (find-elements
                        {:xpath "//div[@id='vehicles']//table/tbody/tr"}))))
      (is (= 3
             (count
              (find-elements
               {:xpath "//div[@id='vehicles']//table/tbody/tr"}))))
      ;; the second row should correspond to the second-vehicle
      (is (= (vehicle-map->vehicle-table-row
              third-vehicle)
             (text
              {:xpath "//div[@id='vehicles']//table/tbody/tr[position()=3]"})))
      )))
