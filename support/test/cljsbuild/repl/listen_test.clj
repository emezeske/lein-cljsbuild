(ns cljsbuild.repl.listen
  (:require [clojure.test :refer [deftest is testing]]
            [cljsbuild.repl.listen :as listen]
            [cljs.repl :as repl]
            [cljs.repl.browser :as browser]
            [cljsbuild.util :as util]))

(def port (Integer. 1234))
(def output-dir "output-dir")
(def command {:shell ["command"]})

(deftest run-repl-listen-test
  (with-redefs [browser/repl-env (fn [_ _ _ _] {})
                repl/repl (fn [_] nil)
                listen/delayed-process-start (fn [_] (future {:kill (fn [] nil) :wait (fn [] nil)}))]
    (is (nil? (listen/run-repl-listen port output-dir)))
    (is (nil? (run-repl-launch port output-dir command)))))
