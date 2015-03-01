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

(defn ns-from-file [f]
  (try
    (when (.exists f)
      (with-open [rdr (io/reader f)]
        (-> (java.io.PushbackReader. rdr)
            read
            second)))
    ;; better exception here eh?
    (catch java.lang.RuntimeException e
      nil)))

(defn cljs-target-file [opts cljs-file]
  (let [target-dir (cljs.closure/output-directory opts)
        ns-sym     (ns-from-file (io/file cljs-file))
        relative-path (string/split
                       (clojure.lang.Compiler/munge (str ns-sym))
                       #"\.")
        parents       (butlast relative-path)
        path          (apply str (interpose java.io.File/separator
                                            (cons target-dir parents)))]
    (io/file (io/file path) 
             (str (last relative-path) ".js"))))

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
    (let [started-at (System/currentTimeMillis)]
      (try
        (binding [*assert* assert?]
          (build (SourcePaths. cljs-paths) compiler-options))
        (fs/touch output-file started-at)
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

(def additional-file-extensions
  (try
    (apply #'read-string [{:read-cond :allow} "#?(:clj 5 :default nil)"])
    #{"cljc"}
    (catch Throwable t
      #{})))

(defn reload-clojure [cljs-files paths compiler-options notify-command]
  ;; touch all cljs target files so that cljsc/build will rebuild them
  (doseq [cljs-file cljs-files]
    (let [target-file (cljs-target-file compiler-options cljs-file)]
      (if (.exists target-file)
        (.setLastModified target-file 5000))))

  (doseq [path paths
          :when (not= path "/data_readers.clj")]
    (try
      (load (drop-extension path))
      (catch Throwable e
        (notify-cljs
          notify-command
          (str "Reloading Clojure file \"" path "\" failed.") red)
        (pst+ e)))))

(defn run-compiler [cljs-paths checkout-paths crossover-path crossover-macro-paths
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
            (for [cljs-path (concat cljs-paths checkout-paths)]
              [cljs-path (util/find-files cljs-path (conj additional-file-extensions "clj"))]))
        cljs-files (mapcat #(util/find-files % (conj additional-file-extensions "cljs")) (concat cljs-paths checkout-paths [crossover-path]))
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
          (reload-clojure cljs-files (map macro-classpath-files macro-modified) compiler-options notify-command))
        (when (seq clj-modified)
          (reload-clojure cljs-files
            (apply concat
              (for [[cljs-path clj-files] clj-files-in-cljs-paths]
                (map (partial relativize cljs-path) clj-files)))
            compiler-options notify-command))
        (when (or (seq macro-modified) (seq clj-modified) (seq cljs-modified) (seq js-modified))
          (compile-cljs cljs-paths compiler-options notify-command incremental? assert? watching?))))
    dependency-mtimes))
