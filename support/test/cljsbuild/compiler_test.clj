(ns cljsbuild.compiler-test
  (:require [cljsbuild.compiler :as compiler]
            [cljsbuild.util :as util]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [cljs.build.api :as bapi]
            [me.raynes.fs :as fs]))

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
(def compiler-options {:output-to output-to
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

(deftest run-compiler
  (with-redefs [fs/exists? (fn [_] false)
                util/find-files (fn [path ext]
                                  (condp = [path ext]
                                    [cljs-path-a #{"cljc" "clj"}] []
                                    [cljs-path-b #{"cljc" "clj"}] []
                                    [cljs-path-a #{"cljc" "cljs"}] [cljs-file-a]
                                    [cljs-path-b #{"cljc" "cljs"}] [cljs-file-b]
                                    [cljs-checkout-path-a #{"cljc" "clj"}] []
                                    [cljs-checkout-path-b #{"cljc" "clj"}] []
                                    [cljs-checkout-path-a #{"cljc" "cljs"}] [cljs-checkout-file-a]
                                    [cljs-checkout-path-b #{"cljc" "cljs"}] [cljs-checkout-file-b]))
                util/sh (fn [_] nil)
                fs/mod-time (fn [_] mtime)
                fs/mkdirs (fn [_] nil)
                bapi/build (fn [_ _] nil)]
    (is (= {cljs-file-a mtime
            cljs-file-b mtime
            cljs-checkout-file-a mtime
            cljs-checkout-file-b mtime}
           (compiler/run-compiler {:cljs-paths cljs-paths
                                   :checkout-paths checkout-paths
                                   :compiler-options compiler-options
                                   :notify-command notify-command
                                   :incremental? incremental?
                                   :assert? assert?
                                   :last-modified-times {}
                                   :watching? false
                                   :project-root ""})))))

(deftest get-oldest-modified-time
  (with-redefs [fs/exists? (constantly true)
                fs/mod-time (fn [file]
                              (condp = file
                                cljs-file-a mtime
                                cljs-file-b mtime-2))]
    (is (= mtime (compiler/get-oldest-modified-time [cljs-file-a cljs-file-b])))))

(deftest get-output-files
  (testing "should get output file provided through output-to option"
    (is (= [output-to] (compiler/get-output-files {:output-to output-to}))))
  (testing "should get output files provided through modules option"
    (is (= [output-to output-to-2] (compiler/get-output-files {:modules {:front {:output-to output-to
                                                                                 :entries #{"hb.front.core"}}
                                                                         :extranet {:output-to output-to-2
                                                                                    :entries #{"hb.core"}}}}))))
  (testing "should return no output files when output-to and modules options not provided"
    (is (= [] (compiler/get-output-files {})))))
