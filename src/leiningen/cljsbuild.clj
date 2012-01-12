(ns leiningen.cljsbuild
  "Compile ClojureScript source into a JavaScript file."
  (:require
    [robert.hooke :as hooke]
    [leiningen.compile :as lcompile]))

; TODO: These are really the same as the :dependencies for the
;       lein-cljsbuild project itself (e.g. in the toplevel project
;       file).  I haven't yet figured out a clean way to DRY them.
(def cljsbuild-dependencies
  '[[org.clojure/clojure "1.3.0"]
    [fs "0.11.1"]
    [emezeske/clojurescript "0.0.2-0df1bc4d7b"]
    [clj-stacktrace "0.2.3"]])

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

(def exit-failure 1)

(defn- printerr [& args]
  (binding [*out* *err*]
    (apply println args)))  

(defn- warn [& args]
  (apply printerr "WARNING:" args))

(defn- usage []
  (printerr "Usage: lein cljsbuild [once|auto]"))

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

(defn cljsbuild
  ([project]
    (usage)
    exit-failure)
  ([project mode]
    (let [cljsbuild (:cljsbuild project)
          watch? (case mode "once" false "auto" true nil)]
      (when (nil? cljsbuild)
        (warn "no :cljsbuild entry found in project definition."))
      (if (nil? watch?)
        (do
          (usage)
          exit-failure)
        (let [compat-cljsbuild (backwards-compat cljsbuild)
              options (deep-merge default-options compat-cljsbuild)]
          (when (not= cljsbuild compat-cljsbuild)
            (warn (str
              "your deprecated :cljsbuild config was interpreted as:\n"
              compat-cljsbuild)))
          (lcompile/eval-in-project
            {:local-repo-classpath true
             :extra-classpath-dirs [(:source-path options)] 
             :dependencies cljsbuild-dependencies}
            `(cljsbuild.core/run-compiler
               ~(:source-path project)
               ~(:source-path options)
               '~(:crossovers options)
               ~(:compiler options)
               ~watch?)
            nil
            nil
            '(require 'cljsbuild.core)))))))

(defn cljsbuild-hook [task & args]
  (cljsbuild (first args) "once")
  (apply task args))

(hooke/add-hook #'lcompile/compile cljsbuild-hook)
