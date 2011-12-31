(ns example.views
  (:require
    [example.crossover.shared :as shared])
  (:use
    [hiccup core page-helpers]))

(defn index-page []
  (html5
    [:head
      [:title (shared/make-example-text)]
      (include-js "/js/main.js")]
    [:body
      [:h1 (shared/make-example-text)]]))
