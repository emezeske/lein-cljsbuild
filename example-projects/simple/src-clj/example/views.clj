(ns example.views
  (:use [hiccup core page-helpers]))

(defn index-page []
  (html5
    [:head
      [:title "Hello World"]
      (include-js "/js/main.js")]
    [:body
      [:h1 "Hello World"]]))
