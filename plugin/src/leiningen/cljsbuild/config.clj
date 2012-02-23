(ns leiningen.cljsbuild.config
  "Utilities for parsing the cljsbuild config."
  (:require
    [clojure.pprint :as pprint]))

(def compiler-output-dir-base ".lein-cljsbuild-compiler-")

(def default-global-options
  {:repl-launch-commands {}
   :repl-listen-port 9000
   :test-commands {}
   :crossover-path "crossover-cljs"
   :crossover-jar false
   :crossovers []})

(def default-compiler-options
  {:output-to "main.js"
   :optimizations :whitespace
   :pretty-print true})

(def default-build-options
  {:source-path "src-cljs"
   :jar false
   :compiler default-compiler-options})

(defn- backwards-compat-builds [options]
  (cond
    (and (map? options) (some #{:compiler :source-path} (keys options)))
      {:builds [options]}
    (vector? options)
      {:builds options}
    :else
      options))

(defn- backwards-compat-crossovers [{:keys [builds crossovers] :as options}]
  (let [all-crossovers (->> builds
                         (mapcat :crossovers)
                         (concat crossovers)
                         (distinct)
                         (vec))
        no-crossovers (assoc options
                        :builds (vec (map #(dissoc % :crossovers) builds)))]
    (if (empty? all-crossovers)
      no-crossovers
      (assoc no-crossovers
        :crossovers all-crossovers))))

(defn- backwards-compat [options]
  (-> options
    backwards-compat-builds
    backwards-compat-crossovers))

(defn- warn-deprecated [options]
  (letfn [(delim [] (println (apply str (take 80  (repeat "-")))))]
    (delim)
    (println
      (str
        "WARNING: your :cljsbuild configuration is in a deprecated format.  It has been\n"
        "automatically converted it to the new format, which will be printed below.\n"
        "It is recommended that you update your :cljsbuild configuration ASAP."))
    (delim)
    (println ":cljsbuild")
    (pprint/pprint options)
    (delim)
    (println
      (str
        "See https://github.com/emezeske/lein-cljsbuild/blob/master/README.md\n"
        "for details on the new format."))
    (delim)
    options))

(declare deep-merge-item)

(defn- deep-merge [& ms]
  (apply merge-with deep-merge-item ms))

(defn- deep-merge-item [a b]
  (if (and (map? a) (map? b))
    (deep-merge a b)
    b))

(defn- set-default-build-options [options]
  (deep-merge default-build-options options))

(defn- set-default-output-dirs [options]
  (let [output-dir-key [:compiler :output-dir]
        builds
         (for [[build counter] (map vector (:builds options) (range))]
           (if (get-in build output-dir-key)
             build
             (assoc-in build output-dir-key
               (str compiler-output-dir-base counter))))]
    (if (apply distinct? (map #(get-in % output-dir-key) builds))
      (assoc options :builds builds)
      (throw (Exception. (str "All " output-dir-key " options must be distinct."))))))

(defn- set-default-options [options]
  (set-default-output-dirs
    (deep-merge default-global-options
      (assoc options :builds
        (map set-default-build-options (:builds options))))))

(defn- normalize-options
  "Sets default options and accounts for backwards compatibility."
  [options]
  (let [compat (backwards-compat options)]
    (when (not= options compat)
      (warn-deprecated compat))
    (set-default-options compat)))

(defn extract-options
  "Given a project, returns a seq of cljsbuild option maps."
  [project]
  (when (nil? (:cljsbuild project))
    (println "WARNING: no :cljsbuild entry found in project definition."))
  (let [raw-options (:cljsbuild project)]
    (normalize-options raw-options)))
