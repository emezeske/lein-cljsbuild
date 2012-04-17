(defproject lein-cljsbuild "0.1.8"
  :description "ClojureScript Autobuilder Plugin"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo}
  :dependencies [[fs "1.1.2"]]
  :dev-dependencies [[midje "1.3.1"]
                     ; NOTE: lein-midje requires different versions to be
                     ; installed for lein1 vs lein2 compatibility :(.
                     [lein-midje "1.0.9"]]
  :eval-in-leiningen true)
