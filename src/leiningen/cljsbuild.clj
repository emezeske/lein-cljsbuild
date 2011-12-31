(ns leiningen.cljsbuild
  "Compile ClojureScript source into a JavaScript file."
  (:use
    [leiningen.compile :only [eval-in-project]]))

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
          options (merge defaults (:cljsbuild project))]
      (eval-in-project
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
