(ns cljsbuild.util-test
  (:require [cljsbuild.util :as util]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]))

(deftest filter-by-ext
  (let [files ["a.a" "b.b" "c.c" "d.d" "e.d" "f.d" "1/2/3/4/5.e"]]
    (is (= (util/filter-by-ext #{"q" "r" "s"} files) []))
    (is (= (util/filter-by-ext #{"a"} files) ["a.a"]))
    (is (= (util/filter-by-ext #{"d"} files) ["d.d" "e.d" "f.d"]))
    (is (= (util/filter-by-ext #{"e"} files) ["1/2/3/4/5.e"]))
    (is (= (util/filter-by-ext #{"a" "b" "c" "d" "e"} files) files))))

(deftest find-files
  (let [path-ends-with? (fn [exts path] (some #(.endsWith path %) exts))]
    (is (true? (every? (partial path-ends-with? #{".md"})
                       (util/find-files ".." #{"md"}))))))
