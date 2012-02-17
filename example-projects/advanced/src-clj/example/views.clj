(ns example.views
  (:require
    [example.crossover.shared :as shared])
  (:use
    [hiccup core page-helpers]))

(defn index-page []
  (html5
    [:head
      [:title (shared/make-example-text)] ]
    [:body
      [:h1 (shared/make-example-text)]
      (include-js "/js/main-debug.js")
      (javascript-tag "example.hello.say_hello()")]))

(defn repl-demo-page []
  (html5
    [:head
      [:title "REPL Demo"]]
    [:body
      [:h1 "REPL Demo"]
      [:hr]
      "This page is meant to be accessed by running:"
      [:pre "lein ring server-headless 3000 &
lein trampoline cljsbuild repl-launch firefox http://localhost:3000/repl-demo"]
      [:hr]
      "Alternately, you can run:"
      [:pre "lein ring server-headless 3000 &
lein trampoline cljsbuild repl-listen"]
      "And then browse to this page manually."]
      [:hr]
      [:h2 {:id "fun"} "Try some fun REPL commands!"]
      [:pre "> (js/alert \"Hello!\"
> (load-namespace 'goog.date.Date)
> (js/alert (goog.date.Date.))
> (console.log (reduce + [1 2 3 4 5]))
> (load-namespace 'goog.dom)
> (goog.dom.setTextContent (goog.dom.getElement \"fun\") \"I changed something....\") "]
      (include-js "/js/main-debug.js")
      (javascript-tag "example.repl.connect()")))
