(ns leiningen.test.cljsbuild.config
  (:use
    leiningen.cljsbuild.config
    midje.sweet))

(fact
  (let [config-0-0-x-early {:source-path "a"
                            :compiler {:output-to "hello.js"}}
        config-0-0-x-late [{:source-path "a"
                            :compiler {:output-to "hello.js"}}]
        config-backwards {:builds
                           [{:source-path "a"
                             :compiler {:output-to "hello.js"}}]}]
    (backwards-compat config-0-0-x-early) => config-backwards
    (backwards-compat config-0-0-x-late) => config-backwards))

(fact
  (let [config-vec {:builds [{:id "a"} {:id "b"}]}
        config-map {:builds {:a {} :b {}}}]
    (convert-builds-map config-vec) => config-vec
    (convert-builds-map config-map) => config-vec))

(fact
  (parse-shell-command []) => {:shell []}
  (parse-shell-command ["a"]) => {:shell ["a"]}
  (parse-shell-command ["a" "b" "c" :x 1 :y 2]) => {:shell ["a" "b" "c"] :x 1 :y 2})

(def target-path "target")

(def config-in
  {:repl-launch-commands {:a ["a"]}
   :repl-listen-port 10000
   :test-commands {:b ["b"]}
   :crossover-path "c"
   :crossover-jar true
   :crossovers ["d" "e"]
   :builds
     '({:source-path "f"
        :jar true
        :notify-command ["g"]
        :incremental false
        :assert false
        :compiler
          {:output-to "h"
           :output-dir "i"
           :warnings false
           :libs ["j"]
           :externs ["k"]
           :optimizations :advanced
           :pretty-print false}})})

(fact "custom settings are not overwritten by defaults"
  (set-default-options target-path config-in) => config-in)

(fact "missing settings have defaults provided"
  (doseq [option (keys config-in)]
    (set-default-options target-path (dissoc config-in option)) => (contains {option anything})))

(defn- get-build [config]
  (first (:builds config)))

(defn- default-build-option [config build option]
  (set-default-options target-path
    (assoc config :builds
      (list (dissoc build option)))))

(fact "missing build settings have defaults provided"
  (let [build (get-build config-in)]
    (doseq [build-option (keys build)]
      (let [defaulted (default-build-option config-in build build-option)]
        (get-build defaulted) => (contains {build-option anything})))))

(defn- get-compiler [config]
  (:compiler (get-build config)))

(defn- default-compiler-option [config compiler option]
  (set-default-options target-path
    (assoc config :builds
      (list
        (assoc compiler :compiler
          (dissoc compiler option))))))

(fact "missing compiler settings have defaults provided"
  (let [compiler (:compiler (get-build config-in))]
    (doseq [compiler-option (keys compiler)]
      (let [defaulted (default-compiler-option config-in compiler compiler-option)]
        (get-compiler defaulted) => (contains {compiler-option anything})))))

(def config-out (set-compiler-global-dirs config-in))

(fact
  (extract-options {:cljsbuild config-in}) => config-out)
