(ns leiningen.test.cljsbuild
  (:use
    leiningen.cljsbuild
    midje.sweet)
  (:require
    [leiningen.trampoline :as ltrampoline]
    [leiningen.cljsbuild.config :as config]
    [leiningen.cljsbuild.jar :as jar]
    [leiningen.cljsbuild.subproject :as subproject]
    [leiningen.core.main :as lmain]
    [leiningen.core.eval :as leval]
    cljsbuild.crossover
    cljsbuild.util
    cljsbuild.compiler
    cljsbuild.test
    cljsbuild.repl.listen
    cljsbuild.repl.rhino))

(def repl-listen-port 10000)
(def repl-launch-command-id "launch-id")
(def repl-launch-command ["launch-me"])
(def test-command-id "test-id")
(def test-command ["test-me"])
(def crossover-path "target/crossover-path")
(def crossovers ['test.crossover])
(def crossover-macros [{:absolute "/root/stuff/test/crossover.clj"
                        :classpath "test/crossover.clj"}])

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

(def project
 {:dependencies [['org.clojure/clojure "1.5.1"]]
  :cljsbuild
   {:repl-listen-port repl-listen-port
    :repl-launch-commands {repl-launch-command-id repl-launch-command}
    :test-commands {test-command-id test-command}
    :crossover-path crossover-path
    :crossover-jar true
    :crossovers crossovers
    :builds builds}})

(defn hook-success [& args]
  nil)

(defn hook-failure [& args]
  (throw (Exception. "Dummy hook failed.")))

(defn eval-locally [_ body _]
  (eval body))

(background
  (around
    :facts (with-redefs [leval/eval-in-project eval-locally
                         cljsbuild.util/once-every-bg (fn [_# _# _#] nil)]
             (binding [*suppress-exit?* true
                       lmain/*exit-process?* false]
               ?form))))

(fact "fail when no arguments present"
  (cljsbuild project) => (throws Exception))

(fact "once/auto call eval-in-project with the right args"
  (doseq [command ["once" "auto"]]
    (cljsbuild project command) => nil
    (provided
      (leval/eval-in-project
        (subproject/make-subproject project crossover-path parsed-builds)
        anything
        anything) => nil :times 1)) )

(fact "once task calls the compiler correctly"
  (doseq [extra-args [[] [build-id]]]
    (apply cljsbuild project "once" extra-args) => nil
    (provided
      (cljsbuild.crossover/crossover-macro-paths
        crossovers) => crossover-macros :times 1
      (cljsbuild.crossover/copy-crossovers
        crossover-path
        crossovers) => nil :times 1
      (cljsbuild.compiler/run-compiler
        source-paths
        crossover-path
        crossover-macros
        parsed-compiler
        anything
        incremental?
        assert?
        {}
        watch?) => nil :times 1)))

(fact "bad build IDs are detected"
  (cljsbuild project "once" "wrong-build-id") => (throws Exception))

(fact "compile-hook calls through to the compiler when task succeeds"
  (compile-hook hook-success project) => nil
  (provided
    (cljsbuild.crossover/crossover-macro-paths
      crossovers) => crossover-macros :times 1
    (cljsbuild.crossover/copy-crossovers
      crossover-path
      crossovers) => nil :times 1
    (cljsbuild.compiler/run-compiler
      source-paths
      crossover-path
      crossover-macros
      parsed-compiler
      anything
      incremental?
      assert?
      {}
      watch?) => nil :times 1))

(fact "compile-hook does not call through to the compiler when task fails"
  (compile-hook hook-failure project) => (throws Exception)
  (provided
    (cljsbuild.crossover/crossover-macro-paths
      anything) => nil :times 0
    (cljsbuild.crossover/copy-crossovers
      anything
      anything) => nil :times 0
    (cljsbuild.compiler/run-compiler
      anything
      anything
      anything
      anything
      anything
      anything
      anything
      anything
      anything) => nil :times 0))

; NOTE: This let has to be outside the fact, because against-background does not
;       like being directly inside a let.
(let [parsed-commands [[test-command-id (config/parse-shell-command test-command)]]]
  (fact "tests work correctly"
      (cljsbuild project "test") => nil
      (cljsbuild project "test" test-command-id) => nil
      (test-hook hook-success project) => nil
      (test-hook hook-failure project) => (throws Exception)
      (against-background
        (cljsbuild.crossover/crossover-macro-paths
          crossovers) => crossover-macros :times 0
        (cljsbuild.crossover/copy-crossovers
          crossover-path
          crossovers) => nil :times 1
        (cljsbuild.compiler/run-compiler
          source-paths
          crossover-path
          crossover-macros
          parsed-compiler
          anything
          incremental?
          assert?
          {}
          watch?) => nil :times 1
        (cljsbuild.test/run-tests parsed-commands) => nil :times 1)))

(against-background [(around :checks
                       (binding [ltrampoline/*trampoline?* true]
                         ?form))]

  (fact "repl-listen calls run-repl-listen"
    (cljsbuild project "repl-listen") => nil
    (provided
      (cljsbuild.repl.listen/run-repl-listen repl-listen-port anything) => nil :times 1))

  (fact "repl-launch with no ID fails"
    (cljsbuild project "repl-launch") => (throws Exception))

  (fact "repl-launch with bad ID fails"
    (cljsbuild project "repl-launch" "wrong-repl-launch-id") => (throws Exception))

  (fact "repl-launch calls run-repl-launch"
    (let [parsed-command (config/parse-shell-command repl-launch-command)]
      (cljsbuild project "repl-launch" repl-launch-command-id) => nil
      (provided
        (cljsbuild.repl.listen/run-repl-launch
          repl-listen-port
          anything
          parsed-command) => nil :times 1)))

  (fact "repl-rhino calls run-repl-rhino"
    (cljsbuild project "repl-rhino") => nil
    (provided
      (cljsbuild.repl.rhino/run-repl-rhino) => nil :times 1)))

(unfinished jar-task)

(fact "jar-hook calls through to get-filespecs"
  (let [out-file "out"
        input-filespecs [{:type :bytes :path "/i/j" :bytes "fake-1"}]
        project-filespecs [{:type :bytes :path "/a/b" :bytes "fake-2"}]
        all-filespecs (concat input-filespecs project-filespecs)]
    (jar-hook jar-task project out-file input-filespecs) => nil
    (provided
      (jar-task project out-file all-filespecs) => nil
      (jar/get-filespecs project) => project-filespecs)))
