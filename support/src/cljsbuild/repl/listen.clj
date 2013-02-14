(ns cljsbuild.repl.listen
  (:require
    [cljs.repl :as repl]
    [cljs.repl.browser :as browser]
    [cljsbuild.util :as util]
    [clojure.string :as string]
    [clojure.tools.nrepl :as nrepl]
    [cemerick.piggieback :as piggieback]))

(defn run-repl-listen [port output-dir]
    (let [conn (nrepl/connect :port 64042) ;; need to figure out how to get this from project.clj's repl-options map
          session (nrepl/client-session (nrepl/client conn Long/MAX_VALUE))]
      (doall (nrepl/message session
        {:op "eval" :code "(require 'cemerick.piggieback 'cljs.repl 'cljs.repl.browser)"}))
      (doall (nrepl/message session
        {:op "eval" :code (string/join
          ["(let [env (cljs.repl.browser/repl-env :port (Integer. " port ") :working-dir \"" output-dir "\")]
              (cemerick.piggieback/cljs-repl :repl-env (doto env cljs.repl/-setup)))"])}))))

(defn piggieback-listen [port output-dir]
  (let [env (browser/repl-env :port (Integer. port) :working-dir output-dir)]
    (piggieback/cljs-repl
      :repl-env (doto env repl/-setup))))

(defn delayed-process-start [command]
  (future
    (try
      ; TODO: Poll the REPL to see if it's ready before starting
      ;       the process, instead of sleeping.
      (util/sleep 5000)
      (util/process-start command)
      (catch Exception e
        (println "Error in background process: " e)
        (throw e)))))

(defn delayed-process-wait [process]
  (let [p @process]
    ((:kill p))
    ((:wait p))))

(defn run-repl-launch [port output-dir command]
  (let [process (delayed-process-start command)]
    (try
      (run-repl-listen port output-dir)
      (finally
        (delayed-process-wait process)))))
