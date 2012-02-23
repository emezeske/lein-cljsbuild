(ns leiningen.cljsbuild
  "Compile ClojureScript source into a JavaScript file."
  (:refer-clojure :exclude [test])
  (:require
    [fs.core :as fs]
    [leiningen.clean :as lclean]
    [leiningen.cljsbuild.config :as config]
    [leiningen.cljsbuild.jar :as jar]
    [leiningen.cljsbuild.subproject :as subproject]
    [leiningen.compile :as lcompile]
    [leiningen.help :as lhelp]
    [leiningen.jar :as ljar]
    [leiningen.test :as ltest]
    [robert.hooke :as hooke]))

(def repl-output-path ".lein-cljsbuild-repl")

(def exit-success 0)
(def exit-failure 1)

(defn- run-local-project [project crossover-path builds requires form]
  (subproject/eval-in-project project crossover-path builds
    `(do
      ~form
      (shutdown-agents))
    requires)
  exit-success)

(defn- run-compiler [project {:keys [crossover-path crossovers builds]} watch?]
  (println "Compiling ClojureScript.")
  ; If crossover-path does not exist before eval-in-project is called,
  ; the files it contains won't be classloadable, for some reason.
  (when (not-empty crossovers)
    (fs/mkdirs crossover-path))
  (run-local-project project crossover-path builds
    '(require 'cljsbuild.compiler 'cljsbuild.crossover 'cljsbuild.util)
    `(do
      (letfn [(copy-crossovers# []
                (cljsbuild.crossover/copy-crossovers
                  ~crossover-path
                  '~crossovers))]
        (copy-crossovers#)
        (when ~watch?
          (cljsbuild.util/once-every 1000 "copying crossovers" copy-crossovers#))
        (cljsbuild.util/in-threads
          (fn [opts#]
            (cljsbuild.compiler/run-compiler
              (:source-path opts#)
              ~crossover-path
              (:compiler opts#)
              ~watch?))
          '~builds)))))

(defn- run-tests [project {:keys [test-commands crossover-path builds]} args]
  (when (> (count args) 1)
    (throw (Exception. "Only expected zero or one arguments.")))
  (let [selected-tests (if (empty? args)
                         (do
                           (println "Running all ClojureScript tests.")
                           (vec (vals test-commands)))
                         (do
                           (println "Running ClojureScript test:" (first args))
                           [(test-commands (first args))]))]
  (run-local-project project crossover-path builds
    '(require 'cljsbuild.test)
    `(cljsbuild.test/run-tests ~selected-tests))))

(defn- once
  "Compile the ClojureScript project once."
  [project options]
  (run-compiler project options false))

(defn- auto
  "Automatically recompile when files are modified."
  [project options]
  (run-compiler project options true))

(defn- clean
  "Remove automatically generated files."
  [project {:keys [crossover-path builds]}]
  (println "Deleting ClojureScript-related generated files.")
  (fs/delete-dir repl-output-path)
  (fs/delete-dir crossover-path)
  (run-local-project project crossover-path builds
    '(require 'cljsbuild.clean 'cljsbuild.util)
    `(cljsbuild.util/in-threads
      (fn [opts#]
        (cljsbuild.clean/cleanup-files
          (:compiler opts#)))
      '~builds)))

(defn- test
  "Run ClojureScript tests."
  [project options args]
  (let [compile-result (run-compiler project options false)]
    (if (not= compile-result exit-success)
      compile-result
      (run-tests project options args))))

(defn- repl-listen
  "Run a REPL that will listen for incoming connections."
  [project {:keys [crossover-path builds repl-listen-port]}]
  (println (str "Running ClojureScript REPL, listening on port " repl-listen-port "."))
  (run-local-project project crossover-path builds
    '(require 'cljsbuild.repl.listen)
    `(cljsbuild.repl.listen/run-repl-listen
      ~repl-listen-port
      ~repl-output-path)))

(defn- repl-launch
  "Run a REPL and launch a custom command to connect to it."
  [project {:keys [crossover-path builds repl-listen-port repl-launch-commands]} args]
  (when (< (count args) 1)
    (throw (Exception. "Must supply a launch command identifier.")))
  (let [launch-name (first args)
        command-args (rest args)
        command-base (repl-launch-commands launch-name)]
    (when (nil? command-base)
      (throw (Exception. (str "Unknown REPL launch command: " launch-name))))
    (let [command (concat command-base command-args)]
      (println "Running ClojureScript REPL and launching command:" command)
      (run-local-project project crossover-path builds
        '(require 'cljsbuild.repl.listen)
        `(cljsbuild.repl.listen/run-repl-launch
            ~repl-listen-port
            ~repl-output-path
            '~command)))))

(defn- repl-rhino
  "Run a Rhino-based REPL."
  [project {:keys [crossover-path builds]}]
  (println "Running Rhino-based ClojureScript REPL.")
  (run-local-project project crossover-path builds
    '(require 'cljsbuild.repl.rhino)
    `(cljsbuild.repl.rhino/run-repl-rhino)))

(defn cljsbuild
  "Run the cljsbuild plugin."
  {:help-arglists '([once auto clean test repl-listen repl-launch repl-rhino])
   :subtasks [#'once #'auto #'clean #'test #'repl-listen #'repl-launch #'repl-rhino]}
  ([project]
    (println
      (lhelp/help-for "cljsbuild"))
    exit-failure)
  ([project subtask & args]
    (let [options (config/extract-options project)]
      (case subtask
        "once" (once project options)
        "auto" (auto project options)
        "clean" (clean project options)
        "test" (test project options args)
        "repl-listen" (repl-listen project options)
        "repl-launch" (repl-launch project options args)
        "repl-rhino" (repl-rhino project options)
        (do
          (println
            "Subtask" (str \" subtask \") "not found."
            (lhelp/subtask-help-for *ns* #'cljsbuild))
          exit-failure)))))

(defn- compile-hook [task & args]
  (let [compile-result (apply task args)]
    (if (not= compile-result exit-success)
      compile-result
      (run-compiler (first args) (config/extract-options (first args)) false))))

(defn- test-hook [task & args]
  (let [test-results [(apply task args)
                      (run-tests (first args) (config/extract-options (first args)) [])]]
    (if (every? #(= % exit-success) test-results)
      exit-success
      exit-failure)))

(defn- clean-hook [task & args]
  (apply task args)
  (clean (first args) (config/extract-options (first args))))

(defn- jar-hook [task & [project out-file filespecs]]
  (apply task [project out-file (concat filespecs (jar/get-filespecs project))]))

; FIXME: These hooks do NOT work with lein2.  It looks like hooks have changed
;        significantly.  Do more research on the subject.
(hooke/add-hook #'lcompile/compile #'compile-hook)
(hooke/add-hook #'ltest/test #'test-hook)
(hooke/add-hook #'lclean/clean #'clean-hook)
(hooke/add-hook #'ljar/write-jar #'jar-hook)
