(ns cljsbuild.util
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string])
  (:import
    (java.io File OutputStreamWriter)
    (java.lang ProcessBuilder$Redirect)
    (java.util List)))

(defn join-paths [& paths]
  (apply str (interpose "/" paths)))

(defn- filter-by-ext [types files]
  (let [ext #(nth (re-matches #".+\.([^\.]+)$" %) 1)]
    (filter #(types (ext %)) files)))

(defn find-files [dir types]
  ; not using fs because it's slow for listing directories; 40ms vs 1ms for
  ; ~typical `cljsbuild auto` scanning
  (letfn [(files-in-dir [^File dir]
            (->> (.listFiles dir)
                 (remove #(.isHidden ^File %))
                 (mapcat #(if (.isFile ^File %)
                            [%]
                            (files-in-dir %)))))]
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

(defn process-start [{:keys [shell stdout stderr]}]
  (let [process (-> (ProcessBuilder. ^List shell)
                    (.redirectOutput (if stdout (ProcessBuilder$Redirect/to stdout) ProcessBuilder$Redirect/INHERIT))
                    (.redirectError (if stderr (ProcessBuilder$Redirect/to stderr) ProcessBuilder$Redirect/INHERIT))
                    (.start))]
    {:kill (fn []
             (.destroy process))
     :wait (fn []
             (.waitFor process)
             (.exitValue process))}))

(defn sh [command]
  (let [process (process-start command)]
    ((:wait process))))
