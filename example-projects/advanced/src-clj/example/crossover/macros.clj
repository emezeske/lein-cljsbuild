;*CLJSBUILD-MACRO-FILE*;

(ns example.crossover.macros)

(defmacro reverse-eval [form]
  (reverse form))
