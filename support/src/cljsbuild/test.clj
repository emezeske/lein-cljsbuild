(ns cljsbuild.test
  (:require
    [cljsbuild.util :as util]))

(defn sh [command]
  (let [process (util/process-start command)]
    ((:wait process))))

(defmacro dofor [seq-exprs body-expr]
  `(doall (for ~seq-exprs ~body-expr)))

(defn run-tests [test-commands]
  (let [success (every? #(= % 0)
                  (dofor [test-command test-commands]
                    (sh test-command)))]
    (when (not success)
      (throw (Exception. "Test failed.")))))
