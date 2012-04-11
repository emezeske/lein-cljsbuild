(ns cljsbuild.test.repl.listen
  (:use
    cljsbuild.repl.listen
    midje.sweet)
  (:require
    [cljs.repl :as repl]
    [cljs.repl.browser :as browser]
    [cljsbuild.util :as util]))

(def port (Integer. 1234))
(def output-dir "output-dir")
(def command {:shell ["command"]})

(fact
  (run-repl-listen port output-dir) => nil

  (binding [*out* (new java.io.StringWriter)]
    (run-repl-launch port output-dir command)) => nil
  (provided
    (delayed-process-start command) => (future {:kill (fn [] nil) :wait (fn [] nil)}))

  (against-background
    (browser/repl-env :port port :working-dir output-dir) => {} :times 1
    (repl/repl {}) => nil :times 1))
