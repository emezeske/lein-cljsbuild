(defproject cljsbuild "0.3.0"
  :description "ClojureScript Autobuilder"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license
    {:name "Eclipse Public License - v 1.0"
     :url "http://www.eclipse.org/legal/epl-v10.html"
     :distribution :repo}
  :dependencies
    [[org.clojure/clojure "1.4.0"]
     [org.clojure/clojurescript "0.0-1552"
       :exclusions [org.apache.ant/ant]]
     ; Ugly workaround for http://dev.clojure.org/jira/browse/CLJS-418
     [org.clojure/google-closure-library-third-party "0.0-2029"]
     [fs "1.1.2"]
     [clj-stacktrace "0.2.5"]]
  :profiles {
    :dev {
      :dependencies [[midje "1.4.0"]]
      :plugins [[lein-midje "2.0.4"]]}})
