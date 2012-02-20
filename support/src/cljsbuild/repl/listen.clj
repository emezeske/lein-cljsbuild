(ns cljsbuild.repl.listen
  (:require
    [cljs.repl :as repl]
    [cljs.repl.browser :as browser]
    [clojure.string :as string])
  (:import
    (java.io InputStreamReader OutputStreamWriter)))

(defn run-repl-listen [port output-dir]
  (let [env (browser/repl-env :port (Integer. port) :working-dir output-dir)]
    (repl/repl env)))

(defn- stream-seq
  "Takes an InputStream and returns a lazy seq of integers from the stream."
  [stream]
  (take-while #(>= % 0) (repeatedly #(.read stream))))

(defn- start-bg-command [command]
  (Thread/sleep 1000) 
  (let [process (.exec (Runtime/getRuntime) (into-array command))]
    ; TODO: Maybe do something better with output; just stream it out?
    (with-open [stdout (.getInputStream process)
                stderr (.getErrorStream process)]
      (let [[out err]
             (for [stream [stdout stderr]]
               (apply str (map char (stream-seq (InputStreamReader. stream "UTF-8")))))]
        {:process process :out out :err err}))))

(defn run-repl-launch [port output-dir command]
  (println "Background command output will be shown after the REPL is exited via :cljs/quit .")
  (let [bg (future (start-bg-command command))]
    (run-repl-listen port output-dir)
    (try
      (let [{:keys [process out err]} @bg
            delim (string/join (take 80 (repeat "-")))
            header #(str delim "\n" % "\n" delim)]
        ; FIXME: This is ugly, and doesn't always work (sigh).  I often have to
        ;        resort to Ctrl+C to kill the console.
        (.destroy process)
        (.waitFor process) 
        (when (not= (.length out) 0)
          (println (header "Standard output from launched command:"))
          (println out))
        (when (not= (.length err) 0)
          (println (header "Standard error from launched command:"))
          (println err)))
      (catch Exception e
        ; TODO: destroy the process if it was started!
        (binding [*out* *err*]
          (println "Launching command failed:" e))))))
