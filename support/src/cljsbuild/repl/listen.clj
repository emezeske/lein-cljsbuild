(ns cljsbuild.repl.listen
  (:require
    [cljs.repl :as repl]
    [cljs.repl.browser :as browser]
    [cljsbuild.util :as util]
    [clojure.string :as string]))

(defn run-repl-listen [port output-dir]
  (let [env (browser/repl-env :port (Integer. port) :working-dir output-dir)]
    (repl/repl env)))

(defn process-start-delayed [command]
  (try
    ; TODO: Poll the REPL to see if it's ready before starting
    ;       the process, instead of sleeping.
    (Thread/sleep 5000)
    (util/process-start command)
    (catch Exception e
      (println "Error in background process: " e)
      (throw e))))

(defn run-repl-launch [port output-dir command]
  (println "Background command output will be shown after the REPL is exited via :cljs/quit .")
  (let [process-delayed (future (process-start-delayed command))]
    (try
      (run-repl-listen port output-dir)
      (finally
        (let [process @process-delayed]
          ((:kill process))
          ((:wait process)))))))
