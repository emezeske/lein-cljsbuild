# lein-cljsbuild

This is a Leiningen plugin that makes it quick and easy to automatically compile
your ClojureScript code into Javascript whenever you modify it.  It's simple
to install and allows you to configure the ClojureScript compiler from within your
`project.clj` file.

Beyond basic compiler support, lein-cljsbuild can optionally help with a few other things:

* [Launching REPLs for interactive development] (https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/doc/REPL.md)
* [Launching ClojureScript tests] (https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/doc/TESTING.md)
* [Sharing code between Clojure and ClojureScript] (https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/doc/CROSSOVERS.md)

The latest version of lein-cljsbuild is `1.0.0`.
[See the release notes here.](https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/doc/RELEASE-NOTES.md)

## Requirements

The lein-cljsbuild plugin works with
[Leiningen] (https://github.com/technomancy/leiningen/blob/master/README.md)
version `2.1.2` or higher.

## Installation

You can install the plugin by adding lein-cljsbuild to your `project.clj`
file in the `:plugins` section:

```clj
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "1.0.0"]])
```

In addition, _you should add an explicit ClojureScript dependency to your
project_, like this:

```clojure
:dependencies [[org.clojure/clojurescript "0.0-XXXX"]]
```

lein-cljsbuild will add a dependency to your project if it doesn't already
contain one, but that functionality will not remain for long.  In general, you
can use lein-cljsbuild with any ClojureScript version.

## Just Give Me a Damned Example Already!

See the
[example-projects] (https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/example-projects)
directory for a couple of simple examples of how to use lein-cljsbuild.  The
[simple project] (https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/example-projects/simple)
shows a dead-simple "compile only" configuration, which is a good place to start.  The
[advanced project] (https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/example-projects/advanced)
contains examples of how to use the extended features of the plugin.

Also, see the
[sample.project.clj] (https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/sample.project.clj)
file for an exhaustive list of all options supported by lein-cljsbuild.

## Basic Configuration

The lein-cljsbuild configuration is specified under the `:cljsbuild` section
of your `project.clj` file.  A simple project might look like this:

```clj
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "1.0.0"]]
  :cljsbuild {
    :builds [{
        ; The path to the top-level ClojureScript source directory:
        :source-paths ["src-cljs"]
        ; The standard ClojureScript compiler options:
        ; (See the ClojureScript compiler documentation for details.)
        :compiler {
          :output-to "war/javascripts/main.js"  ; default: target/cljsbuild-main.js
          :optimizations :whitespace
          :pretty-print true}}]})
```

For an exhaustive list of the configuration options supported by lein-cljsbuild, see the
[sample.project.clj] (https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/sample.project.clj)
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

If you've upgraded any libraries, you *probably* want to run `lein cljsbuild clean` afterward.

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
JAR file. `lein uberjar` derives its behavior from `lein jar` and will include
the ClojureScript as well if enabled.

```clj
:jar true
```

If you are using the
[crossovers] (https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/doc/CROSSOVERS.md)
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
  :plugins [[lein-cljsbuild "1.0.0"]]
  :cljsbuild {
    :builds [
      {:source-paths ["src-cljs-main"]
       :compiler {:output-to "main.js"}
      {:source-paths ["src-cljs-other"]
       :compiler {:output-to "other.js"}}]})
```

This is extremely convenient for doing library development in ClojureScript.
This allows cljsbuild to compile in all four optimization levels at once, for
easier testing, or to compile a test suite alongside the library code.

You can optionally assign an ID to a build configuration and build
only that one:

```clj
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "1.0.0"]]
  :cljsbuild {
    :builds [
      {:source-paths ["src-cljs-main"]
       :compiler {:output-to "main.js"}}
      {:id "other"
       :source-paths ["src-cljs-other"]
       :compiler {:output-to "other.js"}}]})
```

    $ lein cljsbuild auto other

If you want IDs for all of your build configurations, you can specify
them as a map instead of a vector:

```clj
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "1.0.0"]]
  :cljsbuild {
    :builds {
      :main
      {:source-paths ["src-cljs-main"]
       :compiler {:output-to "main.js"}}
      :other
      {:source-paths ["src-cljs-other"]
       :compiler {:output-to "other.js"}}}})
```

You can also build multiple configurations at once:

    $ lein cljsbuild auto main other

See the
[example-projects/advanced] (https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/example-projects/advanced)
directory for a working example of a project that uses this feature.

## REPL Support

Lein-cljsbuild has built-in support for launching ClojureScript REPLs in a variety
of ways.  See the
[REPL documentation] (https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/doc/REPL.md)
for more details.

## Testing Support

Lein-cljsbuild has built-in support for running external ClojureScript test processes.  See the
[testing documentation] (https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/doc/TESTING.md)
for more details.

## Sharing Code Between Clojure and ClojureScript

Sharing code with lein-cljsbuild is accomplished via the configuration
of "crossovers".  See the
[crossovers documentation] (https://github.com/emezeske/lein-cljsbuild/blob/1.0.0/doc/CROSSOVERS.md)
for more details.

## ClojureScript Version

After configuring lein-cljsbuild, `lein deps` will fetch a known-good version of the ClojureScript compiler.
You can use a different version of the compiler via a local clone of the ClojureScript git repository.
See [the wiki](https://github.com/emezeske/lein-cljsbuild/wiki/Using-a-Git-Checkout-of-the-ClojureScript-Compiler) for details.

##  License

Source Copyright © Evan Mezeske, 2011-2013.
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
* Felix H. Dahlke (fhd@ubercode.de)
* Jenan Wise (http://github.com/jenanwise)
* Kris Jenkins (http://github.com/krisajenkins)
* Daniel Turczański (http://jvmsoup.com/)
* Brandon Henry (http://brandonhenry.net/)
* Daniel Gregoire (http://techylinguist.com)
* Chas Emerick (chas@cemerick.com)
* Geoff Salmon (geoff.salmon@gmail.com)
* Brandon Bloom (brandon@brandonbloom.name)
* dspiteself (https://github.com/dspiteself)
* Dirk Geurs (https://github.com/Dirklectisch)
* Chris Allen (cma@bitemyapp.com)
* Sean Grove (s@bushi.do)
* Thomas Heller (info@zilence.net)
* Micah Martin (micah@8thlight.com)
* Harri Salokorpi (harri.salokorpi@reaktor.fi)
