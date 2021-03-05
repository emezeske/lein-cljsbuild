(defproject cljsbuild "2.0.0-SNAPSHOT"
  :description "ClojureScript Autobuilder"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license
    {:name "Eclipse Public License - v 1.0"
     :url "http://www.eclipse.org/legal/epl-v10.html"
     :distribution :repo}
  :dependencies
    [[org.clojure/clojure "1.10.1"]
     [org.clojure/clojurescript "1.10.597"
       :exclusions [org.apache.ant/ant]]
     [me.raynes/fs "1.4.6"]
     [clj-stacktrace "0.2.8"]
     [org.clojure/tools.namespace "0.3.1"]]
  :aot [cljsbuild.test])
