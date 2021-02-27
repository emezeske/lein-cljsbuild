(ns leiningen.cljsbuild-test
  (:require [cljsbuild.util :as cljsbuild.util]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [leiningen.cljsbuild :as cljsbuild]
            [leiningen.cljsbuild.config :as config]
            [leiningen.cljsbuild.subproject :as subproject]
            [leiningen.core.eval :as leval]
            [leiningen.core.main :as lmain]))

(def repl-listen-port 10000)
(def repl-launch-command-id "launch-id")
(def repl-launch-command ["launch-me"])
(def test-command-id "test-id")
(def test-command ["test-me"])

(def build-id "build-id")
(def source-paths ["source-path-a" "source-path-b"])
(def incremental? false)
(def assert? false)
(def watch? false)

(def compiler
  {:output-to "target/output-to"
   :output-dir "target/output-dir"
   :warnings false
   :optimizations :advanced
   :pretty-print false})

(def builds
  (list
    {:id build-id
     :source-paths source-paths
     :jar true
     :notify-command ["notify"]
     :incremental incremental?
     :assert assert?
     :compiler compiler}))

(def parsed-builds
  (map config/set-build-global-dirs
    (map config/parse-notify-command builds)))

(def parsed-compiler
  (:compiler (first parsed-builds)))

(def project-dir "/project")
(def checkouts-dir (io/file project-dir "checkouts"))

(def project
  {:dependencies [['org.clojure/clojure "1.5.1"]
                  ['org.clojure/clojurescript "0.0-3211"]]
   :root project-dir
   :cljsbuild
   {:repl-listen-port repl-listen-port
    :repl-launch-commands {repl-launch-command-id repl-launch-command}
    :test-commands {test-command-id test-command}
    :builds builds}})

(defn hook-success [& args]
  nil)

(defn hook-failure [& args]
  (throw (Exception. "Dummy hook failed.")))

(defn eval-locally [_ body _]
  (eval body))

(deftest cljsbuild
  (with-redefs [leval/eval-in-project eval-locally]
    (binding [cljsbuild/*suppress-exit?* true
              lmain/*exit-process?* false]

      (testing "fail when no arguments present"
        (is (thrown? Exception (cljsbuild/cljsbuild project))))

      (testing "once/auto call eval-in project with the right args"
        (with-redefs [leval/eval-in-project
                      (fn [config _ _]
                        (is (= config (subproject/make-subproject project parsed-builds)))
                        nil)]
          (doseq [command ["once" "auto"]]
            (is (nil? (cljsbuild/cljsbuild project command)))))))))
