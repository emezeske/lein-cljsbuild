(defproject lein-cljsbuild "2.0.0-SNAPSHOT"
  :description "ClojureScript Autobuilder Plugin"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license
  {:name "Eclipse Public License - v 1.0"
   :url "http://www.eclipse.org/legal/epl-v10.html"
   :distribution :repo}
  :dependencies [[me.raynes/fs "1.4.6"]]
  :profiles {:dev {:dependencies [[cljsbuild "2.0.0-SNAPSHOT"]]}}
  :eval-in-leiningen true)
