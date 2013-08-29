(ns cljsbuild.piggieback
  "Tools for integrating piggieback into cljsbuild and teaching piggieback about
   cljsbuild crossovers)"
  (:require [cemerick.piggieback :as p]
            [cljsbuild.crossover :as c]
            [clojure.tools.nrepl.middleware :refer [set-descriptor!]]))

(defn make-wrapper
  [transformation]
  (fn [h]
    (let [piggie (p/wrap-cljs-repl h)]
      (fn [{:keys [op session] :as msg}]
        (let [cljs-active? (@session #'p/*cljs-repl-env*)]
          (piggie (if (and cljs-active? (= op "eval"))
                    (update-in msg [:code] transformation)
                    msg)))))))

(def wrap-cljs-repl (make-wrapper
                     #(if (string? %)
                        (c/remove-cljsbuild-comments %)
                        %)))

(set-descriptor! #'wrap-cljs-repl
  {:requires #{"clone"}
   :expects #{"load-file" "eval"}
   :handles {}})
