(defproject cljsbuild-example-advanced "0.2.7"
  :description "An advanced example of how to use lein-cljsbuild"
  ; Source path for Leiningen 1.x:
  :source-path "src-clj"
  ; Source paths for Leiningen 2.x:
  :source-paths ["src-clj"]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [compojure "1.0.4"]
                 [hiccup "1.0.0"]]
  :dev-dependencies [[lein-ring "0.7.0"]]
  :plugins [[lein-cljsbuild "0.2.7"]]
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
      {"firefox" ["firefox"
                  :stdout ".repl-firefox-out"
                  :stderr ".repl-firefox-err"]
      ; Launch command for interacting with your ClojureScript at a REPL,
      ; without browsing to the app (a static HTML file is used).
      ;     $ lein trampoline cljsbuild repl-launch firefox-naked
       "firefox-naked" ["firefox"
                        "resources/private/html/naked.html"
                        :stdout ".repl-firefox-naked-out"
                        :stderr ".repl-firefox-naked-err"]
      ; This is similar to "firefox" except it uses PhantomJS.
      ;     $ lein trampoline cljsbuild repl-launch phantom <URL>
       "phantom" ["phantomjs"
                  "phantom/repl.js"
                  :stdout ".repl-phantom-out"
                  :stderr ".repl-phantom-err"]
      ; This is similar to "firefox-naked" except it uses PhantomJS.
      ;     $ lein trampoline cljsbuild repl-launch phantom-naked
       "phantom-naked" ["phantomjs"
                        "phantom/repl.js"
                        "resources/private/html/naked.html"
                        :stdout ".repl-phantom-naked-out"
                        :stderr ".repl-phantom-naked-err"]}
    :test-commands
      ; Test command for running the unit tests in "test-cljs" (see below).
      ;     $ lein cljsbuild test
      {"unit" ["phantomjs"
               "phantom/unit-test.js"
               "resources/private/html/unit-test.html"]}
    :crossovers [example.crossover]
    :crossover-jar true
    :builds {
      ; This build has the lowest level of optimizations, so it is
      ; useful when debugging the app.
      :dev
      {:source-path "src-cljs"
       :jar true
       :compiler {:output-to "resources/public/js/main-debug.js"
                  :optimizations :whitespace
                  :pretty-print true}}
      ; This build has the highest level of optimizations, so it is
      ; efficient when running the app in production.
      :prod
      {:source-path "src-cljs"
       :compiler {:output-to "resources/public/js/main.js"
                  :optimizations :advanced
                  :pretty-print false}}
      ; This build is for the ClojureScript unit tests that will
      ; be run via PhantomJS.  See the phantom/unit-test.js file
      ; for details on how it's run.
      :test
      {:source-path "test-cljs"
       :compiler {:output-to "resources/private/js/unit-test.js"
                  :optimizations :whitespace
                  :pretty-print true}}}}
  :ring {:handler example.routes/app})
