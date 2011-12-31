(ns leiningen.cljsbuild
  "Compile ClojureScript source into a JavaScript file."
  (:require
    [robert.hooke :as hooke]
    [leiningen.compile :as lcompile]))

(defn cljsbuild
  ([project]
    (binding [*out* *err*]
      (println "Usage: lein cljsbuild [once|auto]"))) 
  ([project mode]
    (let [watch (case mode
                  "once" false
                  "auto" true)
          defaults {:source-dir "src-cljs"
                    :crossovers [] 
                    :output-file "main.js"
                    :optimizations "whitespace"
                    :pretty-print true}
          cljsbuild (:cljsbuild project)
          options (merge defaults cljsbuild)]
      (when (nil? cljsbuild)
        (println "WARNING: no :cljsbuild entry found in project definition."))
      (lcompile/eval-in-project
        {:local-repo-classpath true
         :extra-classpath-dirs [(:source-dir options)] 
         :dependencies (:dependencies project)}
        `(cljsbuild.core/run-compiler
           ~(:source-dir options)
           ~(:crossovers options)
           ~(:output-file options)
           (keyword ~(:optimizations options)) 
           ~(:pretty-print options)
           ~watch)
        nil
        nil
        '(require 'cljsbuild.core)))))

(defn cljsbuild-hook [task & args]
  (cljsbuild (first args) "once")
  (apply task args))

(hooke/add-hook #'lcompile/compile cljsbuild-hook)
