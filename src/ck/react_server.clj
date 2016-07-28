(ns ck.react-server
  (:require
    [clojure.tools.logging :as log]
    [puppetlabs.trapperkeeper.core :refer [defservice]]
    [puppetlabs.trapperkeeper.services :refer [service-context]]
    [clojure.java.io :as io]
    [conskit.macros :refer [definterceptor]])
  (:import (javax.script ScriptEngineManager Invocable)))

(defn- render-fn* [path nspace method dev?]
  (let [js (doto (.getEngineByName (ScriptEngineManager.) "nashorn")
             (.eval "var global=global||this,self=self||this,nashorn=!0,console=global.console||{};
             ['error','log','info','warn'].forEach(function(o){o in console||(console[o]=function(){})}),
             ['setTimeout','setInterval','setImmediate','clearTimeout','clearInterval','clearImmediate']
             .forEach(function(o){o in global||(global[o]=function(){})});")
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
      (template-fn (if dev? "<div id=\"dev\"></div>" (render-fn (pr-str state-edn))) (get-in state-edn [1 :meta]) (pr-str state-edn)))))

(definterceptor
  ^:react-server-page
  react-server-page
  "Server side rendering of reactjs/reagent views"
  [f config #{get-render-fn get-meta} req]
  (if config
    (let [[status data] (first (f req))
          render (get-render-fn)
          ok? (= status ::ok)
          {:keys [template-fn]} config
          {:keys [id]} (get-meta)]
      {:status (condp = status
                 ::ok 200
                 ::internal-error 500
                 ::unauthorized 401
                 ::redirect 302
                 ::not-found 404)
       :headers {"Content-Type" "text/html"}
       :body (render template-fn [(if ok? id status)
                                  {:meta (dissoc config :template-fn)
                                   :data data}])})
    (f req)))

(defprotocol CKReactServer
  (get-render-fn [this]))

(defservice
  renderer CKReactServer
  [[:ConfigService get-in-config]
   [:ActionRegistry register-bindings! register-interceptors!]]
  (start [this context]
        (log/info "Starting React Server Rendering Service")
        (let [{:keys [pool-size js-path namespace method dev-mode]} (get-in-config [:react-server])]
          (assoc context :pool (ref (repeatedly pool-size #(render-fn* js-path namespace method (= dev-mode "yes")))))))
  (stop [this context]
        (log/info "Stopping React Server Rendering Service")
        (dissoc context :pool))
  (get-render-fn
    [this]
    (let [pool (get (service-context this) :pool)
          {:keys [js-path namespace method]} (get-in-config [:react-server])]
      (fn render [template-fn state-edn]
        (let [rendr (dosync
                      (let [f (first @pool)]
                        (alter pool rest)
                        f))
              rendr (or rendr (render-fn* js-path namespace method))
              html (rendr template-fn state-edn)]
          (dosync (alter pool conj rendr))
          html)))))
