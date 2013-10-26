(ns cljsbuild.compiler
  (:use
    [clojure.pprint]
    [clj-stacktrace.repl :only [pst+]]
    [cljs.closure :only [build]])
  (:require
    [cljsbuild.util :as util]
    [cljs.analyzer :as analyzer]
    [cljs.closure :as closure]
    [clojure.string :as string]
    [clojure.java.io :as io]
    [fs.core :as fs]))

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
        (println (red "Error running :notify-command:"))
        (pst+ e))))
  (println (colorizer message)))

;; Cannnot call build with ["src/cljs" "src/cljs-more"] cause build thinks a vector
;; denotes a cljs-form, so invent a new Compilable type cause thats what its expects
(defrecord SourcePaths [paths]
  cljs.closure/Compilable
  (-compile [_ opts]
    (mapcat #(closure/-compile % opts) paths)))

(defn- compile-cljs [cljs-paths compiler-options notify-command incremental? assert? watching?]
  (let [output-file (:output-to compiler-options)
        output-file-dir (fs/parent output-file)]
    (println (str "Compiling \"" output-file "\" from " (pr-str cljs-paths) "..."))
    (flush)
    (when (not incremental?)
      (fs/delete-dir (:output-dir compiler-options)))
    (when output-file-dir
      (fs/mkdirs output-file-dir))
    (let [started-at (System/nanoTime)]
      (try
        (binding [*assert* assert?]
          (build (SourcePaths. cljs-paths) compiler-options))
        (notify-cljs
          notify-command
          (str "Successfully compiled \"" output-file "\" in " (elapsed started-at) ".") green)
        (catch Throwable e
          (notify-cljs
            notify-command
            (str "Compiling \"" output-file "\" failed.") red)
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

(defn- drop-extension [path]
  (let [i (.lastIndexOf path ".")]
    (if (pos? i)
      (subs path 0 i)
      path)))

(defn- relativize [parent path]
  (let [path (.getCanonicalPath (fs/file path))
        parent (.getCanonicalPath (fs/file parent))]
    (if (.startsWith path parent)
      (subs path (count parent))
      path)))

(defn reload-clojure [paths compiler-options notify-command]
  ; Incremental builds will use cached JS output unless one of the cljs input files
  ; has been modified.  Since reloading a clj file *might* affect the build, but does
  ; not affect any cljs file mtimes, we have to clear the cache here to force everything
  ; to be rebuilt.
  (fs/delete-dir (:output-dir compiler-options))
  (doseq [path paths
          ; only Clojure files that define macros can possibly affect
          ; ClojureScript compilation; those that don't might define the "same"
          ; namespace as a same-named ClojureScript file, and thus interfere
          ; with cljsc's usage of Clojure namespaces during compilation. gh-210
          :when (-> path (string/replace #"^/" "") io/resource
                    ^String slurp (.contains "defmacro"))]
    (try
      (load (drop-extension path))
      (catch Throwable e
        (notify-cljs
          notify-command
          (str "Reloading Clojure file \"" path "\" failed.") red)
        (pst+ e)))))

(defn run-compiler [cljs-paths crossover-path crossover-macro-paths
                    compiler-options notify-command incremental?
                    assert? last-dependency-mtimes watching?]
  (let [compiler-options (merge {:output-wrapper (= :advanced (:optimizations compiler-options))}
                                compiler-options)
        output-file (:output-to compiler-options)
        lib-paths (:libs compiler-options)
        output-mtime (if (fs/exists? output-file) (fs/mod-time output-file) 0)
        macro-files (map :absolute crossover-macro-paths)
        macro-classpath-files (into {} (map vector macro-files (map :classpath crossover-macro-paths)))
        clj-files-in-cljs-paths
          (into {}
            (for [cljs-path cljs-paths]
              [cljs-path (util/find-files cljs-path #{"clj"})]))
        cljs-files (mapcat #(util/find-files % #{"cljs"}) (conj cljs-paths crossover-path))
        js-files (->> lib-paths
                      (mapcat #(util/find-files % #{"js"}))
                      ; Don't include js files in output-dir or our output file itself,
                      ; both possible if :libs is set to [""] (a cljs compiler workaround to
                      ; load all libraries without enumerating them, see
                      ; http://dev.clojure.org/jira/browse/CLJS-526)
                      (remove #(.startsWith ^String % (:output-dir compiler-options)))
                      (remove #(.endsWith ^String % (:output-to compiler-options))))
        macro-mtimes (get-mtimes macro-files)
        clj-mtimes (get-mtimes (mapcat second clj-files-in-cljs-paths))
        cljs-mtimes (get-mtimes cljs-files)
        js-mtimes (get-mtimes js-files)
        dependency-mtimes (merge macro-mtimes clj-mtimes cljs-mtimes js-mtimes)]
    (when (not= last-dependency-mtimes dependency-mtimes)
      (let [macro-modified (list-modified output-mtime macro-mtimes)
            clj-modified (list-modified output-mtime clj-mtimes)
            cljs-modified (list-modified output-mtime cljs-mtimes)
            js-modified (list-modified output-mtime js-mtimes)]
        (when (seq macro-modified)
          (reload-clojure (map macro-classpath-files macro-modified) compiler-options notify-command))
        (when (seq clj-modified)
          (reload-clojure
            (apply concat
              (for [[cljs-path clj-files] clj-files-in-cljs-paths]
                (map (partial relativize cljs-path) clj-files)))
            compiler-options notify-command))
        (when (or (seq macro-modified) (seq clj-modified) (seq cljs-modified) (seq js-modified))
          (compile-cljs cljs-paths compiler-options notify-command incremental? assert? watching?))))
    dependency-mtimes))
