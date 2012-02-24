(defproject cljsbuild-example-advanced "0.1.0"
  :description "An advanced example of how to use lein-cljsbuild"
  :source-path "src-clj"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [compojure "1.0.1"]
                 [hiccup "0.3.8"]
                 ; NOTE: This log4j dependency is not actually used by the project;
                 ; it's just here to make sure that lein-cljsbuild handles complex
                 ; dependencies correctly.
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  :dev-dependencies [[lein-ring "0.5.4"]]
  :plugins [[lein-cljsbuild "0.1.0"]]
  ; Enable the lein hooks for: clean, compile, test, and jar.
  :hooks [leiningen.cljsbuild]
  :cljsbuild {
    ; Configure the REPL support; see the README.md file for more details.
    :repl-listen-port 9000
    :repl-launch-commands
      ; Launch command for connecting the page of choice to the REPL.
      ; Only works if the page at URL automatically connects to the REPL,
      ; like http://localhost:3000/repl-demo does.
      ;     $ lein trampoline cljsbuild repl-launch firefox <URL>
      {"firefox" ["firefox"]
      ; Launch command for interacting with your ClojureScript at a REPL,
      ; without browsing to the app (a static HTML file is used).
      ;     $ lein trampoline cljsbuild repl-launch firefox-naked
       "firefox-naked" ["firefox" "resources/private/html/naked.html"]
      ; This is similar to "firefox" except it uses PhantomJS.
      ;     $ lein trampoline cljsbuild repl-launch phantom <URL>
       "phantom" ["phantomjs" "phantom/repl.js"]
      ; This is similar to "firefox-naked" except it uses PhantomJS.
      ;     $ lein trampoline cljsbuild repl-launch phantom-naked
       "phantom-naked" ["phantomjs" "phantom/repl.js" "resources/private/html/naked.html"]}
    :test-commands
      ; Test command for running the unit tests in "test-cljs" (see below).
      ;     $ lein cljsbuild test
      {"unit" ["phantomjs" "phantom/unit-test.js" "resources/private/html/unit-test.html"]}
    :crossovers [example.crossover]
    :crossover-jar true
    :builds [
      ; This build has the lowest level of optimizations, so it is
      ; useful when debugging the app.
      {:source-path "src-cljs"
       :jar true
       :compiler {:output-to "resources/public/js/main-debug.js"
                  :optimizations :whitespace
                  :pretty-print true}}
      ; This build has the highest level of optimizations, so it is
      ; efficient when running the app in production.
      {:source-path "src-cljs"
       :compiler {:output-to "resources/public/js/main.js"
                  :optimizations :advanced
                  :pretty-print false}}
      ; This build is for the ClojureScript unit tests that will
      ; be run via PhantomJS.  See the phantom/unit-test.js file
      ; for details on how it's run.
      {:source-path "test-cljs"
       :compiler {:output-to "resources/private/js/unit-test.js"
                  :optimizations :whitespace
                  :pretty-print true}}]}
  :ring {:handler example.routes/app})
