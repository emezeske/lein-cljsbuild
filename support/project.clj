(defproject cljsbuild "0.1.3"
  :description "ClojureScript Autobuilder"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/clojurescript "0.0-993"
                   :exclusions [org.apache.ant/ant]]
                 [fs "1.1.2"]
                 [clj-stacktrace "0.2.4"]])
