(ns cljsbuild.test
  (:require
    [clojure.java.io :as io])
  (:import
    (java.io OutputStreamWriter)))

(defn- pump [reader out]
  (let [buffer (make-array Character/TYPE 1000)]
    (loop [len (.read reader buffer)]
      (when-not (neg? len)
        (.write out buffer 0 len)
        (.flush out)
        (Thread/sleep 100)
        (recur (.read reader buffer))))))

(defn sh [command]
  (let [process (.exec (Runtime/getRuntime) (into-array command))]
    (with-open [out (io/reader (.getInputStream process))
                err (io/reader (.getErrorStream process))]
      (let [pump-out (doto (Thread. #(pump out *out*)) .start)
            pump-err (doto (Thread. #(pump err *err*)) .start)]
        (.join pump-out)
        (.join pump-err))
      (.waitFor process)
      (.exitValue process))))

(defmacro dofor [seq-exprs body-expr]
  `(doall (for ~seq-exprs ~body-expr)))

(defn run-tests [test-commands]
  (let [success (every? #(= % 0)
                  (dofor [test-command test-commands]
                    (sh test-command)))]
    (when (not success)
      (throw (Exception. "Test failed.")))))
