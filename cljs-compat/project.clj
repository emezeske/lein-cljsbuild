;; this is _meant_ to be a SNAPSHOT forever so that updates can always percolate
;; down to users. Necessary because cljs compiler API is very volatile, and it
;; doesn't use anything like a semver scheme. See
;; https://github.com/emezeske/lein-cljsbuild/issues/266
(defproject lein-cljsbuild/cljs-compat "1.0.0-SNAPSHOT"
  :description "Matrix of cljsbuild/ClojureScript compatibility"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license
    {:name "Eclipse Public License - v 1.0"
     :url "http://www.eclipse.org/legal/epl-v10.html"
     :distribution :repo}
  :dependencies [[org.clojure/clojure "1.5.1"]])
