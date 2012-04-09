(ns leiningen.test.cljsbuild.jar
  (:use
    leiningen.cljsbuild.jar
    midje.sweet))

(fact
  (relative-path "/" "/a") => "a"
  (relative-path "/a/b/c" "/a/b/c/d/e") => "d/e"
  (relative-path "" "a") => (throws Exception)
  (relative-path "/a/b/c" "/a/b") => (throws Exception))

; TODO: More tests!
