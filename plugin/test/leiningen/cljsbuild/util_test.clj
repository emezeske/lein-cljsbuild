(ns leiningen.cljsbuild.util-test
  (:require [clojure.test :refer [deftest is testing]]
            [leiningen.cljsbuild.util :as util]))

(deftest relative-path
  (is (= (util/relative-path "/" "/a") "a"))
  (is (= (util/relative-path "/a/b/c" "/a/b/c/d/e") "d/e"))
  (is (= (util/relative-path "" "a") "a"))
  (is (= (util/relative-path "/a/b/c" "/a/b") "/a/b")))
