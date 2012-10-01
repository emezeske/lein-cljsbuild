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
(def crossover-macro-absolute "/a/b/crossovers/macros.clj")
(def crossover-macro-classpath "crossovers/macros.clj")
(def crossover-macro-paths [{:absolute crossover-macro-absolute
                             :classpath crossover-macro-classpath}])
(def output-to "output-to")
(def compiler-options
  {:output-to output-to
   :output-dir "output-dir"
   :optimizations :advanced
   :pretty-print false})
(def notify-command {:shell ["a" "b"] :test "c"})
(def incremental? true)
(def mtime 1234)

(fact "run-compiler calls cljs/build correctly"
  (run-compiler
    cljs-path
    crossover-path
    crossover-macro-paths
    compiler-options
    notify-command
    incremental?
    {}) => (just {"src-cljs/a.cljs" mtime,
                  "crossovers/b.cljs" mtime,
                  crossover-macro-absolute mtime})
  (provided
    (fs/exists? output-to) => false :times 1
    (util/find-files cljs-path #{"clj"}) => [] :times 1
    (util/find-files cljs-path #{"cljs"}) => ["src-cljs/a.cljs"] :times 1
    (util/find-files crossover-path #{"cljs"}) => ["crossovers/b.cljs"] :times 1
    (util/sh anything) => nil :times 1
    (fs/mod-time "src-cljs/a.cljs") => mtime :times 1
    (fs/mod-time "crossovers/b.cljs") => mtime :times 1
    (fs/mod-time crossover-macro-absolute) => mtime :times 1
    (fs/mkdirs anything) => nil
    (reload-clojure [crossover-macro-classpath] compiler-options notify-command) => nil :times 1
    (cljs/build cljs-path compiler-options) => nil :times 1))
