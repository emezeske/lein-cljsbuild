(ns leiningen.test.cljsbuild.jar
  (:use
    leiningen.cljsbuild.jar
    clojure.test))

(deftest test-relative-path
  (is (= "a" (relative-path "/" "/a")))
  (is (= "d/e" (relative-path "/a/b/c" "/a/b/c/d/e")))
  (is (thrown? Exception (relative-path "" "a")))
  (is (thrown? Exception (relative-path "/a/b/c" "/a/b"))))

; TODO: More tests!
