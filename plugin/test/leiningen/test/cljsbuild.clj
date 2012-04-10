(ns leiningen.test.cljsbuild
  (:use
    leiningen.cljsbuild
    midje.sweet)
  (:require
    [leiningen.cljsbuild.config :as config]
    [leiningen.cljsbuild.subproject :as subproject]))

; This crazy in-ns business is to mock out the namespaces that would normally
; be required by eval-in-project.  It would be preferable to just :require them,
; but they require Clojure 1.3 and we're running in Leningen 1 which uses 1.2.
; TODO: Once Leiningen 2 is released, get rid of this.

(in-ns 'cljsbuild.crossover)
(clojure.core/use 'midje.sweet)
(unfinished copy-crossovers)

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

(in-ns 'leiningen.test.cljsbuild)

(defmacro quietly [& body]
  `(binding [*out* (new java.io.StringWriter)]
     ~@body))

(defn dummy-eval-in-project [_ _ _ body _]
  (eval body))

(defn dummy-in-threads [f s]
  (doseq [i s] (f i)))

(defn dummy-once-every-bg [_ _ _]
  ; TODO
  nil)

; TODO: Could just define these in the namespaces directly?
(defmacro with-dummies [& forms]
  `(binding [subproject/eval-in-project dummy-eval-in-project
             cljsbuild.util/in-threads dummy-in-threads
             cljsbuild.util/once-every-bg dummy-once-every-bg]
     ~@forms))

(def crossover-path "c")
(def crossovers [])

(def build-id "1")
(def source-path "a")
(def warn-on-undeclared false)

(def compiler
  {:output-to "h"
   :output-dir "i"
   :optimizations :advanced
   :pretty-print false})

(def builds
  (list
    {:id build-id
     :source-path source-path
     :jar true
     :notify-command ["g"]
     :warn-on-undeclared warn-on-undeclared
     :compiler compiler}))

(def parsed-builds
  (map config/parse-notify-command builds))

(def project
 {:dependencies [['org.clojure/clojure "1.3.0"]]
  :cljsbuild
   {:repl-launch-commands {:a ["a"]}
    :repl-listen-port 10000
    :test-commands {:b ["b"]}
    :crossover-path crossover-path
    :crossover-jar true
    :crossovers crossovers
    :builds builds}})

(fact "fail when no arguments present"
  (quietly (cljsbuild project)) => 1)

(fact "once/auto call eval-in-project with the right args"
  (doseq [command ["once" "auto"]]
    (quietly (cljsbuild project command)) => 0
    (provided
      (subproject/eval-in-project
        project
        crossover-path
        parsed-builds
        anything
        anything) => 0 :times 1)) )

(fact "once/auto tasks call the compiler correctly"
  (doseq [[command watch?] {"once" false "auto" true}
          extra-args [[] [build-id]]]
    (with-dummies
      (quietly
        (apply cljsbuild project command extra-args))) => 0
    (provided
      (cljsbuild.crossover/copy-crossovers
        crossover-path
        crossovers) => nil :times 1
      (cljsbuild.compiler/run-compiler
        source-path
        crossover-path
        compiler
        anything
        warn-on-undeclared
        watch?) => nil :times 1)))

(fact "bad build IDs are detected"
  (with-dummies
    (quietly
      (cljsbuild project "once" "wrong-build-id"))) => (throws Exception))

(fact "clean calls cleanup-files"
  (with-dummies
    (quietly
      (cljsbuild project "clean"))) => 0
  (provided
    (cljsbuild.clean/cleanup-files compiler) => nil :times 1))

(fact "test calls run-tests"
  (with-dummies
    (quietly
      (cljsbuild project "test"))) => 0
  (provided
    (cljsbuild.crossover/copy-crossovers
      crossover-path
      crossovers) => nil :times 1
    (cljsbuild.compiler/run-compiler
      source-path
      crossover-path
      compiler
      anything
      warn-on-undeclared
      false) => nil :times 1
    ; TODO: Actually check the run-tests argument.
    ; TODO: Check that test ID selectorsw work.
    (cljsbuild.test/run-tests anything) => nil :times 1))

; TODO: Test REPLs, hooks.
