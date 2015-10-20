(ns cljsbuild.test.compiler
  (:use
    cljsbuild.compiler
    midje.sweet)
  (:require
    [cljsbuild.util :as util]
    [clojure.java.io :as io]
    [cljs.build.api :as bapi]
    [fs.core :as fs]))

(def cljs-path-a "src-cljs-a")
(def cljs-file-a (str cljs-path-a "/file-a.cljs"))
(def cljs-path-b "src-cljs-b")
(def cljs-file-b (str cljs-path-b "/file-b.cljs"))
(def cljs-paths [cljs-path-a cljs-path-b])

(def cljs-checkout-path-a "checkouts/dep-a/cljs-src")
(def cljs-checkout-file-a "checkouts/dep-a/cljs-src/file-a.cljs")
(def cljs-checkout-path-b "checkouts/dep-b/cljs-src")
(def cljs-checkout-file-b "checkouts/dep-b/cljs-src/file-b.cljs")
(def checkout-paths [cljs-checkout-path-a cljs-checkout-path-b])

(def crossover-path "crossovers")
(def crossover-file (str crossover-path "/file-c.cljs"))
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
(def compiler-options-with-defaults
  (assoc compiler-options :output-wrapper true))
(def notify-command {:shell ["a" "b"] :test "c"})
(def assert? false)
(def incremental? true)
(def mtime 1234)

; TODO: We really need more tests here, particularly for the crossover/clojure reloading stuff.

(fact "run-compiler calls cljs/build correctly"
  (run-compiler
    cljs-paths
    checkout-paths
    crossover-path
    crossover-macro-paths
    compiler-options
    notify-command
    incremental?
    assert?
    {}
    false) => (just {cljs-file-a mtime
                  cljs-file-b mtime
                  cljs-checkout-file-a mtime
                  cljs-checkout-file-b mtime
                  crossover-file mtime
                  crossover-macro-absolute mtime})
  (provided
    (fs/exists? output-to) => false :times 1
    (util/find-files cljs-path-a #{"clj"}) => [] :times 1
    (util/find-files cljs-path-b #{"clj"}) => [] :times 1
    (util/find-files cljs-path-a #{"cljs"}) => [cljs-file-a] :times 1
    (util/find-files cljs-path-b #{"cljs"}) => [cljs-file-b] :times 1
    (util/find-files cljs-checkout-path-a #{"clj"}) => [] :times 1
    (util/find-files cljs-checkout-path-b #{"clj"}) => [] :times 1
    (util/find-files cljs-checkout-path-a #{"cljs"}) => [cljs-checkout-file-a] :times 1
    (util/find-files cljs-checkout-path-b #{"cljs"}) => [cljs-checkout-file-b] :times 1
    (util/find-files crossover-path #{"cljs"}) => [crossover-file] :times 1
    (util/sh anything) => nil :times 1
    (fs/mod-time cljs-file-a) => mtime :times 1
    (fs/mod-time cljs-file-b) => mtime :times 1
    (fs/mod-time cljs-checkout-file-a) => mtime :times 1
    (fs/mod-time cljs-checkout-file-b) => mtime :times 1
    (fs/mod-time crossover-file) => mtime :times 1
    (fs/mod-time crossover-macro-absolute) => mtime :times 1
    (fs/mkdirs anything) => nil
    (reload-clojure [cljs-file-a
                     cljs-file-b
                     cljs-checkout-file-a
                     cljs-checkout-file-b
                     crossover-file]
                    [crossover-macro-classpath] compiler-options-with-defaults notify-command) => nil :times 1
    ; bapi/inputs returns different instance each time and it doesn't provide equals method
    (bapi/build
      (as-checker #(and (instance? cljs.closure.Compilable %) (instance? cljs.closure.Inputs %)))
      compiler-options-with-defaults)
    => nil :times 1))
