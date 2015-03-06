(ns cljsbuild.repl.embedded
  "Unifies support for all three embedded REPLs: Rhino, Nashorn & NodeJS"
  (:require [cljs.repl :as repl]
            [cljs.repl.rhino :as rhino]
            [cljs.repl.nashorn :as nashorn]
            [cljs.repl.node :as node]))

(defn run-repl
  "Run a REPL of the specified type"
  [type]
  (repl/repl (case type
               :rhino (rhino/repl-env)
               :nashorn (nashorn/repl-env)
               :node (node/repl-env))))
