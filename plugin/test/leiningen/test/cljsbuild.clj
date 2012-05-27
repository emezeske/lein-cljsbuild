(ns leiningen.test.cljsbuild
  (:use
    leiningen.cljsbuild
    midje.sweet)
  (:require
    [leiningen.trampoline :as ltrampoline]
    [leiningen.cljsbuild.config :as config]
    [leiningen.cljsbuild.jar :as jar]
    [leiningen.cljsbuild.subproject :as subproject]))

; This crazy in-ns business is to mock out the namespaces that would normally
; be required by eval-in-project.  It would be preferable to just :require them,
; but they require Clojure 1.3 and we're running in Leningen 1 which uses 1.2.
; TODO: Once Leiningen 2 is released, get rid of this.

(in-ns 'cljsbuild.crossover)
(clojure.core/use 'midje.sweet)
(unfinished copy-crossovers)
(unfinished crossover-macro-paths)

(in-ns 'cljsbuild.util)
(clojure.core/use 'midje.sweet)
(unfinished once-every-bg)
(unfinished in-threads)

(in-ns 'cljsbuild.compiler)
(clojure.core/use 'midje.sweet)
(unfinished run-compiler)

(in-ns 'cljsbuild.clean)
(clojure.core/use 'midje.sweet)
(unfinished cleanup-files)

(in-ns 'cljsbuild.test)
(clojure.core/use 'midje.sweet)
(unfinished run-tests)

(in-ns 'cljsbuild.repl.listen)
(clojure.core/use 'midje.sweet)
(unfinished run-repl-listen)
(unfinished run-repl-launch)

(in-ns 'cljsbuild.repl.rhino)
(clojure.core/use 'midje.sweet)
(unfinished run-repl-rhino)

(in-ns 'leiningen.test.cljsbuild)

(def repl-listen-port 10000)
(def repl-launch-command-id "launch-id")
(def repl-launch-command ["launch-me"])
(def test-command-id "test-id")
(def test-command ["test-me"])
(def crossover-path "crossover-path")
(def crossovers ['test.crossover])
(def crossover-macros [{:absolute "/root/stuff/test/crossover.clj"
                        :classpath "test/crossover.clj"}])

(def build-id "build-id")
(def source-path "source-path")
(def warn-on-undeclared false)
(def incremental false)

(def compiler
  {:output-to "output-to"
   :output-dir "output-dir"
   :optimizations :advanced
   :pretty-print false})

(def builds
  (list
    {:id build-id
     :source-path source-path
     :jar true
     :notify-command ["notify"]
     :warn-on-undeclared warn-on-undeclared
     :incremental incremental
     :compiler compiler}))

(def parsed-builds
  (map config/set-build-global-dirs
    (map config/parse-notify-command builds)))

(def parsed-compiler
  (:compiler (first parsed-builds)))

(def project
 {:dependencies [['org.clojure/clojure "1.3.0"]]
  :cljsbuild
   {:repl-listen-port repl-listen-port
    :repl-launch-commands {repl-launch-command-id repl-launch-command}
    :test-commands {test-command-id test-command}
    :crossover-path crossover-path
    :crossover-jar true
    :crossovers crossovers
    :builds builds}})

(defn hook-success [& args]
  0)

(defn hook-failure [& args]
  1)

(defn eval-locally [_ _ _ body _]
  (eval body))

(defmacro with-compiler-bindings [& forms]
  `(binding [subproject/eval-in-project eval-locally
             cljsbuild.util/in-threads (fn [f# s#] (doseq [i# s#] (f# i#)))
             cljsbuild.util/once-every-bg (fn [_# _# _#] nil)]
     ~@forms))

(fact "fail when no arguments present"
  (cljsbuild project) => 1)

(fact "once/auto call eval-in-project with the right args"
  (doseq [command ["once" "auto"]]
    (cljsbuild project command) => 0
    (provided
      (subproject/eval-in-project
        project
        crossover-path
        parsed-builds
        anything
        anything) => 0 :times 1)) )

(fact "once task calls the compiler correctly"
  (doseq [extra-args [[] [build-id]]]
    (with-compiler-bindings
      (apply cljsbuild project "once" extra-args)) => 0
    (provided
      (cljsbuild.crossover/crossover-macro-paths
        crossovers) => crossover-macros :times 1
      (cljsbuild.crossover/copy-crossovers
        crossover-path
        crossovers) => nil :times 1
      (cljsbuild.compiler/run-compiler
        source-path
        crossover-path
        crossover-macros
        parsed-compiler
        anything
        warn-on-undeclared
        incremental
        {}) => nil :times 1)))

(fact "bad build IDs are detected"
  (with-compiler-bindings
    (cljsbuild project "once" "wrong-build-id")) => (throws Exception))

(fact "clean calls cleanup-files"
  (with-compiler-bindings
    (cljsbuild project "clean")) => 0
  (with-compiler-bindings
    (clean-hook hook-success project)) => 0
  (with-compiler-bindings
    (clean-hook hook-failure project)) => 0
  (against-background
    (cljsbuild.clean/cleanup-files parsed-compiler) => nil :times 1))

(fact "compile-hook calls through to the compiler when task succeeds"
  (with-compiler-bindings
    (compile-hook hook-success project)) => 0
  (provided
    (cljsbuild.crossover/crossover-macro-paths
      crossovers) => crossover-macros :times 1
    (cljsbuild.crossover/copy-crossovers
      crossover-path
      crossovers) => nil :times 1
    (cljsbuild.compiler/run-compiler
      source-path
      crossover-path
      crossover-macros
      parsed-compiler
      anything
      warn-on-undeclared
      incremental
      {}) => nil :times 1))

(fact "compile-hook does not call through to the compiler when task fails"
  (with-compiler-bindings
    (compile-hook hook-failure project)) => 1
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
      anything) => nil :times 0))

; NOTE: This let has to be outside the fact, because against-background does not
;       like being directly inside a let.
(let [parsed-commands [(config/parse-shell-command test-command)]]
  (fact "tests work correctly"
      (with-compiler-bindings
        (cljsbuild project "test")) => 0
      (with-compiler-bindings
        (cljsbuild project "test" test-command-id)) => 0
      (with-compiler-bindings
        (test-hook hook-success project)) => 0
      (with-compiler-bindings
        (test-hook hook-failure project)) => 1
      (against-background
        (cljsbuild.crossover/crossover-macro-paths
          crossovers) => crossover-macros :times 0
        (cljsbuild.crossover/copy-crossovers
          crossover-path
          crossovers) => nil :times 1
        (cljsbuild.compiler/run-compiler
          source-path
          crossover-path
          crossover-macros
          parsed-compiler
          anything
          warn-on-undeclared
          incremental
          {}) => nil :times 1
        (cljsbuild.test/run-tests parsed-commands) => 0 :times 1)))

(defmacro with-repl-bindings [& forms]
  `(binding [subproject/eval-in-project eval-locally
             ltrampoline/*trampoline?* true]
     ~@forms))

(fact "repl-listen calls run-repl-listen"
  (with-repl-bindings
    (cljsbuild project "repl-listen")) => 0
  (provided
    (cljsbuild.repl.listen/run-repl-listen repl-listen-port anything) => nil :times 1))

(fact "repl-launch with no ID fails"
  (with-repl-bindings
    (cljsbuild project "repl-launch")) => (throws Exception))

(fact "repl-launch with bad ID fails"
  (with-repl-bindings
    (cljsbuild project "repl-launch" "wrong-repl-launch-id")) => (throws Exception))

(fact "repl-launch calls run-repl-launch"
  (let [parsed-command (config/parse-shell-command repl-launch-command)]
    (with-repl-bindings
      (cljsbuild project "repl-launch" repl-launch-command-id)) => 0
    (provided
      (cljsbuild.repl.listen/run-repl-launch
        repl-listen-port
        anything
        parsed-command) => nil :times 1)))

(fact "repl-rhino calls run-repl-rhino"
  (with-repl-bindings
    (cljsbuild project "repl-rhino")) => 0
  (provided
    (cljsbuild.repl.rhino/run-repl-rhino) => nil :times 1))

(unfinished jar-task)

(fact "jar-hook calls through to get-filespecs"
  (let [out-file "out"
        input-filespecs [{:type :bytes :path "/i/j" :bytes "fake-1"}]
        project-filespecs [{:type :bytes :path "/a/b" :bytes "fake-2"}]
        all-filespecs (concat input-filespecs project-filespecs)]
    (jar-hook jar-task project out-file input-filespecs) => 0
    (provided
      (jar-task project out-file all-filespecs) => 0
      (jar/get-filespecs project) => project-filespecs)))
