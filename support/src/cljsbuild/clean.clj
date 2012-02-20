(ns cljsbuild.clean
  (:require
    [fs.core :as fs]))

(defn cleanup-files [compiler-options]
  (fs/delete (:output-to compiler-options))
  (fs/delete-dir (:output-dir compiler-options)))
