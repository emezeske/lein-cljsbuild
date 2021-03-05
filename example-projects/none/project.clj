(defproject none "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.597"]]

  :plugins [[lein-cljsbuild "2.0.0-SNAPSHOT"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "none"
              :source-paths ["src"]
              :compiler {
                ;; Outputs main file as none.js in current directory
                ;; This file mainly consists of code loading other files
                :output-to "none.js"
                ;; Where all the other files are stored. This folder must
                ;; be accessible from your web page, as it will be loaded
                ;; from JavaScript
                :output-dir "out"
                ;; The :none option is much faster than the other ones,
                ;; and is the only one to provide correct source-maps.
                :optimizations :none
                ;; source-maps are used by the browser to show the
                ;; ClojureScript code in the debugger
                :source-map true}}]})
