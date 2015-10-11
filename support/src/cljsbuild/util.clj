(ns cljsbuild.util
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [fs.core :as fs])
  (:import
    (java.io File OutputStreamWriter)
    (java.util Date)))

(defn join-paths [& paths]
  (apply str (interpose "/" paths)))

(defn- filter-by-ext [types files]
  (let [ext #(nth (re-matches #".+\.([^\.]+)$" %) 1)]
    (filter #(types (ext %)) files)))

(defn find-files [dir types]
  ; not using fs because it's slow for listing directories; 40ms vs 1ms for
  ; ~typical `cljsbuild auto` scanning
  (letfn [(files-in-dir [^File dir]
            (let [fs (.listFiles dir)]
              (->> (.listFiles dir)
                   (remove #(.isHidden ^File %))
                   (mapcat #(if (.isFile ^File %)
                              [%]
                              (files-in-dir %))))))]
    (->> (files-in-dir (io/file dir))
      (map #(.getAbsolutePath ^File %))
      (filter-by-ext types))))

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

(def reset-color "\u001b[0m")
(def foreground-red "\u001b[31m")
(def foreground-green "\u001b[32m")
(def foreground-yellow "\u001b[33m")

(defn- colorizer [c]
  (fn [& args]
    (str c (apply str args) reset-color)))

(def red (colorizer foreground-red))
(def green (colorizer foreground-green))
(def yellow (colorizer foreground-yellow))

(def log-format "%tr")

(defn log [& args]
  (apply println (format log-format (Date.)) "-" args))
