(ns cljsbuild.compat
  (:use clojure.test))

; ranges are *inclusive* on both ends
(def matrix {"1.0.3" {:cljs ["0.0-2197"]}
             "1.0.3-SNAPSHOT" {:cljs ["0.0-2197"]}
             "1.0.2" {:cljs ["0.0-2014" "0.0-2173"]}
             "1.0.2-SNAPSHOT" {:cljs ["0.0-2014" "0.0-2173"]}
             "1.0.1" {:cljs ["0.0-2014" "0.0-2173"]}
             "1.0.1-SNAPSHOT" {:cljs ["0.0-2014" "0.0-2173"]}
             "1.0.0" {:cljs ["0.0-2014" "0.0-2173"]}
             "1.0.0-SNAPSHOT" {:cljs ["0.0-2014" "0.0-2173"]}
             "1.0.0-alpha2" {:cljs ["0.0-2014" "0.0-2173"]}})

(defn parse-version
  [version-string]
  (if-let [[_ major minor patch git-commit-number]
           (re-matches #"(\d*)(?:\.(\d+))?(?:\.(\d+))?(?:-(.+))?" version-string)]
    [major minor patch (when git-commit-number (format "%05d" (Long/parseLong git-commit-number)))]
    (throw (IllegalArgumentException. (str "Unparseable version: " version-string)))))

(defn version-in-range?
  [version [low high]]
  (if (.contains version "SNAPSHOT")
    true
    (let [[version low high] (map parse-version [version low (or high "9.9.9-99999")])]
      (and (<= (compare low version) 0)
           (<= 0 (compare high version))))))
