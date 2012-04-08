(ns leiningen.test.cljsbuild.subproject
  (:use
    leiningen.cljsbuild.subproject
    clojure.test))

(deftest test-check-clojure-version
  (are [x] (= nil x)
       (check-clojure-version {'org.clojure/clojure ["1.3.0"]})
       (check-clojure-version {'org.clojure/clojure ["1.4.0"]}))
  (are [x] (thrown? Exception x)
       (check-clojure-version {'org.clojure/clojure ["1.2.0"]})
       (check-clojure-version {})))

(deftest test-merge-dependencies
  (are [x] (apply =
                  (map dependency-map [(concat x cljsbuild-dependencies)
                                       (merge-dependencies x)]))
    [['org.clojure/clojure "1.3.0"] ['a "1"] ['b "2"]]
    [['org.clojure/clojure "1.3.0"] ['cljsbuild "9.9.9"]]))

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

(deftest test-make-subproject-lein1
  (let [subproject (make-subproject-lein1 lein1-project lein-crossover lein-builds)]
    (doseq [dir (concat lein-extra-classpath-dirs [lein-build-path lein-crossover])]
      (is (some #{dir} (:extra-classpath-dirs subproject))))
    (is (= lein-source-path (:source-path subproject)))
    (is (:local-repo-classpath subproject))
    (is (= lein-dev-dependencies (:dev-dependencies subproject)))
    (is (= lein-repositories (:repositories subproject)))
    (is (apply = (map dependency-map [(concat lein-dependencies cljsbuild-dependencies)
                                      (:dependencies subproject)])))))

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

(deftest test-make-subproject-lein2
  (let [subproject (make-subproject-lein2 lein2-project lein-crossover lein-builds)]
    (is (= lein2-metadata (meta subproject)))
    (doseq [dir (concat lein-extra-classpath-dirs [lein-source-path lein-build-path lein-crossover])]
      (is (some #{dir} (:source-paths subproject))))
    (is (:local-repo-classpath subproject))
    (is (= lein-dev-dependencies (:dev-dependencies subproject)))
    (is (= lein-repositories (:repositories subproject)))
    (is (= lein2-eval-in (:eval-in subproject)))
    (is (= lein2-resources-path (:resources-path subproject)))
    (is (apply = (map dependency-map [(concat lein-dependencies cljsbuild-dependencies)
                                      (:dependencies subproject)])))))
