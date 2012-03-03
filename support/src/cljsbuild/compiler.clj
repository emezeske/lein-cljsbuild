(ns cljsbuild.compiler
  (:use
    [clj-stacktrace.repl :only [pst+]]
    [cljs.closure :only [build]])
  (:require
    [cljsbuild.util :as util]
    [fs.core :as fs]))

(def lock (Object.))

(defn- print-safe
  [& args]
  (locking lock
    (apply print args)
    (flush)))

(defn- println-safe
  [& args]
  (locking lock
    (apply println args)
    (flush)))

(defn- elapsed [started-at]
  (let [elapsed-us (- (. System (nanoTime)) started-at)]
    (with-precision 2
      (str (/ (double elapsed-us) 1000000000) " seconds"))))

(defn- notify-cljs [cmd msg]
  (try
    (if (:bell cmd)
      (print-safe \u0007)) 
    (if (first (:shell cmd)) 
      (util/sh (assoc cmd :shell (map #(if (= % "%") msg %) (:shell cmd))))) 
    (catch Throwable e
      (pst+ e))) 
  (println-safe msg))

(defn- compile-cljs [cljs-path compiler-options notify-command]
  (let [output-file (:output-to compiler-options)
        output-file-dir (fs/parent output-file)]
    (println-safe (str "Compiling " output-file " from " cljs-path "..."))
    (flush)
    ; FIXME: I do not trust the ClojureScript compiler's cljs/js caching in the
    ;        output-dir.  It seems to forget to rebuild things sometimes, and
    ;        it's a PITA to debug.  This probably slows down compilation, but
    ;        for the moment it is better than the alternative.
    (fs/delete-dir (:output-dir compiler-options))
    (when output-file-dir
      (fs/mkdirs output-file-dir))
    (let [started-at (. System (nanoTime))]
      (try
        (build cljs-path compiler-options)
        (notify-cljs notify-command (str output-file " compiled in " (elapsed started-at) "."))
        (catch Throwable e
          (notify-cljs notify-command " Failed!")
          (pst+ e))))))

(defn run-compiler [cljs-path crossover-path compiler-options notify-command watch?]
  (loop [last-dependency-mtimes {}]
    (let [output-file (:output-to compiler-options)
          output-mtime (if (fs/exists? output-file) (fs/mod-time output-file) 0)
          find-cljs #(util/find-files % #{"cljs"}) 
          dependency-files (mapcat find-cljs [cljs-path crossover-path])
          dependency-mtimes (map fs/mod-time dependency-files)]
      (when
        (and
          (not= last-dependency-mtimes dependency-mtimes)
          (some #(< output-mtime %) dependency-mtimes))
        (compile-cljs cljs-path compiler-options notify-command))
      (when watch?
        (Thread/sleep 100)
        (recur dependency-mtimes)))))
