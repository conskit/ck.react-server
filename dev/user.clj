(ns user
  (:require [midje.repl :as mr])
  (:use [clojure.repl]))

(defn autotest
  "Start Auto testing"
  []
  (mr/autotest :files "src/ck" "test/ck"))
