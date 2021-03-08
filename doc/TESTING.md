# Testing Support

Lein-cljsbuild has built-in support for running external ClojureScript test processes.
Test commands are configured via the `:test-commands` map. Each key in this map names
a test command, and each value is a vector representing a shell command to run for that
test:

```clj
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "2.0.0-SNAPSHOT"]]
  :cljsbuild {
    :test-commands
      {"my-test" ["phantomjs" "phantom/unit-test.js" "..."]})
```

This example creates one test named "my-test", that will run `phantomjs` with a couple
of arguments. The following command will run this test:

    $ lein cljsbuild test my-test

Alternately, this command will run *all* configured tests:

    $ lein cljsbuild test

## Testing with PhantomJS

For ClojureScript code that targets web browsers (as opposed to Node.js), it is often
useful to run unit tests in the context of a web browser. This allows code that has
side-effects (e.g. DOM manipulation) to be tested.

[PhantomJS](http://www.phantomjs.org) is a headless Webkit-based browser with full
JavaScript support. This means that it can do most anything you would expect a desktop
browser to do, except it does not have a GUI. PhantomJS can be automated via JavaScript,
and thus is convenient to use for running automated tests.

The [advanced example project](https://github.com/emezeske/lein-cljsbuild/blob/2.0.0-SNAPSHOT/example-projects/advanced)
contains an example of how to use PhantomJS for running ClojureScript tests. There are several
components that come together to make this work:

1. A `:builds` entry dedicated to the test code in the `test-cljs` directory. This compiles
the unit tests into JavaScript (so that it they can be run by PhantomJS).

2. A [static HTML page](https://github.com/emezeske/lein-cljsbuild/blob/2.0.0-SNAPSHOT/example-projects/advanced/resources/private/html/unit-test.html)
with a `<script>` tag that will pull in the compiled unit test code when the page is loaded.

3. A `:test-commands` entry that runs PhantomJS, passing it
[a simple script](https://github.com/emezeske/lein-cljsbuild/blob/2.0.0-SNAPSHOT/example-projects/advanced/phantom/unit-test.js)
. This script directs PhantomJS to load the static HTML page, and once it's loaded,
to call the ClojureScript unit test entry point.
