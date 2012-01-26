(defproject cljsbuild-example-simple "0.0.9"
  :description "A simple example of how to use lein-cljsbuild"
  :source-path "src-clj"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [compojure "0.6.5"] 
                 [hiccup "0.3.7"]]
  :dev-dependencies [[lein-cljsbuild "0.0.9"]
                     [lein-ring "0.5.0"]]
  :cljsbuild {:source-path "src-cljs"
              :compiler {:output-to "resources/public/js/main.js"
                         :optimizations :whitespace
                         :pretty-print true}}
  :ring {:handler example.routes/app})
