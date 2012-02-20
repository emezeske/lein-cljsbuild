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
  :plugins [[lein-cljsbuild "0.1.0"]]
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
    ; When you run something like:
    ;     $ lein trampoline cljsbuild repl-launch <id> [args...]
    ; The <id> will be used to look up a command to run to connect to the REPL.
    ; Any args past <id> will be appended to the command.
    ; Defaults to an empty map.
    :repl-launch-commands
      {"firefox" ["firefox"]
       "firefox-naked" ["firefox" "resources/public/html/naked.html"]
       "phantom" ["phantomjs" "phantom/page-repl.js"]
       "phantom-naked" ["phantomjs" "phantom/page-repl.js" "resources/public/html/naked.html"]}
    ; TODO: Document
    :test-commands
      {"unit" ["phantomjs" "phantom/unit-test.js" "resources/private/html/unit-test.html"]}
    ; TODO Fix the below description.
    ; A list of namespaces that should be copied from the Clojure :source-path
    ; into the ClojureScript :source-path.  See the README file's
    ; "Sharing Code Between Clojure and Clojurescript" section for more details.
    ; Defaults to the empty vector [].
    :crossovers [example.crossover]
    ; TODO: Document :crossover-jar once that's added.
    ; :crossover-jar true
    ; :crossover-path true
    ; The :builds option should be set to a sequence of maps.  Each
    ; map will be treated as a separate, independent, ClojureScript
    ; compiler configuration
    :builds [{
      ; The path under which lein-cljsbuild will look for ClojureScript
      ; files to compile.  Defaults to "src-cljs".
      :source-path "src-cljs"
      ; Set this key to make lein-cljsbuild hook into the "lein jar" task, and
      ; add the ClojureScript files to the jar that is created.
      :jar true
      ; The :compiler options are passed directly to the ClojureScript compiler.
      :compiler {
        ; The path to the JavaScript file that will be output.
        ; Defaults to "main.js".
        :output-to "resources/public/js/main.js"
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
        :foreign-libs [[{:file "http://example.com/remote.js"
                         :provides  ["my.example"]}]]}}]})
