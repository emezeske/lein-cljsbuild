(ns leiningen.cljsbuild.jar
  "Utilities for the cljsbuild jar hook."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as s]
    [leiningen.cljsbuild.config :as config]))

(defn- file-bytes
  "Reads a file into a byte array"
  [file]
  (with-open [fis (java.io.FileInputStream. file)]
    (let [ba (byte-array (.length file))]
      (.read fis ba)
      ba)))

(defn- relative-path
  "Given two normalized path strings, returns a path string of the second relative to the first."
  [parent child]
  (s/replace (s/replace child parent "") #"^[\\/]" ""))

;; The reason we return a :bytes filespec is that it's the only way of
;; specifying a file's destination path inside the jar and is contents
;; independently. Obviously this presents issues if there are any very
;; large files - this should be fixable in leiningen 2.0.
(defn- path-filespecs
  "Given a path, returns a seq of filespecs representing files on the path."
  [path]
  (let [dir (io/file path)
        files (file-seq dir)]
    (for [file (filter #(not (.isDirectory %)) files)]
      {:type :bytes
       :path (relative-path (.getCanonicalPath dir) (.getCanonicalPath file))
       :bytes (file-bytes file)})))

(defn get-filespecs
  "Returns a seq of filespecs for cljs dirs (as passed to leiningen.jar/write-jar)"
  [project]
  (let [options (config/extract-options project)
        builds (:builds options)
        build-paths (map :source-path (filter :jar builds))
        paths (if (:crossover-jar options)
                (conj build-paths (:crossover-path options))
                build-paths)]
    (mapcat path-filespecs paths)))
