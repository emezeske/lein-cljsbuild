(ns leiningen.cljsbuild.subproject
  "Utilities for running cljsbuild in a subproject"
  (:import
    org.apache.maven.artifact.versioning.DefaultArtifactVersion))

(def cljsbuild-dependencies '[[cljsbuild "0.1.7"]])
(def required-clojure-version "1.3.0")

(defn check-clojure-version [project-dependencies]
  (let [clojure-dependency ('org.clojure/clojure project-dependencies)]
    (when (nil? clojure-dependency)
      (throw
        (Exception. "lein-cljsbuild requires your project to specify which Clojure version it uses ")))
    (let [version-str (first clojure-dependency)
          version (DefaultArtifactVersion. version-str)
          required (DefaultArtifactVersion. required-clojure-version)]
      (when (< (.compareTo version required) 0)
        (throw
          (Exception.
            (str "lein-cljsbuild requires your project to use Clojure version >= " required-clojure-version)))))))

(defn dependency-map [dependency-vec]
  (into {} (map (juxt first rest) dependency-vec)))

(defn merge-dependencies [project-dependencies]
  (let [project (dependency-map project-dependencies)
        cljsbuild (dependency-map cljsbuild-dependencies)]
    (check-clojure-version project)
    (map (fn [[k v]] (vec (cons k v)))
      (merge project cljsbuild))))

(defn make-subproject [project crossover-path builds]
  {:local-repo-classpath true
   :dependencies (merge-dependencies (:dependencies project))
   :dev-dependencies (:dev-dependencies project)
   :repositories (:repositories project)})

(defn make-subproject-lein1 [project crossover-path builds]
  (merge (make-subproject project crossover-path builds)
    {:source-path (:source-path project)
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
