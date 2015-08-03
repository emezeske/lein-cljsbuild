(ns leiningen.test.cljsbuild.subproject
  (:require
    [leiningen.core.main :as lmain])
  (:use
    leiningen.cljsbuild.subproject
    midje.sweet))

(background
  (around
    :facts (binding [lmain/*exit-process?* false]
             ?form)))

(fact
  (check-clojure-version {}) => nil
  (check-clojure-version {'org.clojure/clojure [required-clojure-version]}) => nil
  (check-clojure-version {'org.clojure/clojure ["1.3.0"]}) => (throws Exception)
  (check-clojure-version {'org.clojure/clojure ["1.2.0"]}) => (throws Exception))

(def clojure-dependency ['org.clojure/clojure required-clojure-version])

(fact
  (let [original [clojure-dependency ['a "1"] ['b "2"]]
        merged (conj original ['cljsbuild cljsbuild-version])]
    (merge-dependencies original) => (just merged :in-any-order))

  (let [original [['a "1"] ['b "2"]]
        merged (concat original [clojure-dependency ['cljsbuild cljsbuild-version]])]
    (merge-dependencies original) => (just merged :in-any-order)))

(def lein-crossover ".crossovers")
(def lein-build-source-paths ["src-cljs-a"])
(def lein-source-paths ["src-clj"])
(def lein-extra-classpath-dirs ["a" "b"])
(def lein-dependencies [clojure-dependency ['a "1"]])
(def expected-dependencies (conj lein-dependencies ['cljsbuild cljsbuild-version]))
(def lein-repositories ["repository"])
(def lein-builds [{:source-paths lein-build-source-paths}])

(def lein-metadata {:test-metadata "testing 1 2 3"})
(def lein-eval-in :trampoline)
(def lein-resource-paths "resources")
(def lein-project
  (with-meta
    {:dependencies lein-dependencies
     :source-paths (concat lein-source-paths lein-extra-classpath-dirs)
     :repositories lein-repositories
     :eval-in lein-eval-in
     :resource-paths lein-resource-paths}
    lein-metadata))

(fact
  (let [subproject (make-subproject lein-project lein-crossover lein-builds)]
    (meta subproject) => lein-metadata
    (doseq [dir (concat lein-extra-classpath-dirs lein-source-paths lein-build-source-paths [lein-crossover])]
      (:source-paths subproject) => (contains dir))
    (:local-repo-classpath subproject)
    (:repositories subproject) => lein-repositories
    (:eval-in subproject) => lein-eval-in
    (:resource-paths subproject) => lein-resource-paths
    (:dependencies subproject) => (just expected-dependencies :in-any-order)))
