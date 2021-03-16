(defproject cljsbuild-example-simple "2.0.0-SNAPSHOT"
  :description "A simple example of how to use lein-cljsbuild"
  :source-paths ["src-clj"]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.597"
                  :exclusions [org.apache.ant/ant]]
                 [compojure "1.6.2"]
                 [hiccup "1.0.5"]]
  :plugins [[lein-cljsbuild "2.0.0-SNAPSHOT"]
            [lein-ring "0.12.5"]]
  :cljsbuild {
    :builds [{:source-paths ["src-cljs"]
              :compiler {:output-to "resources/public/js/main.js"
                         :optimizations :whitespace
                         :pretty-print true}}]}
  :ring {:handler simple.routes/app})
