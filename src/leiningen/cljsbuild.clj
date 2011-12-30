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
                    :output-file "main.js"
                    :optimizations "whitespace"
                    :pretty-print true}
          options (merge defaults (:cljsbuild project))]
      (eval-in-project
        {:local-repo-classpath true
         :dependencies (:dependencies project)}
        `(cljsbuild.core/run-compiler
           ~(:source-dir options)
           ~(:output-file options)
           (keyword ~(:optimizations options)) 
           ~(:pretty-print options)
           ~watch)
        nil
        nil
        '(require 'cljsbuild.core)))))
