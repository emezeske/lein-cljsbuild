# cljsbuild-example-advanced

This is an example web application that uses [Ring][1], [Compojure][2],
and [lein-cljsbuild][3]. It demonstrates the use of lein-cljsbuild to
build ClojureScript into JavaScript.  It also shows how to share code
between Clojure and ClojureScript, including macros.

To play around with this example project, you will first need
[Leiningen][4] installed.

Set up and start the server like this:

    $ lein deps
    $ lein cljsbuild once
    $ lein ring server

[1]: https://github.com/mmcgrana/ring
[2]: https://github.com/weavejester/compojure
[3]: https://github.com/emezeske/lein-cljsbuild
[4]: https://github.com/technomancy/leiningen
