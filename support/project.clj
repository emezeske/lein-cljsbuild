(defproject cljsbuild "1.0.5"
  :description "ClojureScript Autobuilder"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license
    {:name "Eclipse Public License - v 1.0"
     :url "http://www.eclipse.org/legal/epl-v10.html"
     :distribution :repo}
  :dependencies
    [[org.clojure/clojure "1.6.0"]
     [org.clojure/clojurescript "0.0-2985"
       :exclusions [org.apache.ant/ant]]
     [fs "1.1.2"]
     [clj-stacktrace "0.2.5"]]
  :aot [cljsbuild.test]
  :profiles {
    :dev {
      :dependencies [[midje "1.6.3"]]
      :plugins [[lein-midje "3.1.3"]]}})
