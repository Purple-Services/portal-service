(defproject portal "0.5.5-SNAPSHOT"
  :description "Purple Portal Service"
  :url "http://dashboard.purplapp.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha11"]
                 [buddy/buddy-auth "0.8.1"]
                 [clj-time "0.12.0"]
                 [bouncer "1.0.0"]
                 [compojure "1.5.1"]
                 [enlive "1.1.6"]
                 [ring/ring-json "0.4.0"]
                 [ring-cors "0.1.8"]
                 [ring/ring-ssl "0.2.1"]
                 [ring/ring-devel "1.5.0"]
                 [common "2.0.2-SNAPSHOT"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-beanstalk "0.2.7"]
            [com.jakemccrary/lein-test-refresh "0.17.0"]]
  :test-refresh {:notify-on-success false
                 :notify-command ["growlnotify" "-m"]}
  :ring {:handler portal.handler/handler
         :port 3002
         :auto-reload? true
         :auto-refresh? false
         :browser-uri "/"
         :reload-paths ["src"]}
  :aws {:beanstalk {:app-name "portal"
                    :environments [{:name "portal-dev"}
                                   {:name "portal-prod"}]
                    :s3-bucket "leinbeanstalkpurple"
                    :region "us-west-2"}}
  :profiles {:shared [{:dependencies
                       [[javax.servlet/servlet-api "2.5"]
                        [org.seleniumhq.selenium/selenium-java "2.48.2"]
                        [clj-webdriver "0.7.2"]
                        [ring "1.5.0"]
                        [ring/ring-jetty-adapter "1.5.0"]
                        [ring/ring-mock "0.3.0"]
                        [pjstadig/humane-test-output "0.6.0"]]
                       :injections
                       [(require 'pjstadig.humane-test-output)
                        (pjstadig.humane-test-output/activate!)]}]
             :local [:shared :profiles/local
                     {:env {:base-url "http://localhost:3002/"}}]
             :dev [:shared :profiles/dev
                   {:env {:base-url "http://localhost:3002/"}}]
             :prod [:shared :profiles/prod
                    {:env {:base-url "http://localhost:3002/"}}]
             :app-integration-test [:shared
                                    {:env {:test-db-host "localhost"
                                           :test-db-name "ebdb_test"
                                           :test-db-port "3306"
                                           :test-db-user "root"
                                           :test-db-password ""}
                                     :jvm-opts ["-Dwebdriver.chrome.driver=/usr/lib/chromium-browser/chromedriver"]
                                     :plugins [[lein-environ "1.1.0"]]}]
             :app-integration-dev-deploy
             {:aws {:access-key ~(System/getenv "AWS_ACCESS_KEY")
                    :secret-key ~(System/getenv "AWS_SECRET_KEY")}}})
