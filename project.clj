(defproject emezeske/lein-cljsbuild "0.0.2"
  :description "ClojureScript Autobuilder Plugin"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [fs "0.11.1"]
                 [emezeske/clojurescript "0.0.1-329708bdd0"]
                 [clj-stacktrace "0.2.3"]] 
  :eval-in-leiningen true)
