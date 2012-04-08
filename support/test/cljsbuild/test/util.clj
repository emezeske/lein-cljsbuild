(ns cljsbuild.test.util
  (:use
    cljsbuild.util
    clojure.test))

(deftest test-join-paths
  (is (= "") (join-paths))
  (is (= "a") (join-paths "a"))
  (is (= "a/b/c") (join-paths "a" "b" "c")))

(deftest test-filter-by-ext
  (let [files ["a.a" "b.b" "c.c" "d.d" "e.d" "f.d" "1/2/3/4/5.e"]]
    (are [x y] (= x y)
      [] (filter-by-ext files #{"q" "r" "s"})  
      ["a.a"] (filter-by-ext files #{"a"}) 
      ["d.d" "e.d" "f.d"] (filter-by-ext files #{"d"}) 
      ["1/2/3/4/5.e"] (filter-by-ext files #{"e"}) 
      files (filter-by-ext files #{"a" "b" "c" "d" "e"}))))

; TODO: More tests!
