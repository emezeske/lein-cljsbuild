# lein-cljsbuild

This is a leiningen plugin that makes it easy (and quick) to compile
ClojureScript source into JavaScript.  It's similar to [cljs-watch] [1],
but is driven via lein instead of via a standalone executable.  This means
that your project can depend on a specific version of lein-cljsbuild, fetch
it via `lein deps`, and you don't have to install any special executables into
your `PATH`.

Also, this plugin has built-in support for seamlessly sharing code between
your Clojure server-side project and your ClojureScript client-side project.

  [1]: https://github.com/ibdknox/cljs-watch

## Installation

You can install the plugin via lein:

    $ lein plugin install emezeske/lein-cljsbuild 0.0.5

Or by adding lein-cljs to your `project.clj` file in the `:dev-dependencies`
section:

```clojure
(defproject lein-cljsbuild-example "1.2.3"
  :dev-dependencies [[emezeske/lein-cljsbuild "0.0.5"]])
```

Make sure you pull down the jar file:

    $ lein deps

## Just Give Me a Damned Example Already!

See the `example-projects` directory for a couple of simple examples
of how to use lein-cljsbuild.

## Configuration

The lein-cljsbuild configuration is specified under the `:cljsbuild` section
of your `project.clj` file.  A simple project might look like this:

```clojure
(defproject lein-cljsbuild-example "1.2.3"
  :dev-dependencies [[emezeske/lein-cljsbuild "0.0.5"]]
  :cljsbuild {
    ; The path to the top-level ClojureScript source directory:
    :source-path "src-cljs"
    ; The standard ClojureScript compiler options:
    ; (See the ClojureScript compiler documentation for details.)
    :compiler {
      :output-to "war/javascripts/main.js"  ; default: main.js in current directory
      :optimizations :whitespace
      :pretty-print true}})
```

If you'd like your ClojureScript to be compiled whenever you run `lein compile`,
you can also add the following entry to your defproject config:

```clojure
:hooks [leiningen.cljsbuild]
```

##  Usage

Once the plugin is installed, you can build the ClojureScript once:

    $ lein cljsbuild once

Or you can have lein-cljsbuild watch your source files for changes and
automatically rebuild them.  This is recommended for development, as it
avoids the time-consuming JVM startup for each build:

    $ lein cljsbuild auto

## Sharing Code Between Clojure and ClojureScript

Sharing code with lein-cljsbuild is accomplished via the configuration
of "crossovers".  A crossover specifies a namespace in your Clojure project,
the content of which should be copied into your ClojureScript project.  The
files in the Clojure directory will be monitored and copied over when they are
modified.  Of course, remember that since the namespace will be used by both Clojure
and ClojureScript, it will need to only use the subset of features provided by
both languages.

Assuming that your top-level directory structure looks something like this:

<pre>
├── src-clj
│   └── example
│       ├── core.clj
│       ├── something.clj
│       └── crossover
│           ├── some_stuff.clj
│           └── some_other_stuff.clj
└── src-cljs
    └── example
        ├── core.cljs
        ├── whatever.cljs
        └── util.cljs
</pre>

And your `project.clj` file looks like this:

```clojure
(defproject lein-cljsbuild-example "1.2.3"
  :dev-dependencies [[emezeske/lein-cljsbuild "0.0.5"]]
  :source-path "src-clj"
  :cljsbuild {
    :source-path "src-cljs"
    ; Each entry in the :crossovers vector describes a Clojure namespace
    ; that is meant to be used with the ClojureScript code as well.
    ; The files that make up this namespace will be automatically copied
    ; into the ClojureScript source path whenever they are modified.
    :crossovers [example.crossover]
    :compiler {
      :output-to "war/javascripts/main.js"  ; default: main.js in current directory
      :optimizations :whitespace
      :pretty-print true}})
```

Then lein-cljsbuild would copy files from `src-clj/example/crossover`
to `src-cljs/example/crossover`, and you'd end up with this:

<pre>
├── src-clj
│   └── example
│       ├── a_file.clj
│       ├── core.clj
│       └── crossover
│           ├── some_stuff.clj
│           └── some_other_stuff.clj
└── src-cljs
    └── example
        ├── a_different_file.cljs
        ├── crossover
        │   ├── some_stuff.cljs
        │   └── some_other_stuff.cljs
        ├── whatever.cljs
        └── util.cljs
</pre>

With this setup, you would probably want to add `src-cljs/example/crossover`
to your `.gitignore` file (or equivalent), as its contents are updated automatically
by lein-cljsbuild.

## Sharing Macros Between Clojure and ClojureScript

In ClojureScript, macros are still written in Clojure, and can not be written
in the same file as actual ClojureScript code.  Also, to use them in a ClojureScript
namespace, they must be required via `:require-macros` rather than the usual `:require`.

This makes using the crossover feature to share macros between Clojure and ClojureScript
a bit difficult, but lein-cljsbuild has some special constructs to make it possible.

Three things need to be done to use lein-cljsbuild to share macros.

### 1. Keep Macros in Separate Files

These examples assume that your project uses the  `src-clj/example/crossover`
directory, and that all of the macros are in a file called
`src-clj/example/crossover/macros.clj`.

### 2. Tell lein-cljsbuild Which Files Contain Macros

Add this magical comment to any crossover files that contain macros:

```clojure
;*CLJSBUILD-MACRO-FILE*;
```

This tells lein-cljsbuild to keep the `.clj` file extension when copying
the files into the ClojureScript directory, instead of changing it to `.cljs`
as usual.

### 3. Use Black Magic to Require Macros Specially

In any crossover Clojure file, lein-cljsbuild will automatically erase the
following string (if it appears):

```clojure
;*CLJSBUILD-REMOVE*;
```

This magic can be used to generate a `ns` statement that will work in both
Clojure and ClojureScript:

```clojure
(ns example.crossover.some_stuff
  (:require;*CLJSBUILD-REMOVE*;-macros
    [example.crossover.macros :as macros]))
```

Thus, after removing comments, Clojure will see:

```clojure
(ns example.crossover.some_stuff
  (:require
    [example.crossover.macros :as macros]))
```

However, lein-cljsbuild will remove the `;*CLJSBUILD-REMOVE*;` string entirely,
before copying the file.  Thus, ClojureScript will see:

```clojure
(ns example.crossover.some_stuff
  (:require-macros
    [example.crossover.macros :as macros]))
```

And thus the macros can be shared.

##  License

Source Copyright © Evan Mezeske, 2011-2012.
Released under the Eclipse Public License - v 1.0.
See the file COPYING.
