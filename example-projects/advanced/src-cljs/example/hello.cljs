(ns example.hello
  (:require
    [example.crossover.shared :as shared]))

(js/alert (shared/make-example-text))
