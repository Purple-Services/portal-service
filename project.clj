(defproject portal "0.3.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[buddy/buddy-auth "0.8.1"]
                 [clj-time "0.12.0"]
                 [common "1.1.4-SNAPSHOT"]
                 [compojure "1.5.1"]
                 [enlive "1.1.6"]
                 [org.clojure/clojure "1.9.0-alpha11"]
                 [ring/ring-core "1.5.0"]
                 [ring/ring-devel "1.5.0"]
                 [ring/ring-json "0.4.0"]
                 [ring-cors "0.1.8"]]
  :plugins [[lein-ring "0.9.7"]
            [lein-beanstalk "0.2.7"]]
  :ring {:handler portal.handler/handler
         :port 3002
         :auto-reload? true
         :auto-refresh? false
         :browser-uri "/"
         :reload-paths ["src"]}
  :aws {:beanstalk {:environments [{:name "portal-dev"}]
                    :s3-bucket "leinbeanstalkpurple"
                    :region "us-west-2"}})
