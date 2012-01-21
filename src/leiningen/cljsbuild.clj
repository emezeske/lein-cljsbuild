(ns leiningen.cljsbuild
  "Compile ClojureScript source into a JavaScript file."
  (:require
    [robert.hooke :as hooke]
    [leiningen.compile :as lcompile]
    [leiningen.clean :as lclean]))

; TODO: These are really the same as the :dependencies for the
;       lein-cljsbuild project itself (e.g. in the toplevel project
;       file).  I haven't yet figured out a clean way to DRY them.
(def cljsbuild-dependencies
  '[[org.clojure/clojure "1.3.0"]
    [org.clojure/clojurescript "0.0-927"]
    [fs "1.1.2"]
    [clj-stacktrace "0.2.4"]])

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

(defn- run-local-project [project option-seq form]
  (lcompile/eval-in-project
    {:local-repo-classpath true
     :source-path (:source-path project)
     :extra-classpath-dirs (concat
                             (:extra-classpath-dirs project)
                             (map :source-path option-seq))
     :dependencies cljsbuild-dependencies}
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
     (when (nil? (:cljsbuild project))
       (warn "no :cljsbuild entry found in project definition."))
     (let [raw-options (:cljsbuild project)
           option-seq (if (map? raw-options)
                        [(normalize-options raw-options)]
                        (map normalize-options raw-options))]
       (case mode
             "once" (run-compiler project option-seq false)
             "auto" (run-compiler project option-seq true)
             "clean" (cleanup-files project option-seq)
             (do
               (usage)
               exit-failure)))))

(defn compile-hook [task & args]
  (cljsbuild (first args) "once")
  (apply task args))

(defn clean-hook [task & args]
  (cljsbuild (first args) "clean")
  (apply task args))

(hooke/add-hook #'lcompile/compile compile-hook)
(hooke/add-hook #'lclean/clean clean-hook)
