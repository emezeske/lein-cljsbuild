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
    [leiningen.trampoline :as ltrampoline]
    [robert.hooke :as hooke]))

; TODO: lein2: All temporary output files, etc, should go into (:target-path project),
;       in which case they will automatically be removed upon "lein clean".

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

(defn- parse-notify-command [build]
  (assoc build :parsed-notify-command
    (config/parse-shell-command (:notify-command build)))) 

(defn- run-compiler [project {:keys [crossover-path crossovers builds]} build-ids watch?]
  (doseq [build-id build-ids]
    (if (empty? (filter #(= (:id %) build-id) builds))
      (throw (Exception. (str "Unknown build identifier: " build-id)))))
  (println "Compiling ClojureScript.")
  ; If crossover-path does not exist before eval-in-project is called,
  ; the files it contains won't be classloadable, for some reason.
  (when (not-empty crossovers)
    (fs/mkdirs crossover-path))
  (let [filtered-builds (if (empty? build-ids)
                          builds
                          (filter #(some #{(:id %)} build-ids) builds))
        parsed-builds (map parse-notify-command filtered-builds)]
    (run-local-project project crossover-path parsed-builds
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
                (:parsed-notify-command opts#)
                ~watch?))
            '~parsed-builds))))))

(defn- run-tests [project {:keys [test-commands crossover-path builds]} args]
  (when (> (count args) 1)
    (throw (Exception. "Only expected zero or one arguments.")))
  (let [selected-tests (if (empty? args)
                         (do
                           (println "Running all ClojureScript tests.")
                           (vals test-commands))
                         (do
                           (println "Running ClojureScript test:" (first args))
                           [(test-commands (first args))]))
        parsed-tests (map config/parse-shell-command selected-tests)]
  (run-local-project project crossover-path builds
    '(require 'cljsbuild.test)
    `(cljsbuild.test/run-tests '~parsed-tests))))

(defmacro require-trampoline [& forms]
  `(if ltrampoline/*trampoline?*
    (do ~@forms)
    (do
      (println "REPL subcommands must be run via \"lein trampoline cljsbuild <command>\".")
      exit-failure)))

(defn- once
  "Compile the ClojureScript project once."
  [project options build-ids]
  (run-compiler project options build-ids false))

(defn- auto
  "Automatically recompile when files are modified."
  [project options build-ids]
  (run-compiler project options build-ids true))

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
  (require-trampoline
    (println (str "Running ClojureScript REPL, listening on port " repl-listen-port "."))
    (run-local-project project crossover-path builds
      '(require 'cljsbuild.repl.listen)
      `(cljsbuild.repl.listen/run-repl-listen
        ~repl-listen-port
        ~repl-output-path))))

(defn- repl-launch
  "Run a REPL and launch a custom command to connect to it."
  [project {:keys [crossover-path builds repl-listen-port repl-launch-commands]} args]
  (require-trampoline
    (when (< (count args) 1)
      (throw (Exception. "Must supply a launch command identifier.")))
    (let [launch-name (first args)
          command-args (rest args)
          command-base (repl-launch-commands launch-name)]
      (when (nil? command-base)
        (throw (Exception. (str "Unknown REPL launch command: " launch-name))))
      (let [parsed (config/parse-shell-command command-base)
            shell (concat (:shell parsed) command-args)
            command (assoc parsed :shell shell)]
        (println "Running ClojureScript REPL and launching command:" shell)
        (run-local-project project crossover-path builds
          '(require 'cljsbuild.repl.listen)
          `(cljsbuild.repl.listen/run-repl-launch
              ~repl-listen-port
              ~repl-output-path
              '~command))))))

(defn- repl-rhino
  "Run a Rhino-based REPL."
  [project {:keys [crossover-path builds]}]
  (require-trampoline
    (println "Running Rhino-based ClojureScript REPL.")
    (run-local-project project crossover-path builds
      '(require 'cljsbuild.repl.rhino)
      `(cljsbuild.repl.rhino/run-repl-rhino))))

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
        "once" (once project options args)
        "auto" (auto project options args)
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

; Lein2 "preps" the project when eval-in-project is called.  This
; causes it to be compiled, which normally would trigger the compile
; hook below, which is bad because we can't compile unless we're in
; the dummy subproject.  To solve this problem, we disable all of the
; hooks if we notice that lein2 is currently prepping.
(defmacro skip-if-prepping [task args & forms]
  `(if (subproject/prepping?)
    (apply ~task ~args)
    (do ~@forms)))

(defn- compile-hook [task & args]
  (skip-if-prepping task args
    (let [compile-result (apply task args)]
      (if (not= compile-result exit-success)
        compile-result
        (run-compiler (first args) (config/extract-options (first args)) false)))))

(defn- test-hook [task & args]
  (skip-if-prepping task args
    (let [test-results [(apply task args)
                        (run-tests (first args) (config/extract-options (first args)) [])]]
      (if (every? #(= % exit-success) test-results)
        exit-success
        exit-failure))))

(defn- clean-hook [task & args]
  (skip-if-prepping task args
    (apply task args)
    (clean (first args) (config/extract-options (first args)))))

(defn- jar-hook [task & [project out-file filespecs]]
  (skip-if-prepping task [project out-file filespecs]
    (apply task [project out-file (concat filespecs (jar/get-filespecs project))])))

(hooke/add-hook #'lcompile/compile #'compile-hook)
(hooke/add-hook #'ltest/test #'test-hook)
(hooke/add-hook #'lclean/clean #'clean-hook)
(hooke/add-hook #'ljar/write-jar #'jar-hook)
