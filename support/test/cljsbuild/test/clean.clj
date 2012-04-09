(ns cljsbuild.test.clean
  (:use
    cljsbuild.clean
    midje.sweet)
  (:require
    [fs.core :as fs]))

(fact
  (let [output-to "a"
        output-dir "b"]
    (cleanup-files {:output-to output-to :output-dir output-dir}) => nil
    (provided (fs/delete output-to) => nil :times 1)
    (provided (fs/delete-dir output-dir) => nil :times 1)))
