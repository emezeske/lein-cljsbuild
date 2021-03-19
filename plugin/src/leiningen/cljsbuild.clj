(ns leiningen.cljsbuild
  "Compile ClojureScript source into a JavaScript file."
  (:refer-clojure :exclude [test])
  (:require [me.raynes.fs :as fs]
            [leiningen.cljsbuild.config :as config]
            [leiningen.cljsbuild.jar :as jar]
            [leiningen.cljsbuild.subproject :as subproject]
            [leiningen.compile :as lcompile]
            [leiningen.core.eval :as leval]
            [leiningen.core.project :as lproject]
            [leiningen.core.main :as lmain]
            [leiningen.help :as lhelp]
            [leiningen.jar :as ljar]
            [leiningen.test :as ltest]
            [leiningen.trampoline :as ltrampoline]
            [robert.hooke :as hooke]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(def ^:private repl-output-path "repl")

(def ^:dynamic *suppress-exit?* false)

(defn- exit
  ([code]
    (if-not *suppress-exit?*
      `(System/exit ~code)
      (when-not (zero? code)
        `(throw (ex-info "Suppress exit" {:exit-code ~code})))))
  ([] (exit 0)))

(defn- run-local-project [project builds requires form]
  (leval/eval-in-project (subproject/make-subproject project builds)
    `(try
       ~form
       (catch cljsbuild.test.TestsFailedException e#
         ; Do not print stack trace on test failure
         ~(exit 1))
       (catch Exception e#
         (do
           (.printStackTrace e#)
           ~(exit 1))))
    requires))

(defn- read-dependency-project [project-file]
  (when (fs/exists? project-file)
    (let [project-file-path (.getAbsolutePath (io/file project-file))]
      (try
        (lproject/read project-file-path)
        (catch Exception e
          (throw (Exception. (format "Problem loading %s" project-file-path) e)))))))

(defn- read-checkouts
  [project]
  (let [checkouts-dir (io/file (:root project) "checkouts")]
    (for [dep (fs/list-dir checkouts-dir)
          :let [project-file (io/file dep "project.clj")
                checkout-project (read-dependency-project project-file)]
          :when checkout-project]
      checkout-project)))

(defn- walk-checkouts [root-project f]
  (loop [[project & rest] (read-checkouts root-project)
         walk-results []]
    (if project
      (recur (concat (read-checkouts project) rest)
             (concat (f project) walk-results))
      walk-results)))

(defn- checkout-cljs-paths [project]
  (walk-checkouts
    project
    (fn [checkout]
      (if-let [{:keys [builds]} (config/extract-options checkout)]
        (for [build builds
              path (:source-paths build)]
          (.getAbsolutePath (io/file (:root checkout) path)))))))

(defn- run-compiler [project {:keys [builds]} build-ids watch?]
  (doseq [build-id build-ids]
    (if (empty? (filter #(= (:id %) build-id) builds))
      (throw (Exception. (str "Unknown build identifier: " build-id)))))
  (println (if watch? "Watching for changes before compiling ClojureScript..." "Compiling ClojureScript..."))
  (let [filtered-builds (if (empty? build-ids)
                          builds
                          (filter #(some #{(:id %)} build-ids) builds))
        parsed-builds (map config/parse-notify-command filtered-builds)
        checkout-cljs-paths (checkout-cljs-paths project)
        root (:root project)]
    (doseq [build parsed-builds]
      (config/warn-unsupported-warn-on-undeclared build)
      (config/warn-unsupported-notify-command build))
    (run-local-project project parsed-builds
      '(require 'cljsbuild.compiler 'cljsbuild.util)
      `(let [builds# (for [opts# '~parsed-builds]
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
         (loop [modified-times# (repeat (count builds#) {})]
           (let [builds-modified-times# (map vector builds# modified-times#)
                 new-modified-times#
                 (doall
                  (for [[[build# compiler-env#] build-modified-times#] builds-modified-times#]
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
                         {:cljs-paths (:source-paths build#)
                          :checkout-paths '~checkout-cljs-paths
                          :compiler-options (:compiler build#)
                          :notify-command (:parsed-notify-command build#)
                          :incremental? (:incremental build#)
                          :assert? (:assert build#)
                          :last-modified-times build-modified-times#
                          :watching? ~watch?
                          :project-root ~root})))))]
             (when ~watch?
               (Thread/sleep 100)
               (recur new-modified-times#))))))))

(defn- run-tests [project {:keys [test-commands builds]} args]
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
    (when (seq parsed-tests)
      (when (empty? (:shell (second (first parsed-tests))))
        (println (str "Could not locate test command " (first args) "."))
        (lmain/abort))
      (run-local-project project builds
                         '(require 'cljsbuild.test)
                         `(cljsbuild.test/run-tests '~parsed-tests)))))

(defmacro require-trampoline [& forms]
  `(if ltrampoline/*trampoline?*
    (do ~@forms)
    (do
      (println "REPL subcommands must be run via \"lein trampoline cljsbuild <command>\".")
      (lmain/abort))))

(defn- deps
  "Downloads internal `lein-cljsbuild` dependencies"
  [project]
  (println "Downloading cljsbuild dependencies..")
  (leval/eval-in-project
   (subproject/make-subproject project nil nil)
   `(println "Done.")))

(defn- once
  "Compile the ClojureScript project once."
  [project options build-ids]
  (run-compiler project options build-ids false))

(defn- auto
  "Automatically recompile when files are modified."
  [project options build-ids]
  (run-compiler project options build-ids true))

(defn- test
  "Run ClojureScript tests."
  [project options args]
    (run-compiler project options nil false)
    (run-tests project options args))

(defn- repl-listen
  "Run a REPL that will listen for incoming connections."
  [project {:keys [builds repl-listen-port]}]
  (require-trampoline
    (println (str "Running ClojureScript REPL, listening on port " repl-listen-port "."))
    (run-local-project project builds
      '(require 'cljsbuild.repl.listen)
      `(cljsbuild.repl.listen/run-repl-listen
        ~repl-listen-port
        ~repl-output-path))))

(defn- repl-launch
  "Run a REPL and launch a custom command to connect to it."
  [project {:keys [builds repl-listen-port repl-launch-commands]} args]
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
        (run-local-project project builds
          '(require 'cljsbuild.repl.listen)
          `(cljsbuild.repl.listen/run-repl-launch
              ~repl-listen-port
              ~repl-output-path
              '~command))))))

(defn- repl-rhino
  "Run a Rhino-based REPL."
  [project {:keys [builds]}]
  (require-trampoline
    (println "Running Rhino-based ClojureScript REPL.")
    (run-local-project project builds
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
        "deps" (deps project)
        "once" (once project options args)
        "auto" (auto project options args)
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
  "Set up hooks for the plugin. Eventually, this can be changed to just hook,
   and people won't have to specify :hooks in their project.clj files anymore."
  []
  (hooke/add-hook #'lcompile/compile #'compile-hook)
  (hooke/add-hook #'ltest/test #'test-hook)
  (hooke/add-hook #'ljar/write-jar #'jar-hook))
