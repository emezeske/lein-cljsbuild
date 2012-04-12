(ns cljsbuild.repl.listen
  (:require
    [cljs.repl :as repl]
    [cljs.repl.browser :as browser]
    [cljsbuild.util :as util]
    [clojure.string :as string]))

(defn run-repl-listen [port output-dir]
  (let [env (browser/repl-env :port (Integer. port) :working-dir output-dir)]
    (repl/repl env)))

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
