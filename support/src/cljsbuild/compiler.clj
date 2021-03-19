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

(defn- notify-cljs [{:keys [command message color]}]
  (when (seq (:shell command))
    (try
      (util/sh (update-in command [:shell] (fn [old] (concat old [message]))))
      (catch Throwable e
        (println (red "Error running :notify-command:"))
        (pst+ e))))
  (println (color message)))

(defn get-output-files [compiler-options]
  (if-let [output-file (:output-to compiler-options)]
    [output-file]
    (into [] (map :output-to (->> compiler-options :modules vals)))))

(defn- compile-cljs [{:keys [cljs-paths compiler-options notify-command
                             incremental? assert? watching?]}]
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
        (notify-cljs {:command notify-command
                      :message (str "Successfully compiled " (pr-str output-files) " in " (elapsed started-at) ".")
                      :color green})
        (catch Throwable e
          (notify-cljs {:command notify-command
                        :message (str "Compiling " (pr-str output-files) " failed.")
                        :color red})
          (if watching?
            (pst+ e)
            (throw e)))))))

(defn- get-modified-times
  "For given paths, returns a mapping from path to last modified time."
  [paths]
  (into {}
    (map (fn [path] [path (fs/mod-time path)]) paths)))

(defn- get-modified-files
  "Given the last compile time, returns all files that have been modified since."
  [output-modified-time dependency-modified-times]
  (reduce (fn [modified [path mtime]]
            (if (< output-modified-time mtime)
              (conj modified path)
              modified))
          []
          dependency-modified-times))

(defn get-oldest-modified-time [output-files]
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

(defn touch-files
  "Touch all cljs target files so that the CLJS build API will rebuild them. This
  can be necessary when a macro (in a clj file) is being changed, but not cljs files,
  for example."
  [cljs-files compiler-options]
  (doseq [cljs-file cljs-files]
    (let [target-file (bapi/src-file->target-file (io/file cljs-file) compiler-options)]
      (when (.exists target-file)
        (.setLastModified target-file 5000)))))

(defn reload-clojure
  "Reload clojure files that have changed."
  [clj-files notify-command]
  (alter-var-root #'refresh-tracker #(dir/scan-dirs % clj-files))
  (alter-var-root #'refresh-tracker reload/track-reload)
  (when-let [e (::reload/error refresh-tracker)]
    (notify-cljs {:command notify-command
                  :message (str "Reloading Clojure file \"" (::reload/error-ns refresh-tracker) "\" failed.")
                  :color red})
    (pst+ e)))

(defn run-compiler
  "Produces runnable JavaScript using the CLJS build API. Only triggers a build,
  when a file has been modified since the last build.
  Returns a mapping from file to last modified time."
  [{:keys [cljs-paths checkout-paths compiler-options notify-command incremental?
           assert? last-modified-times watching? project-root]}]
  (let [compiler-options (merge {:output-wrapper (= :advanced (:optimizations compiler-options))}
                                compiler-options)
        output-files (get-output-files compiler-options)
        lib-paths (:libs compiler-options)
        output-modified-time (get-oldest-modified-time output-files)
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
        clj-modified-times (get-modified-times clj-files)
        cljs-modified-times (get-modified-times cljs-files)
        js-modified-times (get-modified-times js-files)
        modified-times (merge clj-modified-times cljs-modified-times js-modified-times)]
    (when (not= last-modified-times modified-times)
      (let [clj-modified (get-modified-files output-modified-time clj-modified-times)
            cljs-modified (get-modified-files output-modified-time cljs-modified-times)
            js-modified (get-modified-files output-modified-time js-modified-times)
            relative-checkout-paths (mapv (partial util/relative-path project-root) checkout-paths)]
        (when (seq clj-modified)
          (touch-files cljs-files compiler-options)
          (reload-clojure clj-files notify-command))
        (when (or (seq clj-modified) (seq cljs-modified) (seq js-modified))
          (compile-cljs {:cljs-paths (into cljs-paths relative-checkout-paths)
                         :compiler-options compiler-options
                         :notify-command notify-command
                         :incremental? incremental?
                         :assert? assert?
                         :watching? watching?}))))
    modified-times))
