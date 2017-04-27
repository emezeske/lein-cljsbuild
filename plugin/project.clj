(defproject lein-cljsbuild "1.1.6"
  :description "ClojureScript Autobuilder Plugin"
  :url "http://github.com/emezeske/lein-cljsbuild"
  :license
    {:name "Eclipse Public License - v 1.0"
     :url "http://www.eclipse.org/legal/epl-v10.html"
     :distribution :repo}
    :dependencies [[fs "1.1.2"]]
  :profiles {
    :dev {
      :dependencies [
        [midje "1.6.3"]
        [cljsbuild "1.1.6"]]
      :plugins [[lein-midje "3.1.3"]]}}
  :eval-in-leiningen true)
