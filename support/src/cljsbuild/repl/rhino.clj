(ns cljsbuild.repl.rhino
  (:require
    [cljs.repl :as repl]
    [cljs.repl.rhino :as rhino]))

(defn run-repl-rhino []
  (let [env (rhino/repl-env)]
    (repl/repl env)))
