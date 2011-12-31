(ns example.crossover.shared
  (:require;*CLJSBUILD-REMOVE*;-macros
    [example.crossover.macros :as macros]))

(defn make-example-text []
  (macros/reverse-eval
    ("code" "shared " "from the " "Hello " str))) 
