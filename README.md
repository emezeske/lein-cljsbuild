# lein-cljsbuild

This is a leiningen plugin that makes it easy (and quick) to compile
ClojureScript source into JavaScript.  It's similar to [cljs-watch] [1],
but is driven via lein instead of via a standalone executable.

  [1]: https://github.com/ibdknox/cljs-watch

##  Installation

You can install the plugin via lein:

    $ lein plugin install emezeske/lein-cljsbuild 0.0.1

Or by adding lein-cljs to your `project.clj` file in the `:dev-dependencies`
section:

```clojure
(defproject my-thingie "1.2.3"
  :dev-dependencies [[emezeske/lein-cljsbuild "0.0.1"]])
```

Make sure you pull down the jar file:

    $ lein deps

## Configuration

The lein-cljsbuild configuration is specified under the `:cljsbuild` section
of your `project.clj` file:

```clojure
(defproject my-thingie "1.2.3"
  :dev-dependencies [[emezeske/lein-cljsbuild "0.0.1"]]
  :cljsbuild {
    ; The path to the top-level ClojureScript source directory:
    :source-dir "src-cljs"
    ; The path to the JavaScript output file:
    :output-file "war/javascripts/main.js"
    ; Compiler optimization level.  May be :whitespace, :simple, or :advanced.
    ; See the ClojureScript compiler documentation for details.
    :optimizations :whitespace
    ; Specifies whether the compiler will format the JavaScript output nicely.
    :pretty-print true})
```

##  Usage

Once the plugin is installed, you can build the ClojureScript once:

    $ lein cljsbuild once

Or you can have lein-cljsbuild watch your source files for changes and
automatically rebuild them.  This is recommended for development, as it
avoids the time-consuming JVM startup for each build:

    $ lein cljsbuild auto

##  License

Source Copyright © Evan Mezeske, 2011.
Released under the Eclipse Public License - v 1.0.
See the file COPYING.
