(ns example.views
  (:require
    [example.crossover.shared :as shared])
  (:use
    [hiccup core page-helpers]))

; When using {:optimizations :whitespace}, the Google Closure compiler combines
; its JavaScript inputs into a single file, which obviates the need for a "deps.js"
; file for dependencies.  However, true to ":whitespace", the compiler does not remove
; the code that tries to fetch the (nonexistent) "deps.js" file.  Thus, we have to turn
; off that feature here by setting CLOSURE_NO_DEPS.
;
; Note that this would not be necessary for :simple or :advanced optimizations.
(defn- run-clojurescript [path init]
  (list
    (javascript-tag "var CLOSURE_NO_DEPS = true;")
    (include-js path)
    (javascript-tag init)))

(defn index-page []
  (html5
    [:head
      [:title (shared/make-example-text)] ]
    [:body
      [:h1 (shared/make-example-text)]
      (run-clojurescript
        "/js/main-debug.js"
        "example.hello.say_hello()")]))

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
      [:pre "> (js/alert \"Hello!\)"
> (load-namespace 'goog.date.Date)
> (js/alert (goog.date.Date.))
> (console.log (reduce + [1 2 3 4 5]))
> (load-namespace 'goog.dom)
> (goog.dom.setTextContent (goog.dom.getElement \"fun\") \"I changed something....\") "]
      (run-clojurescript
        "/js/main-debug.js"
        "example.repl.connect()")))
