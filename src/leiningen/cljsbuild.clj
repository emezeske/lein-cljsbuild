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
    [fs "1.1.2"]
    [emezeske/clojurescript "0.0.4+f4c0de502c"]
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

(defn- run-local-project [project options form]
  (lcompile/eval-in-project
    {:local-repo-classpath true
     :source-path (:source-path project)
     :extra-classpath-dirs (conj
                             (:extra-classpath-dirs project)
                             (:source-path options))
     :dependencies cljsbuild-dependencies}
    form
    nil
    nil
    '(require 'cljsbuild.core))
  exit-success)

(defn- run-compiler [project options watch?]
  (run-local-project project options
    `(cljsbuild.core/run-compiler
       ~(:source-path options)
       '~(:crossovers options)
       ~(:compiler options)
       ~watch?)))

(defn- cleanup-files [project options]
  (run-local-project project options
    `(cljsbuild.core/cleanup-files
       ~(:source-path options)
       '~(:crossovers options)
       ~(:compiler options))))

(defn cljsbuild
  ; TODO: Add a decent docstring.
  ([project]
    (usage)
    exit-failure)
  ([project mode]
    (let [orig-options (:cljsbuild project)]
      (when (nil? orig-options)
        (warn "no :cljsbuild entry found in project definition."))
      (let [compat-options (backwards-compat orig-options)
            options (deep-merge default-options compat-options)]
        (when (not= orig-options compat-options)
          (warn (str
            "your deprecated :cljsbuild config was interpreted as:\n"
            compat-options)))
        (case mode
          "once" (run-compiler project options false)
          "auto" (run-compiler project options true)
          "clean" (cleanup-files project options)
          (do
            (usage)
            exit-failure))))))

(defn compile-hook [task & args]
  (cljsbuild (first args) "once")
  (apply task args))

(defn clean-hook [task & args]
  (cljsbuild (first args) "clean")
  (apply task args))

(hooke/add-hook #'lcompile/compile compile-hook)
(hooke/add-hook #'lclean/clean clean-hook)
