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
(def lein-dev-dependencies [['b "2"]])
(def lein-repositories ["repository"])
(def lein-builds [{:source-path lein-build-path}])

(def lein1-project
  {:dependencies lein-dependencies
   :dev-dependencies lein-dev-dependencies
   :source-path lein-source-path
   :extra-classpath-dirs lein-extra-classpath-dirs
   :repositories lein-repositories})

(fact
  (let [subproject (make-subproject-lein1 lein1-project lein-crossover lein-builds)]
    (doseq [dir (concat lein-extra-classpath-dirs [lein-build-path lein-crossover])]
      (:extra-classpath-dirs subproject) => (contains dir))
    (:source-path subproject) => lein-source-path
    (:local-repo-classpath subproject) => truthy
    (:dev-dependencies subproject) => lein-dev-dependencies
    (:repositories subproject) => lein-repositories
    (:dependencies subproject) => (in-any-order expected-dependencies)))

(def lein2-metadata {:test-metadata "testing 1 2 3"})
(def lein2-eval-in :trampoline)
(def lein2-resource-paths "resources")
(def lein2-project
  (with-meta
    {:dependencies lein-dependencies
     :dev-dependencies lein-dev-dependencies
     :source-paths (concat [lein-source-path] lein-extra-classpath-dirs)
     :repositories lein-repositories
     :eval-in lein2-eval-in
     :resource-paths lein2-resource-paths}
    lein2-metadata))

(fact
  (let [subproject (make-subproject-lein2 lein2-project lein-crossover lein-builds)]
    (meta subproject) => lein2-metadata
    (doseq [dir (concat lein-extra-classpath-dirs [lein-source-path lein-build-path lein-crossover])]
      (:source-paths subproject) => (contains dir))
    (:local-repo-classpath subproject)
    (:dev-dependencies subproject) => lein-dev-dependencies
    (:repositories subproject) => lein-repositories
    (:eval-in subproject) => lein2-eval-in
    (:resource-paths subproject) => lein2-resource-paths
    (:dependencies subproject) => (in-any-order expected-dependencies)))
