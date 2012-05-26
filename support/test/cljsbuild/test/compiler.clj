(ns cljsbuild.test.compiler
  (:use
    cljsbuild.compiler
    midje.sweet)
  (:require
    [cljs.closure :as cljs]
    [cljsbuild.util :as util]
    [clojure.java.io :as io]
    [fs.core :as fs]))

(def cljs-path "src-cljs")
(def crossover-path "crossovers")
(def output-to "output-to")
(def compiler-options
  {:output-to output-to
   :output-dir "output-dir"
   :optimizations :advanced
   :pretty-print false})
(def notify-command nil)
(def warn-on-undeclared? true)
(def incremental? true)
(def watch? false)

(fact "run-compiler calls cljs/build correctly"
  (run-compiler
    cljs-path
    crossover-path
    compiler-options
    notify-command
    warn-on-undeclared?
    incremental?
    watch?) => nil
  (provided
    (fs/exists? output-to) => false :times 1
    (util/find-files cljs-path #{"cljs"}) => ["src-cljs/a.cljs"] :times 1
    (util/find-files crossover-path #{"cljs"}) => ["crossovers/b.cljs"] :times 1
    (fs/mod-time "src-cljs/a.cljs") => 1000 :times 1
    (fs/mkdirs anything) => nil
    (cljs/build cljs-path compiler-options) => nil :times 1))
