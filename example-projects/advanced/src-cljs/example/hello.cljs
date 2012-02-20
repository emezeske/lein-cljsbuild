(ns example.hello
  (:require
    [example.crossover.shared :as shared]))

(defn ^:export say-hello [] 
  (js/alert (shared/make-example-text))) 

(defn add-some-numbers [& numbers]
  (apply + numbers))
