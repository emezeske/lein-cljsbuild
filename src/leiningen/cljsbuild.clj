(ns leiningen.cljsbuild
  "Compile ClojureScript source into a JavaScript file."
  (:use
    [leiningen.compile :only [eval-in-project]]))

(defn cljsbuild [project]
  (let [defaults {:source-dir "src-cljs"
                  :output-file "main.js"
                  :optimizations "whitespace"
                  :pretty-print true
                  :watch true}
        options (merge defaults (:cljsbuild project))]
    (eval-in-project
      {:local-repo-classpath true
       :dependencies (:dependencies project)}
      `(cljsbuild.core/run-compiler
         ~(:source-dir options)
         ~(:output-file options)
         (cljsbuild.core/get-input-files true ~(:source-dir options) [])
         ~(:optimizations options)
         ~(:pretty-print options)
         ~(:watch options))
      nil
      nil
      '(require 'cljsbuild.core))))
