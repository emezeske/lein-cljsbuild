(ns leiningen.test.cljsbuild.util
  (:use
    leiningen.cljsbuild.util
    midje.sweet))

(fact
  (relative-path "/" "/a") => "a"
  (relative-path "/a/b/c" "/a/b/c/d/e") => "d/e"
  (relative-path "" "a") => "a"
  (relative-path "/a/b/c" "/a/b") => "/a/b")
