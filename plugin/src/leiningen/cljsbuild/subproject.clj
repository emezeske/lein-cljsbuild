(ns leiningen.cljsbuild.subproject
  "Utilities for running cljsbuild in a subproject"
  (:require
    [leiningen.core.main :as lmain]
    [clojure.java.io :refer (resource)]
    [clojure.string :as string]
    [leiningen.core.classpath :as classpath]))

(def cljsbuild-version
  (let [[_ coords version]
        (-> (or (resource "META-INF/leiningen/lein-cljsbuild/lein-cljsbuild/project.clj")
                ; this should only ever come into play when testing cljsbuild itself
                "project.clj")
            slurp
            read-string)]
    (assert (= coords 'lein-cljsbuild)
            (str "Something very wrong, could not find lein-cljsbuild's project.clj, actually found: "
                 coords))
    (assert (string? version)
            (str "Something went wrong, version of lein-cljsbuild is not a string: "
                 version))
    version))

(def required-clojure-version "1.5.1")

(def cljsbuild-dependencies
  [['cljsbuild cljsbuild-version]
   ['org.clojure/clojure required-clojure-version]])

(defn- numeric-version [v]
  (map #(Integer. %) (re-seq #"\d+" (first (string/split v #"-" 2)))))

(defn- version-satisfies? [v1 v2]
  (let [v1 (numeric-version v1)
        v2 (numeric-version v2)]
    (loop [versions (map vector v1 v2)
           [seg1 seg2] (first versions)]
      (cond (empty? versions) true
            (= seg1 seg2) (recur (rest versions) (first (rest versions)))
            (> seg1 seg2) true
            (< seg1 seg2) false))))

(defn check-clojure-version [project-dependencies]
  (if-let [clojure-dependency ('org.clojure/clojure project-dependencies)]
    (let [version (first clojure-dependency)]
      (when (not (version-satisfies? version required-clojure-version))
        (lmain/abort (str "The ClojureScript compiler requires Clojure version >= "
                          required-clojure-version))))))

(defn dependency-map [dependency-vec]
  (apply array-map (mapcat (juxt first rest) dependency-vec)))

(defn array-map-assoc
  "As assoc, except that it preserves the ArrayMap's ordering. If the key is not
  already in the array map, it is put at the end of the array map."
  [amap k v]
  (apply array-map (apply concat (concat (seq amap) [[k v]]))))

(defn merge-dependencies [project-dependencies]
  (let [project (dependency-map project-dependencies)
        desired-cljs-version ('org.clojure/clojurescript project)
        cljsbuild (dependency-map cljsbuild-dependencies)]
    (check-clojure-version project)
    (when-not desired-cljs-version
      (println "\033[33mWARNING: It appears your project does not contain a ClojureScript"
               "dependency. One will be provided for you by lein-cljsbuild, but it"
               "is strongly recommended that you add your own.  You can find a list"
               "of all ClojureScript releases here:")
      (println "http://search.maven.org/#search|gav|1|g%3A%22org.clojure%22%20AND%20a%3A%22clojurescript%22")
      (println "\033[0m"))
    (->> (reduce-kv array-map-assoc cljsbuild project)
         (map (fn [[k v]] (vec (cons k v)))))))

(defn- get-deps-from-project
  [project]
  ;; This checks to see if the user is running lein 2.7 or greater, and, if so,
  ;; merges in the dependency versions from the `:managed-dependencies` vector.
  (if-let [merge-versions-from-managed-coords (resolve 'leiningen.core.classpath/merge-versions-from-managed-coords)]
    (merge-versions-from-managed-coords
     (get project :dependencies)
     (get project :managed-dependencies))
    (get project :dependencies)))

(defn make-subproject [project crossover-path builds]
  (let [deps (get-deps-from-project project)
        remove-compile-task (fn [tasks]
                              (remove (fn [task]
                                        (= task "compile"))
                                      tasks))]
    (with-meta
      ;; Leiningen sets javac and compile as default prep-tasks. We need to remove
      ;; the compile task, so it doesn't trigger the compile hook for all builds.
      ;; https://github.com/emezeske/lein-cljsbuild/issues/451
      (merge (update project :prep-tasks remove-compile-task)
             {:local-repo-classpath true
              :dependencies (merge-dependencies deps)
              :source-paths (concat
                             (:source-paths project)
                             (mapcat :source-paths builds)
                             (filter some? [crossover-path]))})
      (meta project))))
