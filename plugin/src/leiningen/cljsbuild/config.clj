(ns leiningen.cljsbuild.config
  "Utilities for parsing the cljsbuild config."
  (:require
    [clojure.pprint :as pprint]))

(def compiler-output-dir-base ".lein-cljsbuild-compiler-")
(def compiler-global-dirs
  {:libs "closure-js/libs"
   :externs "closure-js/externs"})

(def default-global-options
  {:repl-launch-commands {}
   :repl-listen-port 9000
   :test-commands {}
   :crossover-path ".crossover-cljs"
   :crossover-jar false
   :crossovers []})

(def default-compiler-options
  {:output-to "main.js"
   :output-dir "tmp/cljsc-out"
   :optimizations :whitespace
   :warnings true
   :externs []
   :libs []
   :pretty-print true})

(def default-build-options
  {:source-path "src-cljs"
   :jar false
   :notify-command nil
   :incremental true
   :assert true
   :compiler default-compiler-options})

(defn convert-builds-map [options]
  (update-in options [:builds]
             #(if (map? %)
                (for [[id build] %]
                  (assoc build :id (name id)))
                %)))

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

(defn backwards-compat [options]
  (-> options
    backwards-compat-builds
    backwards-compat-crossovers))

(defn- warn-deprecated [options]
  (letfn [(delim [] (println (apply str (take 80 (repeat "-")))))]
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
    (if (or (empty? builds)
            (apply distinct? (map #(get-in % output-dir-key) builds)))
      (assoc options :builds builds)
      (throw (Exception. (str "All " output-dir-key " options must be distinct."))))))

(defn set-default-options [options]
  (set-default-output-dirs
    (deep-merge default-global-options
      (assoc options :builds
        (map set-default-build-options (:builds options))))))

(defn set-build-global-dirs [build]
  (reduce (fn [build [k v]] (update-in build [:compiler k] conj v))
          build
          compiler-global-dirs))

(defn set-compiler-global-dirs [options]
  (assoc options :builds
    (map set-build-global-dirs (:builds options))))

(defn- normalize-options
  "Sets default options and accounts for backwards compatibility."
  [options]
  (let [options (convert-builds-map options)
        compat (backwards-compat options)]
    (when (not= options compat)
      (warn-deprecated compat))
    (-> compat
      set-default-options
      set-compiler-global-dirs)))

(defn parse-shell-command [raw]
  (let [[shell tail] (split-with (comp not keyword?) raw)
        options (apply hash-map tail)]
    (merge {:shell shell} options)))

(defn parse-notify-command [build]
  (assoc build :parsed-notify-command
    (parse-shell-command (:notify-command build))))

(defn warn-unsupported-warn-on-undeclared [build]
  (when (contains? build :warn-on-undeclared)
    (println "WARNING: the :warn-on-undeclared option is no longer available.")
    (println "Set \":warnings true\" in your :compiler options instead.")))

(defn warn-unsupported-notify-command [build]
  (when (or (first (filter #(= "%" %) (:shell (:parsed-notify-command build))))
            (:beep (:parsed-notify-command build)))
    (println "WARNING: the :notify-command option no longer accepts the \"%\" or :beep options.")
    (println "See https://github.com/emezeske/lein-cljsbuild/blob/master/doc/RELEASE-NOTES.md for details.")))

(defn extract-options
  "Given a project, returns a seq of cljsbuild option maps."
  [project]
  (when (nil? (:cljsbuild project))
    (println "WARNING: no :cljsbuild entry found in project definition."))
  (let [raw-options (:cljsbuild project)]
    (normalize-options raw-options)))
