(ns leiningen.test.cljsbuild.subproject
  (:use
    leiningen.cljsbuild.subproject
    midje.sweet))

(fact
  (check-clojure-version {'org.clojure/clojure ["1.3.0"]}) => nil
  (check-clojure-version {'org.clojure/clojure ["1.4.0"]}) => nil
  (check-clojure-version {'org.clojure/clojure ["1.2.0"]}) => (throws Exception)
  (check-clojure-version {}) => (throws Exception))

(defn- merge-dependencies-map [dependencies]
  (-> dependencies
    merge-dependencies
    dependency-map))

(defn- expected-dependencies-map [dependencies]
  (-> dependencies
    (concat cljsbuild-dependencies)
    dependency-map))

(fact
  (for [dependencies [[['org.clojure/clojure "1.3.0"] ['a "1"] ['b "2"]]
                      [['org.clojure/clojure "1.3.0"] ['cljsbuild "9.9.9"]]]]
    (merge-dependencies-map dependencies) => (expected-dependencies-map dependencies)))

(def lein-crossover ".crossovers")
(def lein-build-path "src-cljs-a")
(def lein-source-path "src-clj")
(def lein-extra-classpath-dirs ["a" "b"])
(def lein-dependencies [['org.clojure/clojure "1.3.0"] ['a "1"]])
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
    (dependency-map (:dependencies subproject)) => (expected-dependencies-map lein-dependencies)))

(def lein2-metadata {:test-metadata "testing 1 2 3"})
(def lein2-eval-in :trampoline)
(def lein2-resources-path "resources")
(def lein2-project
  (with-meta
    {:dependencies lein-dependencies
     :dev-dependencies lein-dev-dependencies
     :source-paths (concat [lein-source-path] lein-extra-classpath-dirs)
     :repositories lein-repositories
     :eval-in lein2-eval-in
     :resources-path lein2-resources-path}
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
    (:resources-path subproject) => lein2-resources-path
    (dependency-map (:dependencies subproject)) => (expected-dependencies-map lein-dependencies)))
