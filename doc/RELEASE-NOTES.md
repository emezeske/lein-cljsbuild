# Release Notes for lein-cljsbuild

## 0.2.0

1. The compiler is now run under Clojure 1.4.0.
2. Added a new `:build` suboption `:incremental`, which determines whether intermediate JavaScript sources are left intact between automatic builds.  The old behavior was to delete intermediate files before each build.  This slowed things down, but worked around (unisolated) problems with incremental compilation.  Incremental builds are now the default, as they appear to work well, but this option allows the old behavior to be selected if necessary for troubleshooting.
3. The :notify option has been changed such that its argument is consistent with other cljsbuild shell commands.  This means that the `%` argument is no longer respected, and the textual result will simply be appended as the last command line argument.  Also, `:beep true` no longer has any effect.  If either of these features is desired, the recommended solution is a small shell script wrapper.

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
