# Sharing Code Between Clojure >= 1.7.0-beta2 and ClojureScript >= 0.0-3255

A very simple setup is to

- put all shared code clojure files in a separate folder (without other cljs 
or clj files)
- adapt their namespace accordingly 
- change their file ending to cljc and 
- refer to the folder in the `:source-paths` settings of both the main project 
and the cljsbuild. 

You can then refer to the files via ordinary `require` from cljs and clj files.

Once you hit the need for platform specific code, please refer 
to [reader conditionals](http://dev.clojure.org/display/design/Reader+Conditionals)

# Sharing Code Between Clojure and ClojureScript (deprecated)

**cljsbuild crossovers are _deprecated_, and will be removed in v2.x.**
Please use either [reader
conditionals](http://dev.clojure.org/display/design/Reader+Conditionals)
(available in Clojure
>= 1.7.0-beta2 and ClojureScript >= 0.0-3255), or
>[cljx](http://github.com/lynaghk/cljx) to
target both Clojure and ClojureScript from the same codebase.

Sharing code with lein-cljsbuild is accomplished via the configuration
of "crossovers".  A crossover specifies a Clojure namespace, the content
of which should be copied into your ClojureScript project.  This can be any
namespace that is available via the Java CLASSPATH, which includes your
project's main :source-paths by default.

When a crossover namespace is provided by your current project (either via the
main `:source-dir` or one of the `:extra-classpath-dirs` in your project.clj file),
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
└── src-clj
    └── example
        ├── core.clj
        ├── something.clj
        └── crossover
            ├── some_stuff.clj
            └── some_other_stuff.clj
</pre>

And your `project.clj` file looks like this:

```clj
(defproject lein-cljsbuild-example "1.2.3"
  :plugins [[lein-cljsbuild "1.1.1"]]
  :source-paths ["src-clj"]
  :cljsbuild {
    ; Each entry in the :crossovers vector describes a Clojure namespace
    ; that is meant to be used with the ClojureScript code as well.
    ; The files that make up this namespace will be automatically copied
    ; into the ClojureScript source path whenever they are modified.
    :crossovers [example.crossover]
    ; Set the path into which the crossover namespaces will be copied.
    :crossover-path "crossover-cljs"
    ; Set this to true to allow the :crossover-path to be copied into
    ; the JAR file (if hooks are enabled).
    :crossover-jar false})
```

Then lein-cljsbuild would copy files from `src-clj/example/crossover`
to `crossover-cljs`, and you'd end up with this:

<pre>
├── src-clj
│   └── example
│       ├── a_file.clj
│       ├── core.clj
│       └── crossover
│           ├── some_stuff.clj
│           └── some_other_stuff.clj
└── crossover-cljs
    └── example
        └── crossover
            ├── some_stuff.cljs
            └── some_other_stuff.cljs
</pre>

Notice that the files in the `crossover-cljs` directory have had their extensions
modified so that they will be seen by the ClojureScript compiler.  The `crossover-cljs`
directory will automatically be added to the classpath for the ClojureScript compiler,
so your other ClojureScript code should be able to reference it via a regular `(ns)` form.

With this setup, you would probably want to add `crossover-cljs`
to your `.gitignore` file (or equivalent), as its contents are updated automatically
by lein-cljsbuild.

# Sharing Macros Between Clojure and ClojureScript

In ClojureScript, macros are still written in Clojure, and can not be written
in the same file as actual ClojureScript code.  Also, to use them in a ClojureScript
namespace, they must be required via `:require-macros` rather than the usual `:require`.

This makes using the crossover feature to share macros between Clojure and ClojureScript
a bit difficult, but lein-cljsbuild has some special constructs to make it possible.

Three things need to be done to use lein-cljsbuild to share macros.

## 1. Keep Macros in Separate Files

These examples assume that your project uses the  `src-clj/example/crossover`
directory, and that all of the macros are in a file called
`src-clj/example/crossover/macros.clj`.

## 2. Tell lein-cljsbuild Which Files Contain Macros

Add this magical comment to any crossover files that contain macros:

```clj
;*CLJSBUILD-MACRO-FILE*;
```

This tells lein-cljsbuild to refrain from copying the `.clj` files
into the `:crossover-path`.  This step can be skipped if the
macro file is not included in any of the crossover namespaces.

## 3. Use Black Magic to Require Macros Specially

In any crossover Clojure file, lein-cljsbuild will automatically erase the
following string when copying the Clojure file into the `:crossover-path`:

```clj
;*CLJSBUILD-REMOVE*;
```

This magic can be used to generate a `ns` statement that can successfully
reference a macro namespace from both Clojure and ClojureScript:

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
