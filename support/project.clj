(defproject cljsbuild "0.3.0"
  :description "ClojureScript Autobuilder"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license
    {:name "Eclipse Public License - v 1.0"
     :url "http://www.eclipse.org/legal/epl-v10.html"
     :distribution :repo}
  :min-lein-version "2.0.0"
  :dependencies
    [[org.clojure/clojure "1.4.0"]
     [org.clojure/clojurescript "0.0-1552"
       :exclusions [org.apache.ant/ant]]
     ; Ugly workaround for http://dev.clojure.org/jira/browse/CLJS-418
     [org.clojure/google-closure-library-third-party "0.0-2029"]
     [fs "1.1.2"]
     [clj-stacktrace "0.2.5"]
     [org.clojure/tools.nrepl "0.2.1"]
     [com.cemerick/piggieback "0.0.2"]]
  :repl-options {
    :port 64042
    :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
  :profiles {
    :dev {
      :dependencies [[midje "1.4.0"]]
      :plugins [[lein-midje "2.0.4"]]}})
