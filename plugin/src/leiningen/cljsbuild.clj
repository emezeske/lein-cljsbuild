(ns leiningen.cljsbuild
  "Compile ClojureScript source into a JavaScript file."
  (:require
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [clojure.string :as s]
   [robert.hooke :as hooke]
   [leiningen.compile :as lcompile]
   [leiningen.clean :as lclean]
   [leiningen.jar :as ljar]))

(def cljsbuild-dependencies
  '[[cljsbuild "0.1.0"]])

(def repl-output-dir ".lein-cljsbuild-repl")
(def default-compiler-output-dir ".lein-cljsbuild-compiler")

(def default-global-options
  {:repl-launch-commands {}
   :repl-listen-port 9000})

(def default-compiler-options
  {:output-to "main.js"
   :optimizations :whitespace
   :pretty-print true})

(def default-build-options
  {:source-path "src-cljs"
   :crossovers [] 
   :compiler default-compiler-options})

(def exit-success 0)

(def exit-failure 1)

(defn- printerr [& args]
  (binding [*out* *err*]
    (apply println args)))  

(defn- warn [& args]
  (apply printerr "WARNING:" args))

(defn- usage []
  (printerr "Usage: lein cljsbuild [once|auto|clean|repl-listen|repl-launch|repl-rhino]"))

(declare deep-merge-item)

(defn- deep-merge [& ms]
  (apply merge-with deep-merge-item ms))

(defn- deep-merge-item [a b]
  (if (and (map? a) (map? b))
    (deep-merge a b)
    b))

(defn- merge-dependencies [project-dependencies]
  (let [dependency-map #(into {} (map (juxt first rest) %))
        project (dependency-map project-dependencies)
        cljsbuild (dependency-map cljsbuild-dependencies)]
    (map (fn [[k v]] (vec (cons k v)))
      (merge project cljsbuild))))

(defn- run-local-project [project builds requires form]
  (lcompile/eval-in-project
    {:local-repo-classpath true
     :source-path (:source-path project)
     :extra-classpath-dirs (concat
                             (:extra-classpath-dirs project)
                             (map :source-path builds))
     :dependencies (merge-dependencies (:dependencies project))
     :dev-dependencies (:dev-dependencies project)}
    form
    nil
    nil
    requires)
  exit-success)

(defn- run-compiler [project {:keys [builds]} watch?]
  (run-local-project project builds
    '(require 'cljsbuild.compiler)
    `(do
      (println "Compiling ClojureScript.")
      (cljsbuild.compiler/in-threads
        (fn [opts#]
          (cljsbuild.compiler/run-compiler
            (:source-path opts#)
            (:crossovers opts#)
            (:compiler opts#)
            ~watch?))
        '~builds)
        (shutdown-agents))))

(defn- cleanup-files [project {:keys [builds]}]
  (run-local-project project builds
    '(require 'cljsbuild.compiler)
    `(do
      (println "Deleting generated files.")
      (cljsbuild.compiler/in-threads
        (fn [opts#]
          (cljsbuild.compiler/cleanup-files
            (:source-path opts#)
            (:crossovers opts#)
            (:compiler opts#)))
      '~builds)
      (shutdown-agents))))

(defn- run-repl-listen [project {:keys [builds repl-listen-port]}]
  (run-local-project project builds
    '(require 'cljsbuild.repl.listen)
    `(do
      (println (str "Running REPL, listening on port " ~repl-listen-port "."))
      (cljsbuild.repl.listen/run-repl-listen
        ~repl-listen-port
        ~repl-output-dir)
      (shutdown-agents))))

(defn- run-repl-launch [project {:keys [builds repl-listen-port repl-launch-commands]} args]
  (when (< (count args) 1)
    (throw (Exception. "Must supply a launch command identifier.")))
  (let [launch-name (first args)
        command-args (rest args)
        command-base (repl-launch-commands launch-name)]
    (when (nil? command-base)
      (throw (Exception. (str "Unknown REPL launch command: " launch-name))))
    (let [command (concat command-base command-args)]
      (run-local-project project builds
        '(require 'cljsbuild.repl.listen)
        `(do
          (println "Running REPL and launching command:" '~command)
          (cljsbuild.repl.listen/run-repl-launch
            ~repl-listen-port
            ~repl-output-dir
            '~command)
          (shutdown-agents))))))

(defn- run-repl-rhino [project {:keys [builds]}]
  (run-local-project project builds 
    '(require 'cljsbuild.repl.rhino)
    `(do
      (println "Running Rhino-based REPL.")
      (cljsbuild.repl.rhino/run-repl-rhino))))

(defn- set-default-build-options [options]
  (deep-merge default-build-options options))

(defn- set-default-output-dirs [options]
  (let [output-dir-key [:compiler :output-dir]
        builds
         (for [[build counter] (map vector (:builds options) (range))]
           (if (get-in build output-dir-key)
             build
             (assoc-in build output-dir-key
               (str default-compiler-output-dir "-" counter))))]
    (if (apply distinct? (map #(get-in % output-dir-key) builds))
      (assoc options :builds builds)
      (throw (Exception. "Compiler :output-dir options must be distinct.")))))

(defn- set-default-global-options [options]
  (deep-merge default-global-options
    (assoc options :builds
      (map set-default-build-options (:builds options)))))

(defn- warn-deprecated [options]
  (warn "your deprecated :cljsbuild config was interpreted as:")
  (pprint/pprint options *err*)
  (printerr
    "See https://github.com/emezeske/lein-cljsbuild/blob/master/README.md"
    "for details on the new format.")
  options)

(defn- normalize-options
  "Sets default options and accounts for backwards compatibility."
  [options]
  (set-default-output-dirs
    (cond
      (and (map? options) (nil? (:builds options)))
        (warn-deprecated
          [{:builds (set-default-build-options options)}])
      (seq? options)
        (warn-deprecated
          [{:builds (map set-default-build-options options)}])
      :else
        (set-default-global-options options))))

(defn- extract-options
  "Given a project, returns a seq of cljsbuild option maps."
  [project]
  (when (nil? (:cljsbuild project))
    (warn "no :cljsbuild entry found in project definition."))
  (let [raw-options (:cljsbuild project)]
    (normalize-options raw-options)))

(defn cljsbuild
  "Run the cljsbuild plugin.

Usage: lein cljsbuild <command>

Available commands:

  once          Compile the ClojureScript project once.
  auto          Automatically recompile when files are modified.
  clean         Remove automatically generated files.

  repl-listen   Run a REPL that will listen for incoming connections.
  repl-launch   Run a REPL and launch a custom command to connect to it.
  repl-rhino    Run a Rhino-based REPL."
  ([project]
    (usage)
    exit-failure)
  ([project mode & args]
     (let [options (extract-options project)]
       (case mode
         "once" (run-compiler project options false)
         "auto" (run-compiler project options true)
         "clean" (cleanup-files project options)
         "repl-listen" (run-repl-listen project options)
         "repl-launch" (run-repl-launch project options args)
         "repl-rhino" (run-repl-rhino project options)
         (do
           (usage)
           exit-failure)))))

(defn- file-bytes
  "Reads a file into a byte array"
  [file]
  (with-open [fis (java.io.FileInputStream. file)]
    (let [ba (byte-array (.length file))]
      (.read fis ba)
      ba)))

(defn- relative-path
  "Given two normalized path strings, returns a path string of the second relative to the first."
  [parent child]
  (s/replace (s/replace child parent "") #"^[\\/]" ""))

;; The reason we return a :bytes filespec is that it's the only way of
;; specifying a file's destination path inside the jar and is contents
;; independently. Obviously this presents issues if there are any very
;; large files - this should be fixable in leiningen 2.0.
(defn- path-filespecs
  "Given a path, returns a seq of filespecs representing files on the path."
  [path]
  (let [dir (io/file path)
        files (file-seq dir)]
    (for [file (filter #(not (.isDirectory %)) files)]
      {:type :bytes
       :path (relative-path (.getCanonicalPath dir) (.getCanonicalPath file))
       :bytes (file-bytes file)})))

(defn- get-filespecs
  "Returns a seq of filespecs for cljs dirs (as passed to leiningen.jar/write-jar)"
  [project]
  (let [builds (extract-options project)
        paths (map :source-path (filter :jar builds))]
    (mapcat path-filespecs paths)))

(defn compile-hook [task & args]
  (cljsbuild (first args) "once")
  (apply task args))

(defn clean-hook [task & args]
  (cljsbuild (first args) "clean")
  (apply task args))

(defn jar-hook [task & [project out-file filespecs]]
  (apply task [project out-file (concat filespecs (get-filespecs project))]))

(hooke/add-hook #'lcompile/compile compile-hook)
(hooke/add-hook #'lclean/clean clean-hook)
(hooke/add-hook #'ljar/write-jar jar-hook)
