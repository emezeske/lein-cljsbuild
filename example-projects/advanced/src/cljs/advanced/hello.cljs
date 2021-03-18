(ns advanced.hello
  (:require [advanced.shared :as shared]))

(defn ^:export say-hello []
  (js/alert (shared/make-example-text)))

(defn add-some-numbers [& numbers]
  (apply + numbers))
