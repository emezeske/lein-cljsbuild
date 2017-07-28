# REPL Support

Lein-cljsbuild has built-in support for launching ClojureScript REPLs in a variety
of ways. Note as of Leiningen 1.x, all plugin REPL commands must be launched
via `lein trampoline` to work correctly.

## repl-rhino

The simplest REPL uses Rhino to evaluate the JavaScript that results from compiling
your ClojureScript input. This REPL does not have access to your project's namespaces,
and is not evaluated in a browser context. It is, however, useful for running simple
pure-ClojureScript commands. It is also the only REPL option that does not require
your application to provide additional support code:

    $ lein trampoline cljsbuild repl-rhino

## repl-listen

The next REPL is more sophisticated. With support from your application, it is possible
to use `repl-listen` to run a REPL with access to your application's ClojureScript namespaces.
The REPL may be started as follows:

    $ lein trampoline cljsbuild repl-listen

This will open a REPL, which will listen on a TCP port for a ClojureScript application
to connect to it. By default, it will listen on port `9000`, although this may be changed
via the `:repl-listen-port` option. Until a connection is received, the REPL will not be
usable.

From your application, you can connect to the REPL from your Clojurescript code with code such as this:

```clj
(ns lein-cljsbuild-example.repl
   (:require [clojure.browser.repl :as repl]))
; Use of "localhost" will only work for local development.
; Change the port to match the :repl-listen-port.
(repl/connect "http://localhost:9000/repl")
```

For instance, you might include the above call to `repl/connect` in the code for
a particular web page served by your application. So, you would launch your application,
launch `repl-listen`, and then browse to that page. It would then connect to the REPL,
enabling you to execute commands in the context of that page.

For more information on this approach, see the
[ClojureScript Wiki](https://github.com/clojure/clojurescript/wiki/Quick-Start).

## repl-launch

This is the most sophisticated REPL. Like `repl-listen`, `repl-launch` requires
application support code to function. The difference between `repl-listen` and `repl-launch`
is that the latter may be configured to automatically launch the browser after starting
the REPL. This REPL is launched as follows:

    $ lein trampoline cljsbuild repl-launch <launch-id>

Of course, this won't work until you've told lein-cljsbuild what to launch. Multiple
launch presets may be created, and thus the `<launch-id>` parameter is used to select
between them. To configure a launch preset, add an entry to the `:repl-launch-commands` map:

```clj
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "1.1.7"]]
  :cljsbuild {
    :repl-listen-port 9000
    :repl-launch-commands
      {"my-launch" ["firefox" "-jsconsole" "http://localhost/my-page"]})
```

With this configuration, the launch preset is identified by `my-launch`. When a REPL
is started with this launch preset, `firefox -jsconsole http://localhost/my-page`
will be run in the background, and presumably the page it loads will connect to the REPL:

    $ lein trampoline cljsbuild repl-launch my-launch

Note that any additional arguments following the `<launch-id>` will be passed to the
launch command. Thus, with a configuration such as:

```clj
:repl-launch-commands
  {"my-other-launch" ["firefox" "-jsconsole"]}
```

The target URL could be selected like so:

    $ lein trampoline cljsbuild repl-launch my-other-launch http://localhost/another-page

By default, your launch command's standard output and error streams will be streamed
to the console. This can be problematic, as it can spam your console session and
disrupt things. Thus, you may want to redirect the command's output to a file.

If a keyword appears in the command vector, it and all following entries will be
treated as an option map. Currently, the only supported options are
:stdout and :stderr, which allow you to redirect the command's output to files.

```clj
:repl-launch-commands
  {"my-launch" ["firefox" :stdout "my-stdout.txt" :stderr "my-stderr"]}
```

For more ideas on how to use `repl-launch`, take a look at the
[advanced example project](https://github.com/emezeske/lein-cljsbuild/blob/1.1.7/example-projects/advanced)
It has several examples of useful launch commands, with descriptions in its README.
Note that, in particular, the possibilities with
[PhantomJS](http://www.phantomjs.org)
are very intriguing.
