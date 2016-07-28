(ns ck.react-server-test
  (:require
    [puppetlabs.trapperkeeper.app :as app]
    [puppetlabs.trapperkeeper.core :refer [defservice]]
    [puppetlabs.trapperkeeper.services :refer [service-context]]
    [puppetlabs.trapperkeeper.testutils.bootstrap :refer [with-app-with-cli-data]]
    [conskit.core :as ck]
    [conskit.macros :refer :all]
    [conskit.protocols :as ckp]
    [ck.react-server :as rs]
    [cljs.build.api :as api])
  (:use midje.sweet))


(defcontroller
  my-controller
  []
  (action
    ^{:react-server-page {:title "My First Page"
                          :template-fn (fn [rendered-html _ _]
                                         rendered-html)}}
    my-action
    [req]
    {:ck.react-server/ok {}}))

(defprotocol ResultService
  (get-result [this]))

(defservice
  test-service ResultService
  [[:ActionRegistry get-action register-controllers! register-interceptors! register-bindings!]
   [:CKReactServer get-render-fn]]
  (init [this context]
        (register-controllers! [my-controller])
        (register-interceptors! [rs/react-server-page])
        (register-bindings! {:get-render-fn get-render-fn})
        (api/build "dev" {:output-to "dev-resources/out/app.js"
                          :output-dir "dev-resources/out"
                          :optimizations :advanced})
        context)
  (start [this context]
         {:result (get-action ::my-action)})
  (get-result [this]
              (:result (service-context this))))

(with-app-with-cli-data
  app
  [ck/registry rs/renderer test-service]
  {:config "./dev-resources/test-config.conf"}
  (let [serv (app/get-service app :ResultService)
        res (get-result serv)]
    (fact (re-find #"<div id=\"foo\"" (:body (ckp/invoke res {}))) => "<div id=\"foo\"")))

(with-app-with-cli-data
  app
  [ck/registry rs/renderer test-service]
  {:config "./dev-resources/test-config2.conf"}
  (let [serv (app/get-service app :ResultService)
        res (get-result serv)]
    (fact (re-find #"<div id=\"dev\"" (:body (ckp/invoke res {}))) => "<div id=\"dev\"")))
