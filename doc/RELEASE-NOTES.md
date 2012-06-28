# Release Notes for lein-cljsbuild

## 0.2.2

1. Changed to use upstream ClojureScript version 0.0-1424.
2. Fixed an issue with copying crossover files under Windows.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=16&state=closed)

## 0.2.1

1. Automatically add `closure-js/libs` to `:libs` and `closure-js/externs` to `:externs`.  This means that libraries can put their libs and externs in `resources/closure-js/libs/<library-name>` and `resources/closure-js/externs/<library-name>`, respectively, and lein-cljsbuild will automatically pick them up.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=15&state=closed)

## 0.2.0

Note that the minor version was incremented not due to any major features, but due to the fact that the `:notify-command` option was changed in a backwards-incompatible way.

1. The compiler is now run under Clojure 1.4.0.
2. Added a new `:build` suboption `:incremental`, which determines whether intermediate JavaScript sources are left intact between automatic builds.  The old behavior was to delete intermediate files before each build.  This slowed things down, but worked around (unisolated) problems with incremental compilation.  Incremental builds are now the default, as they appear to work well, but this option allows the old behavior to be selected if necessary for troubleshooting.
3. The :notify option has been changed such that its argument is consistent with other cljsbuild shell commands.  This means that the `%` argument is no longer respected, and the textual result will simply be appended as the last command line argument.  Also, `:beep true` no longer has any effect.  If either of these features is desired, the recommended solution is a small shell script wrapper.
4. Clojure source files that reside in the ClojureScript :source-path (as well as crossover macro files) are now monitored for changes.  When modified, they will be reloaded, and a build will be triggered.  This is useful for ClojureScript projects that use macros.
5. Multiple builds are now built sequentially instead of in parallel.  This is due to the fact that the underlying compiler is no longer thread-safe.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=14&state=closed)

## 0.1.10

1. Changed to use upstream ClojureScript version 0.0-1236.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=13&state=closed)

## 0.1.9

1. Changed to use upstream ClojureScript version 0.0-1211.
2. Updated example projects to use the latest Clojure, Ring, Compojure, and Hiccup versions.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=12&state=closed)

## 0.1.8

1. Minor fix for compatibility with the latest Leiningen 2 preview.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?sort=created&direction=desc&state=closed&page=1&milestone=11)

## 0.1.7

1. The various REPL commands now work when used via Leiningen 2.  This should mean that lein-cljsbuild is fully Leiningen-2-compatible.
2. Raise a descriptive error if the parent project uses Clojure < 1.3.
3. Ensure that `lein cljsbuild clean` cleans up :stdout and :stderr files for various commands.
4. Add a comprehensive unit test suite, to hopefully help prevent new releases from breaking things.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?sort=created&direction=desc&state=closed&page=1&milestone=10)

## 0.1.6

1. Changed to use upstream ClojureScript version 0.0-1011.  This should fix REPL issues.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?sort=created&direction=desc&state=closed&page=1&milestone=9)
