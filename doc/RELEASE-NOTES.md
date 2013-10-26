# Release Notes for lein-cljsbuild

## [1.0.0](https://github.com/emezeske/lein-cljsbuild/issues?milestone=31&state=closed)

* The name of each `:test-command` entry is now printed prior to that test being
  run. (gh-244)
* Fixed regression where `cljsbuild auto` would exit on a compilation error
  (gh-249)

## [0.3.4](https://github.com/emezeske/lein-cljsbuild/issues?milestone=31&state=closed)

* Added new `sample` subtask that emits the contents of the `sample.project.clj`
  file detailing cljsbuild's options (gh-232)
* Hard compilation failures now properly fail the broader Leiningen
  invocation (gh-234)
* Any non-string values in a `:test-command` vector now properly cause a failure
  (gh-243)
* The `:output-wrapper` ClojureScript compiler option is now defaulted to `true`
  if the `:optimizations` option is set to `:advanced` (gh-201)
* Fixed an issue where case-sensitivity of the drive letter on windows prevented
  the proper relativization of paths to Clojure files containing macros to be
  (re)loaded (gh-240)
* Test runs now properly fail if any `:test-commands` vector contains any
  non-string values (gh-243)

## [0.3.3](https://github.com/emezeske/lein-cljsbuild/issues?milestone=28&state=closed)

1. Changed to use upstream ClojureScript version 0.0-1859.
2. cljsbuild now warns if you have not explicitly specified a ClojureScript
   dependency in your project. (gh-224)
3. The file scanning associated with `cljsbuild auto` has been improved
   significantly, and should now represent a negligible CPU load. (gh-219)
4. Under `cljsbuild auto`, Clojure files are now only reloaded if they define
   macros. (gh-210)
5. A sane error message is now emitted if you attempt to run a nonexistent
   cljsbuild task (gh-215)
6. Various documentation and example project tweaks.

## 0.3.2

1. Changed to use upstream ClojureScript version 0.0-1806.

## 0.3.1

1. Changed to use upstream ClojureScript version 0.0-1803.
2. Updated the Clojure version used in the plugin to 1.5.1.
3. This plugin version requires Leiningen version 2.1.2 or higher.
4. Fix `lein cljsbuild test` so that it exits quickly (i.e. without a 30-second delay).
5. Hide the Clojure stacktrace when ClojureScript unit tests fail.

## 0.3.0

1. Dropped support for Leiningen 1.x, now that Leiningen 2.0 is available.  **REPEAT: Leiningen 1.x is no longer supported.**
2. Changed the `:source-path "path"` option to `:source-paths ["path" "path"]`.  The new option accepts a vector of paths rather than a single path.  **The old singular `:source-path` is now deprecated and will be removed soon.**
3. Changed all default output paths (e.g. for temporary compiler files, crossover files, and REPL files) to be in the `:target-path` directory.  Explicitly configured paths can still be whatever you like.
4. For compatibility with Leiningen 2.0, `:resource-paths` is now used instead of `:resources-path`.
5. Fixed a long delay before exiting that could sometimes occur after one-shot tasks (like `cljsbuild once`).
6. Changes to JavaScript files in `:libs` now trigger rebuilds when using `cljsbuild auto`.
7. Removed CLOSURE_NO_DEPS notes from example projects, as it is no longer necessary.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=25&state=closed)

## 0.2.10

1. Changed to use upstream ClojureScript version 0.0-1552.
2. Balanced the parenthesis and square braces in the README correctly.
3. Added a workaround for an unresolved upstream compiler issue: http://dev.clojure.org/jira/browse/CLJS-418.
4. Modifications to JavaScript files specified in the :libs compiler option will now cause :builds to be rebuilt.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=24&state=closed)

## 0.2.9

1. Changed to use upstream ClojureScript version 0.0-1513.
2. Changed to use clj-stacktrace version 0.2.5.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=23&state=closed)

## 0.2.8

1. Fixed a bug where a `RejectedExecutionException` could be thrown if hooks were enabled and the project was run via `lein trampoline`.
2. Added the ability to set `clojure.core/*assert*` via the `:assert` option in each `:builds` map.
3. Fixed a bug where if exceptions would not be caught if thrown while automatically reloading a Clojure file (e.g. containing macros).
4. Changed to use upstream ClojureScript version 0.0-1503.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=22&state=closed)

## 0.2.7

1. Fix a bug introduced in 0.2.6 that broke the `cljsbuild jar` task for Leiningen 2.x.
2. Alleviate the need for the parent project to specify a Clojure version.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=21&state=closed)

## 0.2.6

1. Updated to support recent preview releases of Leiningen 2.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=20&state=closed)

## 0.2.5

1. Changed to use upstream ClojureScript version 0.0-1450.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=19&state=closed)

## 0.2.4

1. Removed support for `:warn-on-undeclared`, because the compiler itself now supports a `:warnings` option.  Use that instead.

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=18&state=closed)

## 0.2.3

1. Changed to use upstream ClojureScript version 0.0-1443.
2. Ignore hidden files in the source and crossover paths (this makes things work better with emacs or other editors that use dotfiles for state).

[Milestone Details for this Release](https://github.com/emezeske/lein-cljsbuild/issues?milestone=17&state=closed)

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
