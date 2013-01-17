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
;; denotes a cljs-form, so invent a new Compileable type cause thats what its expects
(defrecord SourcePaths [paths]
  cljs.closure/Compilable
  (-compile [_ opts]
    (mapcat #(closure/-compile % opts) paths)))

(defn- compile-cljs [cljs-paths compiler-options notify-command incremental? assert?]
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
          (pst+ e))))))

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
  (let [path (fs/absolute-path path)
        parent (fs/absolute-path parent)]
    (if (.startsWith path parent)
      (subs path (count parent))
      path)))

(defn reload-clojure [paths compiler-options notify-command]
  ; Incremental builds will use cached JS output unless one of the cljs input files
  ; has been modified.  Since reloading a clj file *might* affect the build, but does
  ; not affect any cljs file mtimes, we have to clear the cache here to force everything
  ; to be rebuilt.
  (fs/delete-dir (:output-dir compiler-options))
  (doseq [path paths]
    (try
      (load (drop-extension path))
      (catch Throwable e
        (notify-cljs
          notify-command
          (str "Reloading Clojure file \"" path "\" failed.") red)
        (pst+ e)))))

(defn get-source-paths [build]
  (if (contains? build :source-paths)
    (:source-paths build)
    [(:source-path build)]))

(defn run-compiler [cljs-paths crossover-path crossover-macro-paths
                    compiler-options notify-command incremental?
                    assert? last-dependency-mtimes]
  (let [output-file (:output-to compiler-options)
        lib-paths (:libs compiler-options)
        output-mtime (if (fs/exists? output-file) (fs/mod-time output-file) 0)
        macro-files (map :absolute crossover-macro-paths)
        macro-classpath-files (into {} (map vector macro-files (map :classpath crossover-macro-paths)))
        clj-files (mapcat #(util/find-files % #{"clj"}) cljs-paths)
        cljs-files (mapcat #(util/find-files % #{"cljs"}) (conj cljs-paths crossover-path))
        js-files (mapcat #(util/find-files % #{"js"}) lib-paths)
        macro-mtimes (get-mtimes macro-files)
        clj-mtimes (get-mtimes clj-files)
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
          ;; FIXME: figure out what this does, since it expects a single cljs-path and not a list of paths?
          (reload-clojure (map (partial relativize (first cljs-paths)) clj-files) compiler-options notify-command))
        (when (or (seq macro-modified) (seq clj-modified) (seq cljs-modified) (seq js-modified))
          (compile-cljs cljs-paths compiler-options notify-command incremental? assert?))))
    dependency-mtimes))
