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
  (let [files ["a.a" "b.b" "c.c" "d.d" "e.d" "f.d" "1/2/3/4/5.e"]
        filter-by-ext #'cljsbuild.util/filter-by-ext]
    (filter-by-ext #{"q" "r" "s"} files) => []
    (filter-by-ext #{"a"} files) => ["a.a"]
    (filter-by-ext #{"d"} files) => ["d.d" "e.d" "f.d"]
    (filter-by-ext #{"e"} files) => ["1/2/3/4/5.e"]
    (filter-by-ext #{"a" "b" "c" "d" "e"} files) => files))

(defn- path-ends-with? [exts path]
  (some #(.endsWith path %) exts))

(fact (find-files ".." #{"md"}) => #(every? (partial path-ends-with? #{".md"}) %))

(unfinished call-once-every)
(unfinished keep-going)

(fact
  (once-every 1000 "description" call-once-every keep-going) => nil
  (provided
    (keep-going) =streams=> [true true true false] :times 4
    (call-once-every) => nil :times 3
    (sleep 1000) => nil :times 3))

; TODO: It would be nice to test process-start, but it does a lot of Java
;       interop so I'm not sure how to go about that just yet.  Maybe if
;       it was switched to using "conch" instead of raw interop it would
;       be easier to test?

(fact
  (let [command {:shell ["command" "arg1" "arg2"]}]
    (sh command) => 0
    (provided
      (process-start command) => {:wait (fn [] 0)} :times 1)))
