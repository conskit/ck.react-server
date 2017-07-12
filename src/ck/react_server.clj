(ns ck.react-server
  (:require
    [clojure.tools.logging :as log]
    [puppetlabs.trapperkeeper.core :refer [defservice]]
    [puppetlabs.trapperkeeper.services :refer [service-context]]
    [clojure.java.io :as io]
    [conskit.macros :refer [definterceptor]])
  (:import (javax.script ScriptEngineManager Invocable)))

(defn- render-fn* [setup path nspace method]
  (let [js (doto (.getEngineByName (ScriptEngineManager.) "nashorn")
             (.eval (-> setup
                        (io/resource)
                        (io/reader)))
             (.eval (-> path
                        (io/resource)
                        (io/reader))))
        view (.eval js nspace)
        render-fn (fn [edn]
                    (.invokeMethod
                      ^Invocable js
                      view
                      method
                      (-> edn
                          list
                          object-array)))]
    (fn [template-fn state-edn]
      (template-fn (render-fn (pr-str state-edn)) (get-in state-edn [1 :meta]) (pr-str state-edn)))))

(definterceptor
  ^:react-server-page
  react-server-page
  "Server side rendering of reactjs/reagent views"
  [f config #{get-render-fn get-meta} req]
  (if config
    (let [[status data options] (f req)
          render (get-render-fn)
          ok? (= status ::ok)
          state-only? (= "true" (get (:headers req) "x-state-only"))
          {:keys [template-fn]} config
          {:keys [id]} (get-meta)
          state [(if ok? id status)
                 {:meta (dissoc config :template-fn)
                  :data data}]]
      (merge {:status  (condp = status
                         ::ok 200
                         ::internal-error 500
                         ::unauthorized 401
                         ::redirect 302
                         ::not-found 404
                         status)
              :headers {"Content-Type" "text/html"}
              :body    (if state-only? (pr-str state) (render template-fn state))}
             options))
    (f req)))

(defprotocol CKReactServer
  (get-render-fn [this]))

(defservice
  renderer CKReactServer
  [[:ConfigService get-in-config]
   [:ActionRegistry register-bindings! register-interceptors!]]
  (start [this context]
        (log/info "Starting React Server Rendering Service")
        (let [{:keys [pool-size js-path namespace method dev-mode setup-script]} (get-in-config [:react-server])]
          (assoc context :pool (ref (repeatedly pool-size (if (= dev-mode "yes")
                                                            (fn []
                                                              #(apply %1 ["<div id=\"dev\"></div>" (get-in %2 [1 :meta]) (pr-str %2)]))
                                                            #(render-fn* (or setup-script "js/setup.js")  js-path namespace method)))))))
  (stop [this context]
        (log/info "Stopping React Server Rendering Service")
        (dissoc context :pool))
  (get-render-fn
    [this]
    (let [pool (get (service-context this) :pool)
          {:keys [js-path namespace method dev-mode setup-script]} (get-in-config [:react-server])]
      (fn render [template-fn state-edn]
        (let [rendr (dosync
                      (let [f (first @pool)]
                        (alter pool rest)
                        f))
              rendr (or rendr (if (= dev-mode "yes")
                                #(apply %1 ["<div id=\"dev\"></div>" (get-in %2 [1 :meta]) (pr-str %2)])
                                (render-fn* (or setup-script "js/setup.js")  js-path namespace method)))
              html (rendr template-fn state-edn)]
          (dosync (alter pool conj rendr))
          html)))))
