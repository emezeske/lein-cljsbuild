(ns leiningen.cljsbuild.subproject
  "Utilities for running cljsbuild in a subproject"
  (:require
    [clojure.string :as string]))

(def cljsbuild-version "0.3.0")
(def required-clojure-version "1.4.0")

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
        (throw
          (Exception.
            (str "The ClojureScript compiler requires Clojure version >= " required-clojure-version)))))))

(defn dependency-map [dependency-vec]
  (into {} (map (juxt first rest) dependency-vec)))

(defn merge-dependencies [project-dependencies]
  (let [project (dependency-map project-dependencies)
        cljsbuild (dependency-map cljsbuild-dependencies)]
    (check-clojure-version project)
    (map (fn [[k v]] (vec (cons k v)))
      (merge cljsbuild project))))

(defn make-subproject [project crossover-path builds]
  {:local-repo-classpath true
   :dependencies (merge-dependencies (:dependencies project))
   :dev-dependencies (:dev-dependencies project)
   :repositories (:repositories project)})

(defn make-subproject-lein1 [project crossover-path builds]
  (merge (make-subproject project crossover-path builds)
    {:source-path (:source-path project)
     :resources-path (:resources-path project)
     :extra-classpath-dirs (concat
                             (:extra-classpath-dirs project)
                             (map :source-path builds)
                             [crossover-path])}))

(defn make-subproject-lein2 [project crossover-path builds]
  (with-meta
    (merge (make-subproject project crossover-path builds)
      {:source-paths (concat
                       (:source-paths project)
                       (map :source-path builds)
                       [crossover-path])
       :resources-path (:resources-path project)
       :checkout-deps-shares (:checkout-deps-shares project)
       :eval-in (:eval-in project)})
    (meta project)))

(defn eval-in-project
  "Support eval-in-project in both Leiningen 1.x and 2.x."
  [project crossover-path builds form requires]
  (let [[eip args]
         (or (try (require 'leiningen.core.eval)
                  [(resolve 'leiningen.core.eval/eval-in-project)
                    [(make-subproject-lein2 project crossover-path builds)
                     form
                     requires]]
                  (catch java.io.FileNotFoundException _))
             (try (require 'leiningen.compile)
                  [(resolve 'leiningen.compile/eval-in-project)
                    [(make-subproject-lein1 project crossover-path builds)
                     form
                     nil
                     nil
                     requires]]
                  (catch java.io.FileNotFoundException _)))]
    (apply eip args)))

(defn prepping? []
 (try
   (require 'leiningen.core.eval)
   (if-let [prepping-var (resolve 'leiningen.core.eval/*prepping?*)]
     (deref prepping-var)
     false)
   (catch java.io.FileNotFoundException _
     false)))
