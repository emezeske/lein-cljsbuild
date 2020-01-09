(ns leiningen.cljsbuild.config-test
  (:require [clojure.test :refer [deftest is testing]]
            [leiningen.cljsbuild.config :as config]))

(deftest backwards-compat
  (let [config-0-0-x-early {:source-path "a"
                            :compiler {:output-to "hello.js"}}
        config-0-0-x-late [{:source-path "a"
                            :compiler {:output-to "hello.js"}}]
        config-backwards {:builds
                           [{:source-paths ["a"]
                             :compiler {:output-to "hello.js"}}]}]
    (is (= (config/backwards-compat config-0-0-x-early) config-backwards))
    (is (= (config/backwards-compat config-0-0-x-late) config-backwards))))

(deftest convert-builds-map
  (let [config-vec {:builds [{:id "a"} {:id "b"}]}
        config-map {:builds {:a {} :b {}}}]
    (is (= (config/convert-builds-map config-vec) config-vec))
    (is (= (config/convert-builds-map config-map) config-vec))))

(deftest parse-shell-command
  (is (= (config/parse-shell-command []) {:shell []}))
  (is (= (config/parse-shell-command ["a"]) {:shell ["a"]}))
  (is (= (config/parse-shell-command ["a" "b" "c" :x 1 :y 2]) {:shell ["a" "b" "c"] :x 1 :y 2})))

(def target-path "target")

(def config-in
  {:repl-launch-commands {:a ["a"]}
   :repl-listen-port 10000
   :test-commands {:b ["b"]}
   :builds
     '({:source-paths ["f"]
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

(defn- get-build [config]
  (first (:builds config)))

(defn- default-build-option [config build option]
  (config/set-default-options target-path
    (assoc config :builds (list (dissoc build option)))))

(deftest set-default-options
  (testing "custom settings are not overwritten by defaults"
    (is (= (config/set-default-options target-path config-in) config-in)))
  (testing "missing global settings have defaults provided"
    (doseq [option (keys config-in)]
      (is (contains? (config/set-default-options target-path (dissoc config-in option)) option))))
  (testing "missing build settings have defaults provided"
    (let [build (get-build config-in)]
      (doseq [build-option (keys build)]
        (let [defaulted (default-build-option config-in build build-option)]
          (is (contains? (get-build defaulted) build-option)))))))

(def config-out (config/set-compiler-global-dirs config-in))

(deftest extract-options
  (is (= (config/extract-options {:cljsbuild config-in}) config-out)))

(def modules-config
  {:compiler {:externs []
              :libs []
              :modules {:front {:output-to "lib/assets/cljs/cljs-front.js"
                                :entries #{"hb.front.core"}}
                        :extranet {:output-to "lib/assets/cljs/cljs-extranet.js"
                                   :entries #{"hb.core"}}}}
   :assert true
   :incremental true
   :jar false
   :notify-command nil
   :source-paths ["src-cljs"]})

(deftest set-default-build-options
  (testing "don't set :output-to option when :modules option is provided"
    (is (= (config/set-default-build-options target-path modules-config) modules-config))))
