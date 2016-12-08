; This is an annotated example of the lein-cljsbuild options that
; may be set in a project.clj file. It is a fairly contrived example
; in order to cover all options exhaustively; it shouldn't be considered
; a representative configuration. For complete, working examples, look in
; the example-projects/ folder.

(defproject org.example/sample "1.0.0-SNAPSHOT"
  ; Your project must use Clojure 1.4 or above to support
  ; ClojureScript compilation.
  :dependencies [[org.clojure/clojure "1.7.0"]
                 ; Your project should specify its own dependency on
                 ; ClojureScript
                 [org.clojure/clojurescript "1.7.170"
                  :exclusions [org.apache.ant/ant]]]
  ; Your project should plugin-depend on lein-cljsbuild, to ensure that
  ; the right version of the plugin is installed.
  :plugins [[lein-cljsbuild "1.1.1"]]
  ; The standard Leiningen :source-paths option is used by lein-cljsbuild
  ; to determine the source directory from which crossover files will
  ; be copied.  Leiningen defaults to ["src"].
  :source-paths ["src-clj"]
  ; This is required for lein-cljsbuild to hook into the default Leningen
  ; tasks, e.g. the "lein compile" and "lein jar" tasks.
  :hooks [leiningen.cljsbuild]
  ; All lein-cljsbuild-specific configuration is under the :cljsbuild key.
  :cljsbuild {
    ; When using a ClojureScript REPL, this option controls what port
    ; it listens on for a browser to connect to.  Defaults to 9000.
    :repl-listen-port 9000
    ; The keys in this map identify repl-launch commands.  The values are
    ; sequences representing shell commands like [command, arg1, arg2, ...].
    ; Defaults to the empty map.
    :repl-launch-commands
      {"firefox" ["firefox"]
       "firefox-naked" ["firefox" "resources/public/html/naked.html"]
       "phantom" ["phantomjs" "phantom/page-repl.js"]
       ; If a keyword appears in the command vector, it and all following
       ; entries will be treated as an option map.  Currently, the only
       ; supported options are :stdout and :stderr, which allow you to
       ; redirect the command's output to files.
       "phantom-naked" ["phantomjs"
                        "phantom/page-repl.js"
                        "resources/public/html/naked.html"
                        :stdout ".repl-phantom-naked-out"
                        :stderr ".repl-phantom-naked-err"]}
    ; The keys in this map identify test commands.  The values are sequences
    ; representing shell commands like [command, arg1, arg2, ...].  Note that
    ; the :stdout and :stderr options work here as well.
    ; Defaults to the empty map.
    :test-commands
      {"unit" ["phantomjs" "phantom/unit-test.js" "resources/private/html/unit-test.html"]}
    ; A list of namespaces that should be copied from the Clojure classpath into
    ; the :crossover-path, with some changes for ClojureScript compatibility. See
    ; doc/CROSSOVERS.md for more details. Defaults to the empty vector [].
    :crossovers [example.crossover]
    ; The directory into which the :crossovers namespaces should be copied.
    ; Defaults to "target/cljsbuild-crossover".
    :crossover-path "target/my-crossovers"
    ; If hooks are enabled, this flag determines whether files from :crossover-path
    ; are added to the JAR file created by "lein jar".
    :crossover-jar true
    ; The :builds option should be set to a sequence of maps.  Each
    ; map will be treated as a separate, independent, ClojureScript
    ; compiler configuration.
    :builds {
      :main {
        ; The paths under which lein-cljsbuild will look for ClojureScript
        ; files to compile.  Defaults to ["src-cljs"].
        :source-paths ["src-cljs"]
        ; If hooks are enabled, this flag determines whether files from these
        ; :source-paths are added to the JAR file created by "lein jar".
        :jar true
        ; If a notify-command is specified, it will be called when compilation succeeds
        ; or fails, and a textual description of what happened will be appended as the
        ; last argument to the command.  If a more complex command needs to be constructed,
        ; the recommendation is to write a small shell script wrapper.
        ; Defaults to nil (disabled).
        :notify-command ["growlnotify" "-m"]
        ; Determines whether the temporary JavaScript files will be left in place between
        ; automatic builds.  Leaving them in place speeds up compilation because things can
        ; be built incrementally.  This probably shouldn't be disabled except for troubleshooting.
        ; Defaults to true.
        :incremental true
        ; Determines whether assertions are enabled for the ClojureScript code.  Technically, this
        ; binds clojure.core/*assert*, which is respected by the ClojureScript compiler.
        ; Defaults to true.
        :assert true
        ; The :compiler options are passed directly to the ClojureScript compiler.
        :compiler {
          ; The path to the JavaScript file that will be output.
          ; Defaults to "target/cljsbuild-main.js".
          :output-to "resources/public/js/main.js"
          ; This flag will turn on compiler warnings for references to
          ; undeclared vars, wrong function call arities, etc. Defaults to true.
          :warnings true
          ; The optimization level.  May be :none, :whitespace, :simple, or :advanced.
          ; :none is the recommended setting for development, while :advanced is the
          ; recommended setting for production, unless something prevents it (incompatible
          ; external library, bug, etc.).
          ; :none requires manual code loading and hence a separate HTML from the other options.
          ; Defaults to :none.
          :optimizations :whitespace
          ; This flag will cause all (assert x) calls to be removed during compilation
          ; Useful for production. Default is always false even in advanced compilation.
          ; Does NOT specify goog.asserts.ENABLE_ASSERTS which is different and used by
          ; the closure library.
          :elide-asserts true
          ; Determines whether the JavaScript output will be tabulated in
          ; a human-readable manner.  Defaults to true.
          :pretty-print true
          ; Determines whether comments will be output in the JavaScript that
          ; can be used to determine the original source of the compiled code.
          ; Defaults to false.
          :print-input-delimiter false
          ; If targeting nodejs add this line. Takes no other options at the moment.
          :target :nodejs
          ; See
          ; https://github.com/clojure/clojurescript/wiki/Source-maps
          :source-map "resources/public/js/main.js.map"
          ; Sets the output directory for temporary files used during
          ; compilation.  Must be unique among all :builds. Defaults to
          ; "target/cljsbuild-compiler-X" (where X is a unique integer).
          :output-dir "target/my-compiler-output-"
          ; Wrap the JavaScript output in (function(){...};)() to avoid clobbering globals.
          ; Defaults to true when using advanced compilation, false otherwise.
          :output-wrapper false
          ; Configure externs files for external libraries.
          ; Defaults to the empty vector [].
          ; For this entry, and those below, you can find a very good explanation at:
          ;     http://lukevanderhart.com/2011/09/30/using-javascript-and-clojurescript.html
          :externs ["jquery-externs.js"]
          ; Adds dependencies on external libraries.  Note that files in these directories will be
          ; watched and a rebuild will occur if they are modified.
          ; Defaults to the empty vector [].
          :libs ["closure/library/third_party/closure"]
          ; Adds dependencies on foreign libraries. Be sure that the url returns a HTTP Code 200
          ; Defaults to the empty vector [].
          :foreign-libs [{:file "http://example.com/remote.js"
                           :provides  ["my.example"]}]
          ; Prepends the contents of the given files to each output file.
          ; Defaults to the empty vector [].
          :preamble ["license.js"]
          ; Configure the input and output languages for the closure library.
          ; May be :ecmascript3, ecmascript5, or ecmascript5-strict.
          ; Defaults to ecmascript3.
          :language-in :ecmascript5
          :language-out :ecmascript5
          ; Configure warnings generated by the Closure compiler.
          :closure-warnings {:externs-validation :off}}}}})
