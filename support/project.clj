(defproject cljsbuild "2.0.0-SNAPSHOT"
  :description "ClojureScript Autobuilder"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license
    {:name "Eclipse Public License - v 1.0"
     :url "http://www.eclipse.org/legal/epl-v10.html"
     :distribution :repo}
  :dependencies
    [[org.clojure/clojure "1.5.1"]
     [org.clojure/clojurescript "0.0-3211"
       :exclusions [org.apache.ant/ant]]
     [fs "1.1.2"]
     [clj-stacktrace "0.2.5"]
     [org.clojure/tools.namespace "0.2.11"]]
  :aot [cljsbuild.test]
  :profiles {
    :dev {
      :dependencies [[midje "1.9.9"]]
      :plugins [[lein-midje "3.2.1"]]}})
