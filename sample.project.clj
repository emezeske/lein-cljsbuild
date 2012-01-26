; This is an annotated example of the lein-cljsbuild options that
; may be set in a project.clj file. It is a fairly contrived example
; in order to cover all options exhaustively; it shouldn't be considered
; a representative configuration. For complete, working examples, look in
; the example-projects/ folder.

(defproject org.example/sample "1.0.0-SNAPSHOT"
  ; Your project must use Clojure 1.3 or above to support
  ; ClojureScript compilation.
  :dependencies [[org.clojure/clojure "1.3.0"]]
  ; Your project should depend on lein-cljsbuild, to ensure that
  ; the right version of the plugin is installed.
  :dev-dependencies [[lein-cljsbuild "0.0.9"]]
  ; The standard Leiningen :source-path option is used by lein-cljsbuild
  ; to determine the source directory from which crossover files will
  ; be copied.  Leiningen defaults to "src".
  :source-path "src-clj"
  ; This is required for lein-cljsbuild to hook into the default Leningen
  ; tasks, e.g. the "lein compile", "lein clean", and "lein jar" tasks.
  :hooks [leiningen.cljsbuild]
  ; All lein-cljsbuild-specific configuration is under the :cljsbuild key.
  ; Note that this could also be a vector of configuration maps, in which
  ; case each map will be treated as a separate, independent, ClojureScript
  ; project.
  :cljsbuild {
    ; The path under which lein-cljsbuild will look for ClojureScript
    ; files to compile.  Defaults to "src-cljs".
    :source-path "src-cljs"
    ; A list of namespaces that should be copied from the Clojure :source-path
    ; into the ClojureScript :source-path.  See the README file's 
    ; "Sharing Code Between Clojure and Clojurescript" section for more details.
    ; Defaults to the empty vector [].
    :crossovers [example.crossover]
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
      ; compilation.  Defaults to ".clojurescript-output".
      :output-dir ".clojurescript-output" 
      ; Adds dependencies on external libraries.
      ; Defaults to the empty vector [].
      :libs ["closure/library/third_party/closure"]
      ; Adds dependencies on foreign libraries.
      ; Defaults to the empty vector [].
      :foreign-libs [[{:file "http://example.com/remote.js"
                       :provides  ["my.example"]}]]}})
