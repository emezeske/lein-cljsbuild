(ns example.shared
  #?(:clj (:require
           [example.macros :as macros]))
  #?(:cljs (:require-macros [example.macros :as macros])))

(defn make-example-text []
  (macros/reverse-eval
    ("code" "shared " "from the " "Hello " str)))
