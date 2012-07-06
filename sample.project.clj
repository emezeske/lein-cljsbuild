; This is an annotated example of the lein-cljsbuild options that
; may be set in a project.clj file. It is a fairly contrived example
; in order to cover all options exhaustively; it shouldn't be considered
; a representative configuration. For complete, working examples, look in
; the example-projects/ folder.

(defproject org.example/sample "1.0.0-SNAPSHOT"
  ; Your project must use Clojure 1.3 or above to support
  ; ClojureScript compilation.
  :dependencies [[org.clojure/clojure "1.3.0"]]
  ; Your project should plugin-depend on lein-cljsbuild, to ensure that
  ; the right version of the plugin is installed.
  :plugins [[lein-cljsbuild "0.2.5"]]
  ; The standard Leiningen :source-path option is used by lein-cljsbuild
  ; to determine the source directory from which crossover files will
  ; be copied.  Leiningen defaults to "src".
  :source-path "src-clj"
  ; This is required for lein-cljsbuild to hook into the default Leningen
  ; tasks, e.g. the "lein compile", "lein clean", and "lein jar" tasks.
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
    ; Defaults to ".crossover-cljs".
    :crossover-path ".crossover-cljs"
    ; If hooks are enabled, this flag determines whether files from :crossover-path
    ; are added to the JAR file created by "lein jar".
    :crossover-jar true
    ; The :builds option should be set to a sequence of maps.  Each
    ; map will be treated as a separate, independent, ClojureScript
    ; compiler configuration.
    :builds {
      :main {
        ; The path under which lein-cljsbuild will look for ClojureScript
        ; files to compile.  Defaults to "src-cljs".
        :source-path "src-cljs"
        ; If hooks are enabled, this flag determines whether files from this
        ; :source-path are added to the JAR file created by "lein jar".
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
        ; The :compiler options are passed directly to the ClojureScript compiler.
        :compiler {
          ; The path to the JavaScript file that will be output.
          ; Defaults to "main.js".
          :output-to "resources/public/js/main.js"
          ; This flag will turn on compiler warnings for references to
          ; undeclared vars, wrong function call arities, etc. Defaults to true.
          :warnings true
          ; The optimization level.  May be :whitespace, :simple, or :advanced.
          ; Defaults to :whitespace.
          :optimizations :whitespace
          ; Determines whether the JavaScript output will be tabulated in
          ; a human-readable manner.  Defaults to true.
          :pretty-print true
          ; Determines whether comments will be output in the JavaScript that
          ; can be used to determine the original source of the compiled code.
          ; Defaults to false.
          :print-input-delimiter false
          ; Sets the output directory for temporary files used during
          ; compilation.  Must be unique among all :builds. Defaults to
          ; ".lein-cljsbuild-compiler-X" (where X is a unique integer).
          :output-dir ".clojurescript-output"
          ; Configure externs files for external libraries.
          ; Defaults to the empty vector [].
          ; For this entry, and those below, you can find a very good explanation at:
          ;     http://lukevanderhart.com/2011/09/30/using-javascript-and-clojurescript.html
          :externs ["jquery-externs.js"]
          ; Adds dependencies on external libraries.
          ; Defaults to the empty vector [].
          :libs ["closure/library/third_party/closure"]
          ; Adds dependencies on foreign libraries.
          ; Defaults to the empty vector [].
          :foreign-libs [{:file "http://example.com/remote.js"
                           :provides  ["my.example"]}]}}}})
