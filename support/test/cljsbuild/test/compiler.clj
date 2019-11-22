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

(def output-to "output-to")
(def output-to-2 "output-to-2")
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
(def mtime-2 5678)

(fact "run-compiler calls cljs/build correctly"
  (run-compiler
    cljs-paths
    checkout-paths
    compiler-options
    notify-command
    incremental?
    assert?
    {}
    false) => (just {cljs-file-a mtime
                     cljs-file-b mtime
                     cljs-checkout-file-a mtime
                     cljs-checkout-file-b mtime})
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
    (util/sh anything) => nil :times 1
    (fs/mod-time cljs-file-a) => mtime :times 1
    (fs/mod-time cljs-file-b) => mtime :times 1
    (fs/mod-time cljs-checkout-file-a) => mtime :times 1
    (fs/mod-time cljs-checkout-file-b) => mtime :times 1
    (fs/mkdirs anything) => nil
    ; bapi/inputs returns different instance each time and it doesn't provide equals method
    (bapi/build
      (as-checker #(and (instance? cljs.closure.Compilable %) (instance? cljs.closure.Inputs %)))
      compiler-options-with-defaults)
    => nil :times 1))

(fact "returns oldest modified time"
      (get-oldest-mtime [cljs-file-a cljs-file-b]) => mtime
      (provided
       (fs/exists? cljs-file-a) => true
       (fs/mod-time cljs-file-a) => mtime
       (fs/exists? cljs-file-b) => true
       (fs/mod-time cljs-file-b) => mtime-2))

(fact "should get output file provided through output-to option"
      (get-output-files {:output-to output-to}) => [output-to])

(fact "should get output files provided through modules option"
      (get-output-files {:modules {:front {:output-to output-to
                                           :entries #{"hb.front.core"}}
                                   :extranet {:output-to output-to-2
                                              :entries #{"hb.core"}}}}) => [output-to output-to-2])

(fact "should return no output files when output-to and modules options not provided"
      (get-output-files {}) => [])
