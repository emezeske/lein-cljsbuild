(ns leiningen.cljsbuild.subproject-test
  (:require [clojure.test :refer [deftest is testing]]
            [leiningen.cljsbuild.subproject :as subproject]
            [leiningen.core.main :as lmain]
            [me.raynes.fs :as fs]))

(deftest check-clojure-version
  (binding [lmain/*exit-process?* false]
    (is (nil? (subproject/check-clojure-version {})))
    (is (nil? (subproject/check-clojure-version {'org.clojure/clojure [subproject/required-clojure-version]})))
    (is (thrown? Exception (subproject/check-clojure-version {'org.clojure/clojure ["1.3.0"]})))
    (is (thrown? Exception (subproject/check-clojure-version {'org.clojure/clojure ["1.2.0"]})))))

(def clojure-dependency ['org.clojure/clojure subproject/required-clojure-version])
(def clojurescript-dependency ['org.clojure/clojurescript "0.0-3211"])

(deftest merge-dependencies
  (let [original [clojure-dependency clojurescript-dependency ['a "1"] ['b "2"]]
        merged (conj original ['cljsbuild subproject/cljsbuild-version])]
    (is (= (set (subproject/merge-dependencies original)) (set merged))))
  (let [original [clojurescript-dependency ['a "1"] ['b "2"]]
        merged (concat original [clojure-dependency ['cljsbuild subproject/cljsbuild-version]])]
    (is (= (set (subproject/merge-dependencies original)) (set merged)))))

(def lein-build-source-paths ["src-cljs-a"])
(def lein-source-paths ["src-clj"])
(def lein-extra-classpath-dirs ["a" "b"])
(def lein-dependencies [clojure-dependency clojurescript-dependency ['a "1"]])
(def expected-dependencies (conj lein-dependencies ['cljsbuild subproject/cljsbuild-version]))
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

(deftest make-subproject
  (let [subproject (subproject/make-subproject lein-project lein-builds)
        source-paths (set (:source-paths subproject))]
    (doseq [dir (concat lein-extra-classpath-dirs lein-source-paths lein-build-source-paths)]
      (is (contains? source-paths dir)))
    (is (= (meta subproject) lein-metadata))
    (is (:local-repo-classpath subproject))
    (is (= (:repositories subproject) lein-repositories))
    (is (= (:eval-in subproject) lein-eval-in))
    (is (= (:resource-paths subproject) lein-resource-paths))
    (is (= (set (:dependencies subproject)) (set expected-dependencies)))))
