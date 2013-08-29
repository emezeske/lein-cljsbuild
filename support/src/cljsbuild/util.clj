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

(defn remove-hidden [files]
  (remove #(.startsWith % ".") files))

(defn find-dir-files [root files types]
  (for [file (remove-hidden (filter-by-ext files types))]
    (join-paths root file)))

(defn find-files [dir types]
  (let [iter (fs/iterate-dir dir)]
    (mapcat
      (fn [[root _ files]]
        (find-dir-files root files types))
      iter)))

(defn sleep [ms]
  (Thread/sleep ms))

(defn once-every
  ([ms desc f keep-going]
    (while (keep-going)
      (try
        (f)
        (catch Exception e
          (println (str "Error " desc ": " e))))
      (sleep ms)))
  ([ms desc f]
    (once-every ms desc f (fn [] true))))

(defn once-every-bg [& args]
  (future
    (apply once-every args)))

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

(defn maybe-writer [file fallback]
  (if file
    (do
      (fs/delete file)
      (io/writer file))
    fallback))

(defn process-start [{:keys [shell stdout stderr]}]
  ; FIXME: These writers get left open.  Not a huge deal, but...
  (let [stdout-writer (maybe-writer stdout *out*)
        stderr-writer (maybe-writer stderr *err*)
        process (.exec (Runtime/getRuntime) (into-array String shell))
        pumper (future (process-pump process stdout-writer stderr-writer))]
    {:kill (fn []
             (.destroy process))
     :wait (fn []
             (.waitFor process)
             (deref pumper)
             (.exitValue process))}))

(defn sh [command]
  (let [process (process-start command)]
    ((:wait process))))
