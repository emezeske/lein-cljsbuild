(ns cljsbuild.test.test
  (:use
    cljsbuild.test
    midje.sweet)
  (:require
    [cljsbuild.util :as util]))

(fact
  (let [command-1 {:shell ["command1"]}
        command-2 {:shell ["command2"]}
        command-3 {:shell ["command3"]}
        commands [command-1 command-2 command-3]]
    (run-tests commands) => nil
    (provided
      (util/sh command-1) => 0 :times 1
      (util/sh command-2) => 0 :times 1
      (util/sh command-3) => 0 :times 1)))

(fact
  (let [command-1 {:shell ["command1"]}
        command-2 {:shell ["command2"]}
        commands [command-1 command-2]]
    (run-tests commands) => (throws cljsbuild.test.TestsFailedException)
    (provided
      (util/sh command-1) => 0 :times 1
      (util/sh command-2) => 1 :times 1)))
