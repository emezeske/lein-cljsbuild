(ns cljsbuild.util
  (:require
    [clojure.string :as string]
    [fs.core :as fs]))

(defn join-paths [& paths]
  (apply str (interpose "/" paths)))

(defn filter-by-ext [files types]
  (let [ext #(last (string/split % #"\."))]
    (filter #(types (ext %)) files)))

(defn find-dir-files [root files types]
  (for [files (filter-by-ext files types)]
    (join-paths root files)))

(defn find-files [dir types]
  (let [iter (fs/iterate-dir dir)]
    (mapcat
      (fn [[root _ files]]
        (find-dir-files root files types))
      iter)))

(defn in-threads
  "Given a seq and a function, applies the function to each item in a different thread
and returns a seq of the results. Launches all the threads at once."
  [f s]
  (doall (map deref (doall (map #(future (f %)) s)))))

(defn once-every [ms desc f]
  (future
    (while true
      (Thread/sleep ms)
      (try
        (f)
        (catch Exception e
          (println (str "Error " desc ": " e)))))))
