(ns cljsbuild.compiler
  (:use
    [clj-stacktrace.repl :only [pst+]]
    [cljs.closure :only [build]])
  (:require
    [cljsbuild.util :as util]
    [cljs.compiler :as compiler]
    [fs.core :as fs]))

(def lock (Object.))

(defn- apply-safe [f & args]
  (locking lock
    (apply f args)
    (flush)))

(defn- println-safe [& args]
  (apply-safe println args))

(defn- elapsed [started-at]
  (let [elapsed-us (- (. System (nanoTime)) started-at)]
    (with-precision 2
      (str (/ (double elapsed-us) 1000000000) " seconds"))))

(defn- notify-cljs [command message]
  (when (:bell command)
    (apply-safe print \u0007))
  (when (seq (:shell command))
    (try
      (util/sh (assoc command :shell (map #(if (= % "%") message %) (:shell command))))
      (catch Throwable e
        (println-safe "Error running :notify-command:")
        (pst+ e))))
  (println-safe message))

(defn- compile-cljs [cljs-path compiler-options notify-command
                     warn-on-undeclared]
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
        (binding [compiler/*cljs-warn-on-undeclared* warn-on-undeclared]
          (build cljs-path compiler-options))
        (notify-cljs notify-command (str output-file " compiled in " (elapsed started-at) "."))
        (catch Throwable e
          (notify-cljs notify-command " Failed!")
          (pst+ e))))))

(defn run-compiler [cljs-path crossover-path compiler-options notify-command
                    warn-on-undeclared watch?]
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
        (compile-cljs cljs-path compiler-options notify-command
                      warn-on-undeclared))
      (when watch?
        (Thread/sleep 100)
        (recur dependency-mtimes)))))
