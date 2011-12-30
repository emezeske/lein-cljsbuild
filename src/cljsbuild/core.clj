(ns cljsbuild.core
  (:require
    [clojure.java.io :as io]
    [clojure.string :as string]
    [clj-stacktrace.repl :as st]
    [fs :as fs] 
    [cljs.closure :as cljsc]))

(def tmpdir "/tmp/clojurescript-output")

(defn- get-mtime [file]
  (.lastModified (io/file file)))

(defn- filter-cljs [files]
  (let [ext #(last (string/split % #"\."))]
    ; Need to return *.clj as well as *.cljs because ClojureScript
    ; macros are written in Clojure.
    (filter #(#{"clj" "cljs"} (ext %) ) files)))

(defn- find-dir-cljs [root files]
  (for [cljs (filter-cljs files)] (fs/join root cljs)))

(defn- find-cljs [dir]
  (let [iter (fs/iterdir dir)]
    (mapcat
      (fn [[root _ files]]
        (find-dir-cljs root files))
      iter)))

(defn- elapsed [started-at]
  (let [elapsed-us (- (. System (nanoTime)) started-at)]
    (with-precision 2
      (str (/ (double elapsed-us) 1000000000) " seconds"))))

(defn- compile-cljs [source-dir output-file optimizations pretty?]
  (print (str "Compiling " output-file " from " source-dir "..."))
  (flush)
  (when (.exists (io/file tmpdir))
    (fs/delete tmpdir))
  (let [started-at (. System (nanoTime))]
    (try
      (cljsc/build
        source-dir
        {:optimizations optimizations
         :pretty-print pretty?
         :output-to output-file
         :output-dir tmpdir})
      (println (str " Done in " (elapsed started-at) "."))
      (catch Exception e
        (println " Failed!")
        (st/pst+ e)))))

(defn run-compiler [source-dir output-file optimizations pretty? watch?]
  (println "Compiler started.")
  (loop [last-input-mtimes {}]
    (let [output-mtime (get-mtime output-file)
          input-files (find-cljs source-dir)
          input-mtimes (map get-mtime input-files)]
      (when (and
              (not= input-mtimes last-input-mtimes)
              (some #(< output-mtime %) input-mtimes))
        (compile-cljs source-dir output-file optimizations pretty?))
      (when watch?
        (Thread/sleep 100)
        (recur input-mtimes)))))
