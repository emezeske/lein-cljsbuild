(ns cljsbuild.compiler
  (:use [clojure.pprint]
        [clj-stacktrace.repl :only [pst+]])
  (:require [cljsbuild.util :as util]
            [cljs.build.api :as bapi]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]
            [clojure.tools.namespace
             [track :as track]
             [dir :as dir]
             [reload :as reload]]))

(defonce refresh-tracker (track/tracker))

(def reset-color "\u001b[0m")
(def foreground-red "\u001b[31m")
(def foreground-green "\u001b[32m")

(defn- colorizer [c]
  (fn [& args]
    (str c (apply str args) reset-color)))

(def red (colorizer foreground-red))
(def green (colorizer foreground-green))

(defn- elapsed [started-at]
  (let [elapsed-us (- (System/currentTimeMillis) started-at)]
    (with-precision 2
      (str (/ (double elapsed-us) 1000) " seconds"))))

(defn- notify-cljs [command message colorizer]
  (when (seq (:shell command))
    (try
      (util/sh (update-in command [:shell] (fn [old] (concat old [message]))))
      (catch Throwable e
        (println (red "Error running :notify-command:"))
        (pst+ e))))
  (println (colorizer message)))

(defn get-output-files [compiler-options]
  (if-let [output-file (:output-to compiler-options)]
    [output-file]
    (into [] (map :output-to (->> compiler-options :modules vals)))))

(defn- compile-cljs [cljs-paths compiler-options notify-command incremental? assert? watching?]
  (let [output-files (get-output-files compiler-options)
        output-files-parent (map fs/parent output-files)]
    (println (str "Compiling " (pr-str output-files) " from " (pr-str cljs-paths) "..."))
    (flush)
    (when (not incremental?)
      (fs/delete-dir (:output-dir compiler-options)))
    (doseq [output-file-parent output-files-parent]
      (when output-file-parent
        (fs/mkdirs output-file-parent)))
    (let [started-at (System/currentTimeMillis)]
      (try
        (binding [*assert* assert?]
          (bapi/build (apply bapi/inputs cljs-paths) compiler-options))
        (doseq [output-file output-files]
          (fs/touch output-file started-at))
        (notify-cljs
          notify-command
          (str "Successfully compiled " (pr-str output-files) " in " (elapsed started-at) ".") green)
        (catch Throwable e
          (notify-cljs
            notify-command
            (str "Compiling " (pr-str output-files) " failed.") red)
          (if watching?
            (pst+ e)
            (throw e)))))))

(defn- get-mtimes [paths]
  (into {}
    (map (fn [path] [path (fs/mod-time path)]) paths)))

(defn- list-modified [output-mtime dependency-mtimes]
  (reduce (fn [modified [path mtime]]
            (if (< output-mtime mtime)
              (conj modified path)
              modified))
          []
          dependency-mtimes))

(defn get-oldest-mtime [output-files]
  (apply min (map (fn [output-file]
                    (if (fs/exists? output-file)
                      (fs/mod-time output-file)
                      0))
                  output-files)))

(defn- drop-extension [path]
  (let [i (.lastIndexOf path ".")]
    (if (pos? i)
      (subs path 0 i)
      path)))

(def additional-file-extensions
  (try
    (apply #'read-string [{:read-cond :allow} "#?(:clj 5 :default nil)"])
    #{"cljc"}
    (catch Throwable t
      #{})))

(defn reload-clojure [cljs-files paths compiler-options notify-command]
  ;; touch all cljs target files so that cljsc/build will rebuild them
  (doseq [cljs-file cljs-files]
    (let [target-file (bapi/src-file->target-file (io/file cljs-file) compiler-options)]
      (if (.exists target-file)
        (.setLastModified target-file 5000))))

  ;; reload Clojure files
  (alter-var-root #'refresh-tracker #(apply dir/scan % paths))
  (alter-var-root #'refresh-tracker reload/track-reload)
  (when-let [e (::reload/error refresh-tracker)]
    (notify-cljs notify-command
                 (str "Reloading Clojure file \"" (::reload/error-ns refresh-tracker) "\" failed.") red)
    (pst+ e)))

(defn run-compiler [cljs-paths checkout-paths compiler-options notify-command incremental?
                    assert? last-dependency-mtimes watching? root]
  (let [compiler-options (merge {:output-wrapper (= :advanced (:optimizations compiler-options))}
                                compiler-options)
        output-files (get-output-files compiler-options)
        lib-paths (:libs compiler-options)
        output-mtime (get-oldest-mtime output-files)
        clj-files (mapcat (fn [cljs-path]
                            (util/find-files cljs-path (conj additional-file-extensions "clj")))
                          (concat cljs-paths checkout-paths))
        cljs-files (->> (concat cljs-paths checkout-paths)
                     (mapcat #(util/find-files % (conj additional-file-extensions "cljs")))
                     (remove #(contains? cljs.compiler/cljs-reserved-file-names (.getName (io/file %)))))
        js-files (let [output-dir-str
                       (.getAbsolutePath (io/file (:output-dir compiler-options)))]
                   (->> lib-paths
                        (mapcat #(util/find-files % #{"js"}))
                      ; Don't include js files in output-dir or our output file itself,
                      ; both possible if :libs is set to [""] (a cljs compiler workaround to
                      ; load all libraries without enumerating them, see
                      ; http://dev.clojure.org/jira/browse/CLJS-526)
                      (remove #(.startsWith ^String % output-dir-str))
                      (remove #(.endsWith ^String % (:output-to compiler-options)))))
        clj-mtimes (get-mtimes clj-files)
        cljs-mtimes (get-mtimes cljs-files)
        js-mtimes (get-mtimes js-files)
        dependency-mtimes (merge clj-mtimes cljs-mtimes js-mtimes)
        relative-checkout-paths (mapv (partial util/relative-path root) checkout-paths)]
    (when (not= last-dependency-mtimes dependency-mtimes)
      (let [clj-modified (list-modified output-mtime clj-mtimes)
            cljs-modified (list-modified output-mtime cljs-mtimes)
            js-modified (list-modified output-mtime js-mtimes)]
        (when (seq clj-modified)
          (reload-clojure cljs-files clj-files compiler-options notify-command))
        (when (or (seq clj-modified) (seq cljs-modified) (seq js-modified))
          (compile-cljs (into cljs-paths relative-checkout-paths) compiler-options notify-command incremental? assert? watching?))))
    dependency-mtimes))
