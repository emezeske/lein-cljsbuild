(defproject cljsbuild-example-simple "1.0.0-alpha1"
  :description "A simple example of how to use lein-cljsbuild"
  :source-paths ["src-clj" "src-cljs"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-1859"
                  :exclusions [org.apache.ant/ant]]
                 [compojure "1.1.6"]
                 [hiccup "1.0.4"]]
  :plugins [[lein-cljsbuild "1.0.0-alpha1"]
            [lein-ring "0.8.8"]]
  :cljsbuild {
    :builds [{:source-paths ["src-cljs"]
              :compiler {:output-to "resources/public/js/main.js"
                         :optimizations :whitespace
                         :pretty-print true}}]}
  :ring {:handler example.routes/app})
