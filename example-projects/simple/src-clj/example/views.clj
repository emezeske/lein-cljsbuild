(ns example.views
  (:require
    [hiccup
      [page :refer [html5]]
      [page :refer [include-js]]]))

(defn index-page []
  (html5
    [:head
      [:title "Hello World"]
      (include-js "/js/main.js")]
    [:body
      [:h1 "Hello World"]]))
