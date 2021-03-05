;; (ns cljsbuild.test.repl.rhino
;;   (:use
;;     cljsbuild.repl.rhino
;;     midje.sweet)
;;   (:require
;;     [cljs.repl :as repl]
;;     [cljs.repl.rhino :as rhino]))

;; (fact
;;   (run-repl-rhino) => nil
;;   (provided
;;     (rhino/repl-env) => {} :times 1
;;     (repl/repl {}) => nil :times 1))
