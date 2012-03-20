# lein-cljsbuild

This is a Leiningen plugin that makes it quick and easy to automatically compile
your ClojureScript code into Javascript whenever you modify it.  It's simple
to install and allows you to configure the ClojureScript compiler from within your
`project.clj` file.

Beyond basic compiler support, lein-cljsbuild can optionally help with a few other things:

* [Launching REPLs for interactive development] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/doc/REPL.md)
* [Launching ClojureScript tests] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/doc/TESTING.md)
* [Sharing code between Clojure and ClojureScript] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/doc/CROSSOVERS.md)

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
  :plugins [[lein-cljsbuild "0.1.3"]])
```

For versions of Leiningen older than `1.7.0` (not recommended), add
lein-cljsbuild to the `:dev-dependencies` section instead:

```clj
; Using Leiningen 1.6.x or older:
(defproject lein-cljsbuild-example "1.2.3"
  :dev-dependencies [[lein-cljsbuild "0.1.3"]])
```

Make sure you pull down the jar file:

    $ lein deps

## Upgrading from 0.0.x

The `:cljsbuild` configuration format has changed.  This version is backwards-compatible
with `0.0.x`, but the next major version won't be.  See the
[migration documentation] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/doc/MIGRATING-TO-0.1.3.md)
for details.

## Just Give Me a Damned Example Already!

See the
[example-projects] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/example-projects)
directory for a couple of simple examples of how to use lein-cljsbuild.  The
[simple project] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/example-projects/simple)
shows a dead-simple "compile only" configuration, which is a good place to start.  The
[advanced project] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/example-projects/advanced)
contains examples of how to use the extended features of the plugin.

Also, see the
[sample.project.clj] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/sample.project.clj)
file for an exhaustive list of all options supported by lein-cljsbuild.

## Basic Configuration

The lein-cljsbuild configuration is specified under the `:cljsbuild` section
of your `project.clj` file.  A simple project might look like this:

```clj
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "0.1.3"]]
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
[sample.project.clj] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/sample.project.clj)
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

Some common lein-cljsbuild tasks can hook into the main Leiningen tasks
to enable ClojureScript support in each of them.  The following tasks are
supported:

    $ lein compile
    $ lein clean
    $ lein test
    $ lein jar

To enable ClojureScript support for these tasks, add the following entry to
your project configuration:

```clj
:hooks [leiningen.cljsbuild]
```

Note that by default the `lein jar` task does *not* package your ClojureScript
code in the JAR file.  This feature needs to be explicitly enabled by adding
the following entry to each of the `:builds` that you want included in the
JAR file.

```clj
:jar true
```

If you are using the
[crossovers] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/doc/CROSSOVERS.md)
feature, and want the `:crossover-path` included in the JAR file, add this entry to your
top-level `:cljsbuild` configuration:

```clj
:crossover-jar true
```

## Multiple Build Configurations

If the `:builds` sequence contains more than one map lein-cljsbuild
will treat each map as a separate ClojureScript compiler configuration,
and will build all of them in parallel:

```clj
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "0.1.3"]]
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
[example-projects/advanced] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/example-projects/advanced)
directory for a working example of a project that uses this feature.

## REPL Support

Lein-cljsbuild has built-in support for launching ClojureScript REPLs in a variety
of ways.  See the
[REPL documentation] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/doc/REPL.md)
for more details.

## Testing Support

Lein-cljsbuild has built-in support for running external ClojureScript test processes.  See the
[testing documentation] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/doc/TESTING.md)
for more details.

## Sharing Code Between Clojure and ClojureScript

Sharing code with lein-cljsbuild is accomplished via the configuration
of "crossovers".  See the
[crossovers documentation] (https://github.com/emezeske/lein-cljsbuild/blob/0.1.3/doc/CROSSOVERS.md)
for more details.

##  License

Source Copyright © Evan Mezeske, 2011-2012.
Released under the Eclipse Public License - v 1.0.
See the file COPYING.

## Contributors

* Evan Mezeske **(Author)** (evan@mezeske.com)
* Shantanu Kumar (kumar.shantanu@gmail.com)
* Luke VanderHart (http://github.com/levand)
* Phil Hagelberg (phil@hagelb.org)
* Daniel E. Renfer (duck@kronkltd.net)
* Daniel Harper (http://djhworld.github.com)
* Philip Kamenarsky (http://github.com/pkamenarsky)
