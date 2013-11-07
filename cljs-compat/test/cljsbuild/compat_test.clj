(ns cljsbuild.compat-test
  (:use clojure.test
        cljsbuild.compat))

(deftest test-version-in-range?
  (are [version range] (version-in-range? version range)
       "0.0-1500" ["0.0-1500"]
       "0.0-1500" ["0.0-1500" "0.0-1500"]
       "0.0-1500" ["0.0-1400" "0.0-2014"]
       "0.0-1500" ["0.0-1500" "0.1-0"])
  (are [version range] (not (version-in-range? version range))
       "0.0-1500" ["0.0-1501"]
       "0.0-1500" ["0.1-0"]))
