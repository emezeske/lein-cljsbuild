# cljsbuild-example-simple

This is an example web application that uses [lein-cljsbuild][1],
[Ring][2], and [Compojure][3].  It demonstrates the use of
lein-cljsbuild to build ClojureScript into JavaScript.

To play around with this example project, you will first need
[Leiningen][4] installed.

## Running the App

Set up and start the server like this:

    $ lein deps
    $ lein cljsbuild once
    $ lein ring server-headless 3000

Now, point your web browser at `http://localhost:3000`, and see the web app in action!

[1]: https://github.com/emezeske/lein-cljsbuild
[2]: https://github.com/mmcgrana/ring
[3]: https://github.com/weavejester/compojure
[4]: https://github.com/technomancy/leiningen
