(ns cljsbuild.util
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [fs.core :as fs])
  (:import
    (java.io OutputStreamWriter)))

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

(defn- pump-file [reader out]
  (let [buffer (make-array Character/TYPE 1000)]
    (loop [len (.read reader buffer)]
      (when-not (neg? len)
        (.write out buffer 0 len)
        (.flush out)
        (Thread/sleep 100)
        (recur (.read reader buffer))))))

(defn- process-pump [process stdout stderr]
  (with-open [out (io/reader (.getInputStream process))
              err (io/reader (.getErrorStream process))]
    (let [pump-out (doto (Thread. #(pump-file out stdout)) .start)
          pump-err (doto (Thread. #(pump-file err stderr)) .start)]
      (.join pump-out)
      (.join pump-err))))

(defn- process-parse-command [raw]
  (let [[shell tail] (split-with (comp not keyword?) raw)
        opts (apply hash-map tail)
        maybe-file (fn [file fallback]
                     (if file
                       (do
                         (fs/delete file)
                         (io/writer file))
                       fallback))]
    {:shell shell
     ; FIXME: These files get left open.  Not a huge deal, but...
     :stdout (maybe-file (:stdout opts) *out*)
     :stderr (maybe-file (:stderr opts) *err*)}))

(defn process-start [command]
  (let [{:keys [shell stdout stderr]} (process-parse-command command)
        process (.exec (Runtime/getRuntime) (into-array shell))
        pumper (future (process-pump process stdout stderr))]
    {:kill (fn []
             (.destroy process))
     :wait (fn []
             (.waitFor process)
             (deref pumper)
             (.exitValue process))}))
