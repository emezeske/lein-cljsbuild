(ns advanced.shared
  #?(:clj (:require
           [advanced.macros :as macros]))
  #?(:cljs (:require-macros [advanced.macros :as macros])))

(defn make-example-text []
  (macros/reverse-eval
    ("code" "shared " "from the " "Hello " str)))
