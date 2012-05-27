(ns cljsbuild.test.crossover
  (:use
    cljsbuild.crossover
    midje.sweet)
  (:require
    [clojure.java.io :as io]
    [fs.core :as fs]))

(def crossover-path "/project/crossovers")
(def crossovers '[a a.a a.b])

(fact
  (copy-crossovers crossover-path crossovers) => nil
  (provided
    (find-crossovers crossovers false) =>
      [["/project/src" (io/file "/project/src/a.clj")]
       ["/project/src" (io/file "/project/src/a/a.clj")]
       ["/project/src" (io/file "/project/src/a/b.clj")]] :times 1
    (fs/mkdirs anything) => nil
    (crossover-needs-update? anything anything) => true
    (write-crossover anything anything) => nil :times 3))

(def cljsbuild-remove ";*CLJSBUILD-REMOVE*;")
(def clojurescript-source
  (str
    "(ns a)
    " cljsbuild-remove "
    (def a 5)"))

(defn- tempfile []
  (doto (fs/temp-file) .deleteOnExit))

(let [from-resource (tempfile)
      to-file (tempfile)]
  (spit from-resource clojurescript-source)
  (fact "crossover files are copied/edited correctly"
    (write-crossover from-resource to-file) => anything
    (.indexOf (slurp to-file) cljsbuild-remove) => -1))

; TODO: It would be nice to test more of the crossover features, but they
;       are pretty heavily dependent on interop and resources, which makes
;       that difficult.
