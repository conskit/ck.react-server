(ns test.core
  (:require [reagent.core :as r]
            [cljs.reader :as edn]))

(defn some-component []
  [:div#foo
   [:h3 "I am a component!"]
   [:p.someclass
    "I have " [:strong "bold"]
    [:span {:style {:color "red"}} " and red"]
    " text."]])

(defn calling-component []
  [:div "Parent component"
   [some-component]])


(defn ^:export render-to-string
  "Takes an app state as EDN and returns the HTML for that state.
  It can be invoked from JS as `hrubix.core.render_to_string(edn)`."
  [state-edn]
  (let [state (edn/read-string state-edn)]
    (r/render-to-string [calling-component])))