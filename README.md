# ck.react-server [![Build Status](https://travis-ci.org/conskit/ck.react-server.svg?branch=master)](https://travis-ci.org/conskit/ck.react-server) [![Dependencies Status](https://jarkeeper.com/conskit/ck.react-server/status.svg)](https://jarkeeper.com/conskit/ck.react-server) [![Clojars Project](https://img.shields.io/clojars/v/ck.react-server.svg)](https://clojars.org/ck.react-server)

A nashorn-based react serverside rendering module for [conskit](https://github.com/conskit/conskit)

## Requires
- Java 8 or above (for nashorn)

## Usage
Add the following to your `bootstrap.cfg`:

```
ck.react-server/renderer
```

Add the following to your `config.conf`

```properties
react-server: {
  pool-size: 3 # number of rendering functions available at runtime
  js-path: "public/js/compiled/app.js" # clojurescript output file
  namespace: "myapp.core" # clojurescript namespace  
  method: "render_to_string" # method that executes react's renderToString
  dev-mode: yes # dev-mode will not render using nashorn and will allow rendering on the client
}
```

Add the dependency, binding and interceptor in your serivice

```clojure
(ns myapp
  (:require [ck.react-server :as ckrs]))

(defservice
  my-service
  [[:ActionRegistry register-bindings! register-interceptors!]
   [:CKReactServer get-render-fn]]
  (init [this context]
    ...
    (register-bindings {:get-render-fn get-render-fn}))
    (register-interceptors [ckrs/react-server-page])
  ...)
```

Create a template function like the following

```clojure
;; Hiccup is used here but this could be any templating tool that returns HTML as a string
(defn app-template
  [rendered-html meta state]
  (hiccup/html5
    [:head
      [:meta {:name "keywords" :content (:keywords meta)}]
      [:title (:title meta)]]
    [:body
     [:div#app rendered-html]
     [:script#app-state {:type "application/edn"} app-state]
     [:script "myapp.core.init()"]]))
```

Annotate the actions with `:react-server-page` and return the status along with any data needed for the page

```clojure
(action
  ^{:react-server-page {:title "My Awesome App"
                        :keywords "my, awesome, app, wow"
                        :template-fn app-template}
  awesome-page
  [req]
  ;; awesome logic
  [:ck.react-server/ok {:page :data}])
```

The status can be an explicit code e.g. `[501 {:page :data}]` or one of 

```clojure
:ck.react-server/ok ;; 200
:ck.react-server/internal-error ;;500
:ck.react-server/unauthorized ;; 401
:ck.react-server/redirect ;;302
:ck.react-server/not-found ;; 404
```

You can also return additional response data (such as cookies or session data) by specifying a third entry in the returned vector

```clojure
(action
  ^{:react-server-page {:title "My Awesome App"
                        :keywords "my, awesome, app, wow"
                        :template-fn app-template}
  awesome-page
  [req]
  ;; awesome logic
  [:ck.react-server/ok {:page :data} {:cookies {"secret-cookie" {:value "emosewa"}}}])
```

## Documentation

### Service

Key: `:CKReactServer`

|Method | Description|
|----|---|
|`get-render-fn` | Returns a rendering function |

### Interceptor


## License

Copyright © 2016 Jason Murphy

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
