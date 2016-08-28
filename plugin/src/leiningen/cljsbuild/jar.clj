(ns leiningen.cljsbuild.jar
  "Utilities for the cljsbuild jar hook."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as s]
    [fs.core :as fs]
    [leiningen.cljsbuild.config :as config]))

(defn file-bytes
  "Reads a file into a byte array"
  [filename]
  (let [file (io/file filename)]
    (with-open [input (java.io.FileInputStream. file)]
      (let [data (byte-array (.length file))]
        (.read input data)
        data))))

(defn relative-path
  "Given two normalized path strings, returns a path string of the second relative to the first."
  [parent child]
  (let [relative (s/replace child parent "")]
    (when (= child relative)
      (throw (Exception.
               (str child " is not a child of " parent))))
    (s/replace relative #"^[\\/]" "")))

(defn join-paths [& paths]
  (apply str (interpose "/" paths)))

(defn canonical-path [path]
  (.getCanonicalPath (io/file path)))

;; The reason we return a :bytes filespec is that it's the only way of
;; specifying a file's destination path inside the jar and is contents
;; independently. Obviously this presents issues if there are any very
;; large files - this should be fixable in leiningen 2.0.
(defn path-filespecs
  "Given a path, returns a seq of filespecs representing files on the path."
  [path]
  (let [filenames (apply concat
                    (for [[root _ filenames] (fs/iterate-dir path)]
                      (for [file filenames]
                        (join-paths root file))))]
    (for [filename filenames]
      {:type :bytes
       :path (relative-path (canonical-path path) (canonical-path filename))
       :bytes (file-bytes filename)})))

(defn get-filespecs
  "Returns a seq of filespecs for cljs dirs (as passed to leiningen.jar/write-jar)"
  [project]
  (let [options (config/extract-options project)
        builds (:builds options)
        build-paths (mapcat :source-paths (filter :jar builds))]
    (mapcat path-filespecs build-paths)))
