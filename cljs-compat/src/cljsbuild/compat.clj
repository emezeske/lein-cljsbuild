(ns cljsbuild.compat
  (:use clojure.test))

; ranges are *inclusive* on both ends
(def matrix
  (->> {["0.0-2197"] #{"1.0.4" "1.0.4-SNAPSHOT" "1.0.3" "1.0.3-SNAPSHOT"}
        ["0.0-2014" "0.0-2173"] #{"1.0.2" "1.0.2-SNAPSHOT" "1.0.1" "1.0.1-SNAPSHOT"
                                  "1.0.0" "1.0.0-SNAPSHOT" "1.0.0-alpha2"}}
    (mapcat (fn [[cljs-range cljsbuild-versions]]
              (map #(vector % {:cljs cljs-range}) cljsbuild-versions)))
    (into {})))

(defn parse-version
  [version-string]
  (if-let [[_ major minor patch git-commit-number]
           (re-matches #"(\d*)(?:\.(\d+))?(?:\.(\d+))?(?:-(.+))?" version-string)]
    [major minor patch (when git-commit-number (format "%05d" (Long/parseLong git-commit-number)))]
    (throw (IllegalArgumentException. (str "Unparseable version: " version-string)))))

(defn version-in-range?
   [version [low high]]
   (let [[version low high] (map parse-version [version low (or high "9.9.9-99999")])]
     (and (<= (compare low version) 0)
          (<= 0 (compare high version)))))

