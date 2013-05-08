(defproject cljsbuild "0.4.0"
  :description "ClojureScript Autobuilder"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license
    {:name "Eclipse Public License - v 1.0"
     :url "http://www.eclipse.org/legal/epl-v10.html"
     :distribution :repo}
  :dependencies
    [[org.clojure/clojure "1.5.1"]
     [org.clojure/clojurescript "0.0-1803"
       :exclusions [org.apache.ant/ant]]
     ; Ugly workaround for http://dev.clojure.org/jira/browse/CLJS-418
     [org.clojure/google-closure-library-third-party "0.0-2029-2"]
     [fs "1.1.2"]
     [clj-stacktrace "0.2.5"]]
  :profiles {
    :dev {
      :dependencies [[midje "1.4.0"]]
      :plugins [[lein-midje "2.0.4"]]}})
