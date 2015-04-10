(defproject cljsbuild "1.0.5-LOCAL"
  :description "ClojureScript Autobuilder"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license
    {:name "Eclipse Public License - v 1.0"
     :url "http://www.eclipse.org/legal/epl-v10.html"
     :distribution :repo}
  :dependencies
    [[org.clojure/clojure "1.5.1"]
     [org.clojure/clojurescript "0.0-2411"
       :exclusions [org.apache.ant/ant]]
     [fs "1.3.3"]
     [clj-stacktrace "0.2.5"]]
  :aot [cljsbuild.test]
  :profiles {
    :dev {
      :dependencies [[midje "1.5.1"]]
      :plugins [[lein-midje "2.0.4"]]}})
