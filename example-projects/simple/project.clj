(defproject cljsbuild-example-simple "0.3.0"
  :description "A simple example of how to use lein-cljsbuild"
  :source-paths ["src-clj"]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [compojure "1.0.4"]
                 [hiccup "1.0.0"]]
  :plugins [[lein-cljsbuild "0.2.9"]
            [lein-ring "0.7.0"]]
  :cljsbuild {
    :builds [{:source-path "src-cljs"
              :compiler {:output-to "resources/public/js/main.js"
                         :optimizations :whitespace
                         :pretty-print true}}]}
  :ring {:handler example.routes/app}
  ; for Leiningen 1.x:
  :source-path "src-clj"
  :dev-dependencies [[lein-ring "0.7.0"]])
