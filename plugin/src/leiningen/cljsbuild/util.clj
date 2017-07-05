(ns leiningen.cljsbuild.util
  (:require
   [clojure.string :as s]))

(defn relative-path
  "Given two normalized path strings, returns a path string of the second relative to the first.
  Otherwise returns the second path."
  [parent child]
  (let [relative (s/replace child parent "")]
    (if (= child relative)
      child
      (s/replace relative #"^[\\/]" ""))))
