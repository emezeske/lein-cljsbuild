(ns cljsbuild.test.util
  (:use
    cljsbuild.util
    midje.sweet)
  (:require
    [clojure.java.io :as io]
    [fs.core :as fs]))

(fact
  (join-paths) => ""
  (join-paths "a") => "a"
  (join-paths "a" "b" "c") => "a/b/c")

(fact
  (let [files ["a.a" "b.b" "c.c" "d.d" "e.d" "f.d" "1/2/3/4/5.e"]]
    (filter-by-ext files #{"q" "r" "s"}) => []
    (filter-by-ext files #{"a"}) => ["a.a"]
    (filter-by-ext files #{"d"}) => ["d.d" "e.d" "f.d"]
    (filter-by-ext files #{"e"}) => ["1/2/3/4/5.e"]
    (filter-by-ext files #{"a" "b" "c" "d" "e"}) => files))

(fact
  (find-files "a" #{"z"}) => ["a/j.z" "a/k.z" "a/l.z" "a/b/m.z" "a/b/n.z"]
  (provided (fs/iterate-dir "a") =>
    [["a" "" ["i.a" "j.z" "k.z" "l.z"]]
     ["a/b" "" ["m.z" "n.z" "o.b"]]]
    :times 1))

(defn- call-in-threads [x]
  (inc x))

(fact
  (in-threads call-in-threads [1 2 3]) => [2 3 4])

(unfinished call-once-every)
(unfinished keep-going)

(fact
  (once-every 1000 "description" call-once-every keep-going) => nil
  (provided
    (keep-going) =streams=> [true true true false] :times 4
    (call-once-every) => nil :times 3
    (sleep 1000) => nil :times 3))

(fact
  (maybe-writer "filename" *out*) => :success
  (provided
    (fs/delete "filename") => nil :times 1
    (io/writer "filename") => :success :times 1))

(fact
  (maybe-writer nil *out*) => *out*)

; TODO: It would be nice to test process-start, but it does a lot of Java
;       interop so I'm not sure how to go about that just yet.  Maybe if
;       it was switched to using "conch" instead of raw interop it would
;       be easier to test?

(fact
  (let [command {:shell ["command" "arg1" "arg2"]}]
    (sh command) => 0
    (provided
      (process-start command) => {:wait (fn [] 0)} :times 1)))
