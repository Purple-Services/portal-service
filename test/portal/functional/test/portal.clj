(ns portal.functional.test.portal
  (:require [clj-webdriver.taxi :refer :all]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [common.db :as db]
            [environ.core :refer [env]]
            [portal.test.db-tools :refer [setup-ebdb-test-pool!
                                          clear-test-database]]
            [portal.test.login-test :refer [register-user!]]
            [portal.handler]
            [ring.adapter.jetty :refer [run-jetty]]))

;; normally, the test server runs on port 3000. If you would like to manually
;; run tests, you can set this to (def test-port 3000) in the repl
;; just reload this file (C-c C-l in cider) when running
(def test-port 5744)
(def base-url (env :base-url))

;; common elements
(def logout-lg-xpath
  {:xpath "//ul/li[contains(@class,'hidden-lg')]//a[text()='LOG OUT']"})
(def logout-sm-xpath
  {:xpath "//ul[contains(@class,'hidden-xs')]/li//a[text()='LOG OUT']"})

(def login-xpath {:xpath "//button[text()='LOGIN']"})

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

(defn go-to-login-page
  "Navigate to the portal"
  []
  (to (str base-url  "login")))

(use-fixtures :each with-browser with-server)

(defn login-portal
  "Login with the client using email and password as credentials"
  [email password]
  (go-to-login-page)
  (let [email-input    (find-element {:xpath "//input[@type='text']"})
        password-input (find-element {:xpath "//input[@type='password']"})]
    (input-text email-input email)
    (input-text password-input password)
    (click (find-element login-xpath))))

(defn logout-portal
  "Logout, assuming the portal has already been logged into"
  []
  (click (if (visible? (find-element logout-lg-xpath))
           (find-element logout-lg-xpath)
           (find-element logout-sm-xpath))))

(deftest login-tests
  (let [email "foo@bar.com"
        password "foobar"
        alert-danger {:xpath (str "//div[contains(@class,'alert-danger')]")}
        logout {:xpath "//a[text()='LOG OUT']"}
        full-name "Foo Bar"]
    (testing "Login with a username and password that doesn't exist"
      (go-to-login-page)
      (login-portal email password)
      (wait-until #(exists?
                    alert-danger))
      (is (= (text (find-element
                    alert-danger))
             "Error:\nError: Incorrect email / password combination.")))
    (testing "Create a user, login with credentials"
      (register-user! {:db-conn (db/conn)
                       :platform-id email
                       :password password
                       :full-name full-name})
      (go-to-login-page)
      (login-portal email password)
      (wait-until #(exists? logout))
      (is (exists? (find-element logout))))
    (testing "Log back out."
      (logout-portal)
      (is (exists? (find-element login-xpath))))))
