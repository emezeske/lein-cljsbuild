(ns leiningen.cljsbuild
  "Compile ClojureScript source into a JavaScript file."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as s]
   [robert.hooke :as hooke]
   [leiningen.compile :as lcompile]
   [leiningen.clean :as lclean]
   [leiningen.jar :as ljar]))

(def cljsbuild-dependencies
  '[[cljsbuild "0.0.14"]])

(def default-compiler
  {:output-to "main.js"
   :optimizations :whitespace
   :pretty-print true
   :output-dir ".clojurescript-output"})

(def default-options
  {:source-path "src-cljs"
   :crossovers [] 
   :compiler default-compiler})

(def relocations
  {:source-dir [:source-path] 
   :output-file [:compiler :output-to]
   :optimizations [:compiler :optimizations] 
   :pretty-print [:compiler :pretty-print]})

(def exit-success 0)

(def exit-failure 1)

(defn- printerr [& args]
  (binding [*out* *err*]
    (apply println args)))  

(defn- warn [& args]
  (apply printerr "WARNING:" args))

(defn- usage []
  (printerr "Usage: lein cljsbuild [once|auto|clean]"))

(declare deep-merge-item)

(defn- deep-merge [& ms]
  (apply merge-with deep-merge-item ms))

(defn- deep-merge-item [a b]
  (if (and (map? a) (map? b))
    (deep-merge a b)
    b))

(defn- backwards-compat [cljsbuild]
  (apply dissoc
    (apply deep-merge cljsbuild
      (for [[source dest] relocations]
        (when-let [value (source cljsbuild)]
          (warn source "is deprecated.")
          (when (nil? (get-in cljsbuild dest))
            (assoc-in {} dest value)))))
    (keys relocations)))

(defn- merge-dependencies [project-dependencies]
  (let [dependency-map #(into {} (map (juxt first rest) %))
        project (dependency-map project-dependencies)
        cljsbuild (dependency-map cljsbuild-dependencies)]
    (map (fn [[k v]] (vec (cons k v)))
      (merge project cljsbuild))))

(defn- run-local-project [project option-seq form]
  (lcompile/eval-in-project
    {:local-repo-classpath true
     :source-path (:source-path project)
     :extra-classpath-dirs (concat
                             (:extra-classpath-dirs project)
                             (map :source-path option-seq))
     :dependencies (merge-dependencies (:dependencies project))
     :dev-dependencies (:dev-dependencies project)}
    form
    nil
    nil
    '(require 'cljsbuild.core))
  exit-success)

(defn- run-compiler [project option-seq watch?]
  (run-local-project project option-seq
                     `(do
                        (println "Compiling ClojureScript")
                        (cljsbuild.core/in-threads
                           (fn [opts#] (cljsbuild.core/run-compiler
                                        (:source-path opts#)
                                        (:crossovers opts#)
                                        (:compiler opts#)
                                        ~watch?))
                           '~option-seq)
                          (shutdown-agents))))

(defn- cleanup-files [project option-seq]
  (run-local-project project option-seq
                     `(do
                        (println "Deleting generated files.")
                        (cljsbuild.core/in-threads
                         (fn [opts#] (cljsbuild.core/cleanup-files
                                      (:source-path opts#)
                                      (:crossovers opts#)
                                      (:compiler opts#)))
                         '~option-seq)
                        (shutdown-agents))))

(defn- normalize-options
  "Sets default options and accounts for backwards compatibility"
  [orig-options]
  (let [compat-options (backwards-compat orig-options)]
    (when (not= orig-options compat-options)
      (warn (str
             "your deprecated :cljsbuild config was interpreted as:\n"
             compat-options)))
    (deep-merge default-options compat-options)))

(defn- extract-options
  "Given a project, returns a seq of cljsbuild option maps."
  [project]
  (when (nil? (:cljsbuild project))
    (warn "no :cljsbuild entry found in project definition."))
  (let [raw-options (:cljsbuild project)]
    (if (map? raw-options)
      [(normalize-options raw-options)]
      (map normalize-options raw-options))))

(defn cljsbuild
  "Run the cljsbuild plugin.

Usage: lein cljsbuild [once|auto|clean]

  once   Compile the ClojureScript project once.
  auto   Automatically recompile when files are modified.
  clean  Remove automatically generated files."
  ([project]
    (usage)
    exit-failure)
  ([project mode]
     (let [option-seq (extract-options project)]
       (case mode
             "once" (run-compiler project option-seq false)
             "auto" (run-compiler project option-seq true)
             "clean" (cleanup-files project option-seq)
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
