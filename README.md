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

## Requirements

The lein-cljsbuild plugin works with [Leiningen] [2] version `1.6.2` or higher,
although 1.7.0 or higher is recommended.

  [2]: https://github.com/technomancy/leiningen/blob/master/README.md

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

See the `example-projects` directory for a couple of simple examples
of how to use lein-cljsbuild.

Also, see the `sample.project.clj` file in this directory for an
exhaustive list of all options supported by lein-cljsbuild.

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

For an exhaustive list of the configuration options supported by lein-cljsbuild,
see the `sample.project.clj` file in this directory.

##  Usage

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

See the `example-projects/advanced` directory for a working example of a
project that uses this feature.

## REPL Support

Lein-cljsbuild has built-in support for launching ClojureScript REPLs in a variety
of ways.  Note as of Leiningen 1.x, all plugin REPL commands must be launched
via `lein trampoline` to work correctly.

### repl-rhino

The simplest REPL uses Rhino to evaluate the JavaScript that results from compiling
your ClojureScript input.  This REPL does not have access to your project's namespaces,
and is not evaluated in a browser context.  It is, however, useful for running simple
pure-ClojureScript commands.  It is also the only REPL option that does not require
your application to provide additional support code:

    $ lein trampoline cljsbuild repl-rhino

### repl-listen

The next REPL is more sophisticated.  With support from your application, it is possible
to use `repl-listen` to run a REPL with access to your application's ClojureScript namespaces.
The REPL may be started as follows:

    $ lein trampoline cljsbuild repl-listen

This will open a REPL, which will listen on a TCP port for a ClojureScript application
to connect to it.  By default, it will listen on port `9000`, although this may be changed
via the `:repl-listen-port` option.  Until a connection is received, the REPL will not be
usable.

From your application, you can connect to the REPL with code such as this:

```clj
(ns lein-cljsbuild-example.repl
   (:require [clojure.browser.repl :as repl]))
; Use of "localhost" will only work for local development.
; Change the port to match the :repl-listen-port.
(repl/connect "http://localhost:9000/repl")
```

For instance, you might include the above call to `repl/connect` in the code for
a particular web page served by your application.  So, you would launch your application,
launch `repl-listen`, and then browse to that page.  It would then connect to the REPL,
enabling you to execute commands in the context of that page.

For more information on this approach, see the [ClojureScript Wiki] [3].

  [3] https://github.com/clojure/clojurescript/wiki/Quick-Start

### repl-launch

Finally, the most sophisticated REPL.  Like `repl-listen`, `repl-launch` requires
application support code to function.  The difference between `repl-listen` and `repl-launch`
is that the latter may be configured to automatically launch the browser after starting
the REPL.  This REPL is launched as follows:

    $ lein trampoline cljsbuild repl-launch <launch-id>

Of course, this won't work until you've told lein-cljsbuild what to launch.  Multiple
launch presets may be created, and thus the `<launch-id>` parameter is used to select
between them.  To configure a launch preset, add an entry to the `:repl-launch-commands` map:

```clj
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "0.1.0"]]
  :cljsbuild {
    :repl-listen-port 9000
    :repl-launch-commands
      {"my-launch" ["firefox" "-jsconsole" "http://localhost/my-page"]})
```

With this configuration, the launch preset is identified by `my-launch`.  When a REPL
is started with this launch preset, `firefox -jsconsole http://localhost/my-page`
will be run in the background, and presumably the page it loads will connect to the REPL:

    $ lein trampoline cljsbuild repl-launch my-launch

Note that any additional arguments following the `<launch-id>` will be passed to the
launch command.  Thus, with a configuration such as:

```clj
:repl-launch-commands
  {"my-other-launch" ["firefox" "-jsconsole"}
```

The target URL could be selected like so:

    $ lein trampoline cljsbuild repl-launch my-other-launch http://localhost/another-page

For more ideas on how to use `repl-launch`, take a look at `example-projects/advanced`.
It has several examples of useful launch commands, with descriptions in its README.
Note that, in particular, the possibilities with [PhantomJS] [4] are very intriguing.

  [4] http://www.phantomjs.org/

## Sharing Code Between Clojure and ClojureScript

Sharing code with lein-cljsbuild is accomplished via the configuration
of "crossovers".  A crossover specifies a Clojure namespace, the content
of which should be copied into your ClojureScript project.  This can be any
namespace that is available via the Java CLASSPATH, which includes your
project's main :source-path by default.

When a crossover namespace is provided by your current project (either via the
main :source-dir or one of the :extra-classpath-dirs in your project.clj file),
the files that make up that namespace (recursively) will be monitored for changes,
and will be copied to the ClojureScript project whenever modified.

Crossover namespaces provided by jar files cannot be searched recursively, and
thus must be specified on a per-file basis.  They are copied over once, when
lein-cljsbuild begins compilation, and are not monitored for changes.

Of course, remember that since the namespace will be used by both Clojure
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

```clj
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "0.1.0"]]
  :source-path "src-clj"
  :cljsbuild {
    :builds [{
      :source-path "src-cljs"
      ; Each entry in the :crossovers vector describes a Clojure namespace
      ; that is meant to be used with the ClojureScript code as well.
      ; The files that make up this namespace will be automatically copied
      ; into the ClojureScript source path whenever they are modified.
      :crossovers [example.crossover]
      :compiler {
        :output-to "war/javascripts/main.js"  ; default: main.js in current directory
        :optimizations :whitespace
        :pretty-print true}}]})
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

```clj
;*CLJSBUILD-MACRO-FILE*;
```

This tells lein-cljsbuild to refrain from copying the `.clj` files
into the ClojureScript directory.  This step can be skipped if the
macro file is not included in any of the crossover namespaces.

### 3. Use Black Magic to Require Macros Specially

In any crossover Clojure file, lein-cljsbuild will automatically erase the
following string (if it appears):

```clj
;*CLJSBUILD-REMOVE*;
```

This magic can be used to generate a `ns` statement that will work in both
Clojure and ClojureScript:

```clj
(ns example.crossover.some_stuff
  (:require;*CLJSBUILD-REMOVE*;-macros
    [example.crossover.macros :as macros]))
```

Thus, after removing comments, Clojure will see:

```clj
(ns example.crossover.some_stuff
  (:require
    [example.crossover.macros :as macros]))
```

However, lein-cljsbuild will remove the `;*CLJSBUILD-REMOVE*;` string entirely,
before copying the file.  Thus, ClojureScript will see:

```clj
(ns example.crossover.some_stuff
  (:require-macros
    [example.crossover.macros :as macros]))
```

And thus the macros can be shared.

##  License

Source Copyright © Evan Mezeske, 2011-2012.
Released under the Eclipse Public License - v 1.0.
See the file COPYING.

## Contributors

* Evan Mezeske **(Author)** (evan@mezeske.com)
* Shantanu Kumar (kumar.shantanu@gmail.com)
* Luke VanderHart (http://github.com/levand)
