(ns portal.functional.test.selenium
  (:require [clj-webdriver.taxi :refer :all]
            [common.db :as db]
            [environ.core :refer [env]]
            [portal.test.db-tools :refer [setup-ebdb-test-pool!
                                          clear-test-database
                                          setup-ebdb-test-for-conn-fixture
                                          clear-and-populate-test-database
                                          clear-and-populate-test-database-fixture]]
            [portal.handler]
            [ring.adapter.jetty :refer [run-jetty]]))

;; note: you will need a bit of elisp in order be able to use load the file
;; without having the vars named improperly
;; here is an example of what you will need below
;; ex:  ~/emacs/cider/cider-config.el
;; (defun portal-clj-reset ()
;;   (when (string= (buffer-name) "selenium.clj")
;;     (cider-interactive-eval
;;      "(portal.functional.test.selenium/reset-vars!)")))
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

