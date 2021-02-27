(ns cljsbuild.test-test
  (:require [cljsbuild.test :as test]
            [cljsbuild.util :as util]
            [clojure.test :refer [deftest is testing]]))

(deftest run-tests
  (let [command-1 {:shell ["command1"]}
        command-2 {:shell ["command2"]}
        command-3 {:shell ["command3"]}
        commands [["command1" command-1] ["command2" command-2] ["command3" command-3]]]
    (testing "all test commands succeeded"
      (with-redefs [util/sh (fn [_] 0)]
        (is (nil? (test/run-tests commands)))))
    (testing "one test command didn't succeed and an exception is thrown"
      (with-redefs [util/sh (fn [_] 1)]
        (is (thrown? Exception (test/run-tests commands)))))))
