(defproject cljsbuild-example-advanced "0.1.0"
  :description "An advanced example of how to use lein-cljsbuild"
  :source-path "src-clj"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [compojure "0.6.5"] 
                 [hiccup "0.3.7"]
                 ; NOTE: This log4j dependency is not actually used by the project;
                 ; it's just here to make sure that lein-cljsbuild handles complex
                 ; dependencies correctly.
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  :dev-dependencies [[lein-ring "0.5.0"]]
  :plugins [[lein-cljsbuild "0.1.0"]]
  ; Enable the "lein clean" and "lein compile" hooks.
  :hooks [leiningen.cljsbuild]
  :cljsbuild {
    ; Configure the REPL support; see the README.md file for more details.
    :repl-listen-port 9000
    :repl-launch-commands
      {"firefox" ["firefox"]
       "firefox-naked" ["firefox" "resources/public/html/naked.html"]
       "phantom" ["phantomjs" "phantom/page-repl.js"]
       "phantom-naked" ["phantomjs" "phantom/page-repl.js" "resources/public/html/naked.html"]}
    ; Configure two separate builds; one with few optimizations for
    ; development/debugging, and one with many optimizations for
    ; production use.
    :builds [
      {:source-path "src-cljs"
       :jar true
       :crossovers [example.crossover]
       :compiler {:output-to "resources/public/js/main-debug.js"
                  :optimizations :whitespace
                  :pretty-print true}}
      {:source-path "src-cljs"
       :crossovers [example.crossover]
       :compiler {:output-to "resources/public/js/main.js"
                  :optimizations :advanced
                  :pretty-print false}}]}
  :ring {:handler example.routes/app})
