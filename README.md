# lein-cljsbuild

This is a Leiningen plugin that makes it quick and easy to automatically compile
your ClojureScript code into Javascript whenever you make changes to it.  It's simple
to install and allows you to configure the ClojureScript compiler from within your
`project.clj` file.

Beyond basic compiler support, lein-cljsbuild can optionally help with a few other things:
    * [Sharing code between Clojure and ClojureScript] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.0/doc/REPL.md)
    * [Launching REPLs for interactive development] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.0/doc/CROSSOVERS.md)

## Requirements

The lein-cljsbuild plugin works with
[Leiningen] (https://github.com/technomancy/leiningen/blob/master/README.md)
version `1.6.2` or higher, although `1.7.0` or higher is recommended.

## Installation

If you're using Leiningen `1.7.0` or newer, you can install the plugin by
adding lein-cljsbuild to your `project.clj` file in the `:plugins` section:

```clj
; Using Leiningen 1.7.0 or newer:
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "0.1.0"]])
```

For versions of Leiningen older than `1.7.0` (not recommended), add
lein-cljsbuild to the `:dev-dependencies` section instead:

```clj
; Using Leiningen 1.6.x or older:
(defproject lein-cljsbuild-example "1.2.3"
  :dev-dependencies [[lein-cljsbuild "0.1.0"]])
```

Make sure you pull down the jar file:

    $ lein deps

## Just Give Me a Damned Example Already!

See the
[example-projects] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.0/example-projects)
directory for a couple of simple examples of how to use lein-cljsbuild.  The
[simple project] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.0/example-projects/simple)
shows a dead-simple "compile only" configuration, which is a good place to start.  The
[advanced project] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.0/example-projects/advanced)
contains examples of how to use the extended features of the plugin.

Also, see the
[sample.project.clj] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.0/sample.project.clj)
file for an exhaustive list of all options supported by lein-cljsbuild.

## Basic Configuration

The lein-cljsbuild configuration is specified under the `:cljsbuild` section
of your `project.clj` file.  A simple project might look like this:

```clj
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "0.1.0"]]
  :cljsbuild {
    :builds [{
        ; The path to the top-level ClojureScript source directory:
        :source-path "src-cljs"
        ; The standard ClojureScript compiler options:
        ; (See the ClojureScript compiler documentation for details.)
        :compiler {
          :output-to "war/javascripts/main.js"  ; default: main.js in current directory
          :optimizations :whitespace
          :pretty-print true}}]})
```

For an exhaustive list of the configuration options supported by lein-cljsbuild, see the
[sample.project.clj] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.0/sample.project.clj)
file.

## Basic Usage

Once the plugin is installed, you can build the ClojureScript once:

    $ lein cljsbuild once

Or you can have lein-cljsbuild watch your source files for changes and
automatically rebuild them.  This is recommended for development, as it
avoids the time-consuming JVM startup for each build:

    $ lein cljsbuild auto

To delete all of the JavaScript and ClojureScript files that lein-cljsbuild
automatically generated during compilation, run:

    $ lein cljsbuild clean

## Hooks

If you'd like your ClojureScript to be compiled when you run `lein compile`, and
deleted when you run `lein clean`, add the following entry to your project
configuration:

```clj
:hooks [leiningen.cljsbuild]
```

Note that this is also required for lein-cljsbuild to hook into the `lein jar`
task.  For that to work, you will also need to explicitly enable the `jar` hook
by adding the following entry to your :cljsbuild configuration map:

```clj
:jar true
```

## Multiple Build Configurations

If the `:builds` sequence contains more than one map lein-cljsbuild
will treat each map as a separate ClojureScript compiler configuration,
and will build all of them in parallel:

```clj
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "0.1.0"]]
  :cljsbuild {
    :builds [
      {:source-path "src-cljs-main"
       :compiler {:output-to "main.js"}}
      {:source-path "src-cljs-other"
       :compiler {:output-to "other.js"}}}])
```

This is extremely convenient for doing library development in ClojureScript.
This allows cljsbuild to compile in all four optimization levels at once, for
easier testing, or to compile a test suite alongside the library code.

See the
[example-projects/advanced] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.0/example-projects/advanced)
directory for a working example of a project that uses this feature.

## REPL Support

Lein-cljsbuild has built-in support for launching ClojureScript REPLs in a variety
of ways.  See the
[REPL documentation] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.0/doc/REPL.md)
for more details.

# Sharing Code Between Clojure and ClojureScript

Sharing code with lein-cljsbuild is accomplished via the configuration
of "crossovers".  See the
[crossovers documentation] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.0/doc/CROSSOVERS.md)
for more details.

##  License

Source Copyright © Evan Mezeske, 2011-2012.
Released under the Eclipse Public License - v 1.0.
See the file COPYING.

## Contributors

* Evan Mezeske **(Author)** (evan@mezeske.com)
* Shantanu Kumar (kumar.shantanu@gmail.com)
* Luke VanderHart (http://github.com/levand)
