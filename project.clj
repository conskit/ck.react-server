(defproject ck.react-server "0.1.0-SNAPSHOT"
  :description "FIXME: ck.react-server module for Conskit"
  :url "https://website.com/example/ck.react-server"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [conskit "0.3.0-SNAPSHOT"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[puppetlabs/trapperkeeper "1.4.1" :classifier "test"]
                                  [puppetlabs/kitchensink "1.3.1" :classifier "test" :scope "test"]
                                  [midje "1.8.3"]
                                  [org.clojure/clojurescript "1.8.51"]
                                  [reagent "0.5.1"]]
                   :plugins [[lein-midje "3.2"]]}})
