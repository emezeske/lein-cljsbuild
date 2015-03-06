(ns cljsbuild.test.repl.embedded
  (:use
    cljsbuild.repl.embedded
    midje.sweet)
  (:require
    [cljs.repl :as repl]
    [cljs.repl.rhino :as rhino]
    [cljs.repl.nashorn :as nashorn]
    [cljs.repl.node :as node]))

(fact
  (run-repl :rhino) => nil
  (provided
    (rhino/repl-env) => {} :times 1
    (repl/repl {}) => nil :times 1))

(fact
  (run-repl :nashorn) => nil
  (provided
    (nashorn/repl-env) => {} :times 1
    (repl/repl {}) => nil :times 1))

(fact
  (run-repl :node) => nil
  (provided
    (node/repl-env) => {} :times 1
    (repl/repl {}) => nil :times 1))


