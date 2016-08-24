(defproject ck.react-server "1.0.0-rc1"
  :description "Module that adds support for React/Reagent/Om server-side rendering to Conskit using Nashorn"
  :url "https://github.com/conskit/ck.react-server"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [conskit "1.0.0-rc1"]]
  :profiles {:dev {:source-paths ["dev"]
                   :resource-paths ["dev-resources"]
                   :dependencies [[puppetlabs/trapperkeeper "1.4.1" :classifier "test"]
                                  [puppetlabs/kitchensink "1.3.1" :classifier "test" :scope "test"]
                                  [midje "1.8.3"]
                                  [org.clojure/clojurescript "1.8.51"]
                                  [reagent "0.5.1"]]
                   :plugins [[lein-midje "3.2"]]}})
