# Release Notes for lein-cljsbuild

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
