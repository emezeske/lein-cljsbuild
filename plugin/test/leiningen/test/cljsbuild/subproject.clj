(ns leiningen.test.cljsbuild.subproject
  (:use
    leiningen.cljsbuild.subproject
    midje.sweet))

(fact
  (check-clojure-version {}) => nil
  (check-clojure-version {'org.clojure/clojure [required-clojure-version]}) => nil
  (check-clojure-version {'org.clojure/clojure ["1.3.0"]}) => (throws Exception)
  (check-clojure-version {'org.clojure/clojure ["1.2.0"]}) => (throws Exception))

(def clojure-dependency ['org.clojure/clojure required-clojure-version])

(fact
  (let [original [clojure-dependency ['a "1"] ['b "2"]]
        merged (conj original ['cljsbuild cljsbuild-version])]
    (merge-dependencies original) => (in-any-order merged))

  (let [original [['a "1"] ['b "2"]]
        merged (concat original [clojure-dependency ['cljsbuild cljsbuild-version]])]
    (merge-dependencies original) => (in-any-order merged)))

(def lein-crossover ".crossovers")
(def lein-build-path "src-cljs-a")
(def lein-source-path "src-clj")
(def lein-extra-classpath-dirs ["a" "b"])
(def lein-dependencies [clojure-dependency ['a "1"]])
(def expected-dependencies (conj lein-dependencies ['cljsbuild cljsbuild-version]))
(def lein-repositories ["repository"])
(def lein-builds [{:source-path lein-build-path}])

(def lein-metadata {:test-metadata "testing 1 2 3"})
(def lein-eval-in :trampoline)
(def lein-resource-paths "resources")
(def lein-project
  (with-meta
    {:dependencies lein-dependencies
     :source-paths (concat [lein-source-path] lein-extra-classpath-dirs)
     :repositories lein-repositories
     :eval-in lein-eval-in
     :resource-paths lein-resource-paths}
    lein-metadata))

(fact
  (let [subproject (make-subproject lein-project lein-crossover lein-builds)]
    (meta subproject) => lein-metadata
    (doseq [dir (concat lein-extra-classpath-dirs [lein-source-path lein-build-path lein-crossover])]
      (:source-paths subproject) => (contains dir))
    (:local-repo-classpath subproject)
    (:repositories subproject) => lein-repositories
    (:eval-in subproject) => lein-eval-in
    (:resource-paths subproject) => lein-resource-paths
    (:dependencies subproject) => (in-any-order expected-dependencies)))
