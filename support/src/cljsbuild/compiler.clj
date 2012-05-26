(ns cljsbuild.compiler
  (:use
    [clj-stacktrace.repl :only [pst+]]
    [cljs.closure :only [build]])
  (:require
    [cljsbuild.util :as util]
    [cljs.compiler :as compiler]
    [fs.core :as fs]))

(defonce lock (Object.))

(defn- apply-safe [f args]
  (locking lock
    (apply f args)
    (flush)))

(defn- println-safe [& args]
  (apply-safe println args))

(def reset-color "\u001b[0m")
(def foreground-red "\u001b[31m")
(def foreground-green "\u001b[32m")

(defn- colorizer [c]
  (fn [& args]
    (str c (apply str args) reset-color)))

(def red (colorizer foreground-red))
(def green (colorizer foreground-green))

(defn- elapsed [started-at]
  (let [elapsed-us (- (. System (nanoTime)) started-at)]
    (with-precision 2
      (str (/ (double elapsed-us) 1000000000) " seconds"))))

(defn- notify-cljs [command message colorizer]
  (when (seq (:shell command))
    (try
      (util/sh (update-in command [:shell] (fn [old] (concat old [message]))))
      (catch Throwable e
        (println-safe (red "Error running :notify-command:"))
        (pst+ e))))
  (println-safe (colorizer message)))

(defn- compile-cljs [cljs-path compiler-options notify-command
                     warn-on-undeclared? incremental?]
  (let [output-file (:output-to compiler-options)
        output-file-dir (fs/parent output-file)]
    (println-safe (str "Compiling \"" output-file "\" from \"" cljs-path "\"..."))
    (flush)
    (when (not incremental?)
      (fs/delete-dir (:output-dir compiler-options)))
    (when output-file-dir
      (fs/mkdirs output-file-dir))
    (let [started-at (System/nanoTime)]
      (try
        (binding [compiler/*cljs-warn-on-undeclared* warn-on-undeclared?]
          (build cljs-path compiler-options))
        (notify-cljs
          notify-command
          (str "Successfully compiled \"" output-file "\" in " (elapsed started-at) ".") green)
        (catch Throwable e
          (notify-cljs
            notify-command
            (str "Compiling \"" output-file "\" failed:") red)
          (pst+ e))))))

(defn run-compiler [cljs-path crossover-path compiler-options notify-command
                    warn-on-undeclared? incremental? watch?]
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
                      warn-on-undeclared? incremental?))
      (when watch?
        (Thread/sleep 100)
        (recur dependency-mtimes)))))
