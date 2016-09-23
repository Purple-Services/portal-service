(ns portal.functional.test.portal
  (:require [clj-webdriver.taxi :refer :all]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [common.db :as db]
            [environ.core :refer [env]]
            [portal.test.db-tools :refer [setup-ebdb-test-pool!
                                          clear-test-database
                                          setup-ebdb-test-for-conn-fixture
                                          clear-and-populate-test-database
                                          clear-and-populate-test-database-fixture]]
            [portal.test.login-test :refer [register-user!]]
            [portal.handler]
            [ring.adapter.jetty :refer [run-jetty]]))

;; note: you will need a bit of elisp in order be able to use load the file
;; without having the vars named improperly
;; here is an example of what you will need below:
;; (defun portal-clj-reset ()
;;   (when (string= (buffer-name) "portal.clj")
;;     (cider-interactive-eval
;;      "(reset-vars!)")))

;; (add-hook 'cider-mode-hook
;; 	  (lambda ()
;; 	    (add-hook 'cider-file-loaded-hook 'portal-clj-reset)))

;; for manual testing:
;; (setup-test-env!) ; make sure profiles.clj was loaded with
;;                   ; :base-url "http:localhost:5744/"
;; -- run tests --
;; (reset-db!) ; note: most tests will need this run between them anyhow
;; -- run more tests
;; (stop-server server)
;; (stop-browser)


;; normally, the test server runs on port 3000. If you would like to manually
;; run tests, you can set this to (def test-port 3000) in the repl
;; just reload this file (C-c C-l in cider) when running
(def test-port 5745)
(def test-base-url (str "http://localhost:" test-port "/"))
(def base-url test-base-url)


;; common elements
(def logout-lg-xpath
  {:xpath "//ul/li[contains(@class,'hidden-lg')]//a[text()='LOG OUT']"})
(def logout-sm-xpath
  {:xpath "//ul[contains(@class,'hidden-xs')]/li//a[text()='LOG OUT']"})

(def login-button {:xpath "//button[text()='LOGIN']"})

(def login-email-input
  {:xpath "//input[@type='text' and @placeholder='email']"})
(def forgot-password-link {:xpath "//a[@class='forgot-password']"})

(defn start-server [port]
  (let [_ (setup-ebdb-test-pool!)
        server (run-jetty #'portal.handler/handler
                          {:port port
                           :join? false
                           })]
    server))

(defn stop-server [server]
  (do
    (clear-test-database)
    ;; close out the db connection
    (.close (:datasource (db/conn)))
    (.stop server)))

(defn with-server [t]
  (let [server (start-server test-port)]
    (t)
    (stop-server server)))

(defn start-browser []
  (set-driver! {:browser :chrome}))

(defn stop-browser []
  (quit))

(defn with-browser [t]
  (start-browser)
  (t)
  (stop-browser))

(defn with-redefs-fixture [t]
  (with-redefs [common.config/base-url test-base-url]
    (t)))

(use-fixtures :once with-server with-browser with-redefs-fixture)
(use-fixtures :each clear-and-populate-test-database-fixture)

;; beging fns for testing at the repl
(defn reset-vars!
  []
  (def base-url (env :base-url))
  ;; obviously means that :base-url will use port 5744
  (def test-port 5744))

(defn set-server!
  []
  (def server (start-server test-port))
  (setup-ebdb-test-pool!))

(defn setup-test-env!
  []
  (reset-vars!)
  (set-server!)
  (start-browser))

(defn reset-db! []
  (clear-and-populate-test-database))

;; end fns for testing at the repl

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
  (to (str base-url "login")))

(defn go-to-uri
  "Given an uri, go to it"
  [uri]
  (to (str base-url uri)))


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
      (register-user! {:db-conn (db/conn)
                       :platform-id email
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
        full-name "James Borden"]
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
        (register-user! {:db-conn (db/conn)
                         :platform-id email
                         :password password
                         :full-name full-name})
        (go-to-uri "login")
        (input-text (find-element  login-email-input) email)
        (click (find-element forgot-password-link))
        (check-success-alert "An email has been sent to james@purpleapp.com. Please click the link included in that message to reset your password."))
      )))
