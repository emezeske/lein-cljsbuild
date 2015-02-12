(ns leiningen.cljsbuild
  "Compile ClojureScript source into a JavaScript file."
  (:refer-clojure :exclude [test])
  (:require
    [fs.core :as fs]
    [leiningen.cljsbuild.config :as config]
    [leiningen.cljsbuild.jar :as jar]
    [leiningen.cljsbuild.subproject :as subproject]
    [leiningen.compile :as lcompile]
    [leiningen.core.eval :as leval]
    [leiningen.core.main :as lmain]
    [leiningen.help :as lhelp]
    [leiningen.jar :as ljar]
    [leiningen.test :as ltest]
    [leiningen.trampoline :as ltrampoline]
    [robert.hooke :as hooke]
    [clojure.java.io :as io]
    [clojure.string :as string]))

(def ^:private repl-output-path "repl")

(defn- run-local-project [project crossover-path builds requires form]
  (leval/eval-in-project (subproject/make-subproject project crossover-path builds)
    ; Without an explicit exit, the in-project subprocess seems to just hang for
    ; around 30 seconds before exiting.  I don't fully understand why...
    `(try
       (do
         ~form
         (System/exit 0))
       (catch cljsbuild.test.TestsFailedException e#
         ; Do not print stack trace on test failure
         (System/exit 1))
       (catch Exception e#
         (do
           (.printStackTrace e#)
           (System/exit 1))))
    requires))

(defn- run-compiler [project {:keys [crossover-path crossovers builds]} build-ids watch?]
  (doseq [build-id build-ids]
    (if (empty? (filter #(= (:id %) build-id) builds))
      (throw (Exception. (str "Unknown build identifier: " build-id)))))
  (println "Compiling ClojureScript.")
  ; If crossover-path does not exist before eval-in-project is called,
  ; the files it contains won't be classloadable, for some reason.
  (when (not-empty crossovers)
    (println "\033[31mWARNING: lein-cljsbuild crossovers are deprecated, and will be removed in future versions. See https://github.com/emezeske/lein-cljsbuild/blob/master/doc/CROSSOVERS.md for details.\033[0m")
    (fs/mkdirs crossover-path))
  (let [filtered-builds (if (empty? build-ids)
                          builds
                          (filter #(some #{(:id %)} build-ids) builds))
        parsed-builds (map config/parse-notify-command filtered-builds)]
    (doseq [build parsed-builds]
      (config/warn-unsupported-warn-on-undeclared build)
      (config/warn-unsupported-notify-command build))
    (run-local-project project crossover-path parsed-builds
      '(require 'cljsbuild.compiler 'cljsbuild.crossover 'cljsbuild.util)
      `(do
        (letfn [(copy-crossovers# []
                  (cljsbuild.crossover/copy-crossovers
                    ~crossover-path
                    '~crossovers))]
          (copy-crossovers#)
          (when ~watch?
            (cljsbuild.util/once-every-bg 1000 "copying crossovers" copy-crossovers#))
          (let [crossover-macro-paths# (cljsbuild.crossover/crossover-macro-paths '~crossovers)
                builds# (for [opts# '~parsed-builds]
                          [opts# (cljs.env/default-compiler-env (:compiler opts#))])]
            ;; Prep the environments
            (doseq [[build# compiler-env#] builds#]
              ;; Require any ns if necessary
              (doseq [handler# (:warning-handlers build#)]
                (when (symbol? handler#)
                  (let [[n# sym#] (string/split (str handler#) #"/")]
                    (assert (and n# sym#)
                            (str "Symbols for :warning-handlers must be fully-qualified, " (pr-str handler#) " is missing namespace."))
                    (when (and n# sym#)
                      (require (symbol n#)))))))
            (loop [dependency-mtimes# (repeat (count builds#) {})]
              (let [builds-mtimes# (map vector builds# dependency-mtimes#)
                    new-dependency-mtimes#
                      (doall
                       (for [[[build# compiler-env#] mtimes#] builds-mtimes#]
                       (cljs.analyzer/with-warning-handlers
                         (if-let [handlers# (:warning-handlers build#)]
                           ;; Prep the warning handlers via eval and
                           ;; resolve if custom, otherwise default to
                           ;; built-in warning handlers
                           (mapv (fn [handler#]
                                   ;; Resolve symbols to their fns
                                   (if (symbol? handler#)
                                     (resolve handler#)
                                     handler#)) (eval handlers#))
                           cljs.analyzer/*cljs-warning-handlers*)
                         (binding [cljs.env/*compiler* compiler-env#]
                           (cljsbuild.compiler/run-compiler
                            (:source-paths build#)
                            ~crossover-path
                            crossover-macro-paths#
                            (:compiler build#)
                            (:parsed-notify-command build#)
                            (:incremental build#)
                            (:assert build#)
                            mtimes#
                            ~watch?)))))]
                 (when ~watch?
                   (Thread/sleep 100)
                   (recur new-dependency-mtimes#))))))))))

(defn- run-tests [project {:keys [test-commands crossover-path builds]} args]
  (when (> (count args) 1)
    (lmain/abort "Only expected zero or one arguments."))
  (when (and (= (count args) 1) (not (get test-commands (first args))))
    (lmain/abort (format "No such test name: %s - valid names are: %s"
                         (first args)
                         (apply str (interpose ", " (keys test-commands))))))
  (let [selected-tests (if (empty? args)
                           (seq test-commands)
                           [[(first args) (test-commands (first args))]])
        parsed-tests (map (fn [[test-name test-cmd]]
                            [test-name (config/parse-shell-command test-cmd)])
                          selected-tests)]
    (doseq [[_ test-command] test-commands]
      (when-not (every? string? test-command)
        (lmain/abort
          "Invalid :test-command, contains non-string value:" test-command)))
    (when (empty? (:shell (second (first parsed-tests))))
      (println (str "Could not locate test command " (first args) "."))
      (lmain/abort))
    (run-local-project project crossover-path builds
                       '(require 'cljsbuild.test)
                       `(cljsbuild.test/run-tests '~parsed-tests))))

(defmacro require-trampoline [& forms]
  `(if ltrampoline/*trampoline?*
    (do ~@forms)
    (do
      (println "REPL subcommands must be run via \"lein trampoline cljsbuild <command>\".")
      (lmain/abort))))

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
  [project {:keys [crossover-path builds repl-launch-commands test-commands]}]
  (lmain/abort "\033[31m`cljsbuild clean` is deprecated as of 1.0.4; please just use `lein clean`, and either direct ClojureScript compilation output to `target/*`, or add your ClojureScript output paths to Leiningen's `:clean-targets` vector.\033[0m"))

(defn- test
  "Run ClojureScript tests."
  [project options args]
    (run-compiler project options nil false)
    (run-tests project options args))

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

(defn- sample
  "Display a sample project.clj."
  []
  (-> (io/resource "sample.project.clj") slurp println))

(defn cljsbuild
  "Run the cljsbuild plugin."
  {:help-arglists '([once auto test repl-listen repl-launch repl-rhino sample])
   :subtasks [#'once #'auto #'test #'repl-listen #'repl-launch #'repl-rhino #'sample]}
  ([project]
    (println
      (lhelp/help-for "cljsbuild"))
    (lmain/abort))
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
        "sample" (sample)
        (do
          (println
            "Subtask" (str \" subtask \") "not found."
            (lhelp/subtask-help-for *ns* #'cljsbuild))
          (lmain/abort))))))

(defn compile-hook [task & args]
  (apply task args)
  (run-compiler (first args) (config/extract-options (first args)) nil false))

(defn test-hook [task & args]
  (apply task args)
  (run-tests (first args) (config/extract-options (first args)) []))

(defn jar-hook [task & [project out-file filespecs]]
  (apply task [project out-file (concat filespecs (jar/get-filespecs project))]))

(defn activate
  "Set up hooks for the plugin.  Eventually, this can be changed to just hook,
   and people won't have to specify :hooks in their project.clj files anymore."
  []
  (hooke/add-hook #'lcompile/compile #'compile-hook)
  (hooke/add-hook #'ltest/test #'test-hook)
  (hooke/add-hook #'ljar/write-jar #'jar-hook))
