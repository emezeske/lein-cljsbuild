(ns leiningen.test.cljsbuild.config
  (:use
    leiningen.cljsbuild.config
    clojure.test))

(deftest test-backwards-compatibility
  (let [config-0-0-x-early {:source-path "a"
                            :compiler {:output-to "hello.js"}}
        config-0-0-x-late [{:source-path "a"
                            :compiler {:output-to "hello.js"}}]
        config-backwards {:builds
                           [{:source-path "a"
                             :compiler {:output-to "hello.js"}}]}]
    (is (= config-backwards (backwards-compat config-0-0-x-early))) 
    (is (= config-backwards (backwards-compat config-0-0-x-late)))))

(deftest test-ids
  (let [config-vec {:builds [{:id "a"} {:id "b"}]}
        config-map {:builds {:a {} :b {}}}]
    (is (= config-vec (convert-builds-map config-vec)))
    (is (= config-vec (convert-builds-map config-map)))))

(deftest test-shell-command
  (is (= {:shell []}
         (parse-shell-command [])))
  (is (= {:shell ["a"]}
         (parse-shell-command ["a"])))
  (is (= {:shell ["a" "b" "c"] :x 1 :y 2}
         (parse-shell-command ["a" "b" "c" :x 1 :y 2]))))

(deftest test-default-options
  (let [config-in {:repl-launch-commands {:a ["a"]}
                   :repl-listen-port 10000
                   :test-commands {:b ["b"]}
                   :crossover-path "c"
                   :crossover-jar true
                   :crossovers ["d" "e"]
                   :builds
                     '({:source-path "f"
                        :jar true
                        :notify-command ["g"]
                        :warn-on-undeclared false
                        :compiler
                          {:output-to "h"
                           :output-dir "i"
                           :optimizations :advanced
                           :pretty-print false}})}]
    ; Ensure that none of our custom settings are overwritten by defaults.
    (is (= config-in (set-default-options config-in)))
    ; Ensure that if any custom setting is missing, a default is provided.
    (doseq [option (keys config-in)]
      (is (contains? (set-default-options (dissoc config-in option)) option)))
    (let [build (first (:builds config-in))]
      (doseq [build-option (keys build)]
        (let [defaulted (set-default-options
                          (assoc config-in :builds 
                            (list (dissoc build build-option))))]
        (is (contains? (first (:builds defaulted)) build-option))))
      (let [compiler (:compiler build)]
        (doseq [compiler-option (keys compiler)]
          (let [defaulted (set-default-options
                            (assoc config-in :builds
                              (list
                                (assoc build :compiler
                                  (dissoc compiler :compiler-option)))))]
          (is (contains? (:compiler (first (:builds defaulted))) compiler-option))))))))
