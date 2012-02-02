(defproject lein-cljsbuild "0.0.10"
  :description "ClojureScript Autobuilder Plugin"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  ; TODO: These dependencies are also maintained in leiningen.cljsbuild.
  ;       Figure out some way to DRY them.
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/clojurescript "0.0-927"]
                 [fs "1.1.2"]
                 [clj-stacktrace "0.2.4"]]
  :eval-in-leiningen true)
