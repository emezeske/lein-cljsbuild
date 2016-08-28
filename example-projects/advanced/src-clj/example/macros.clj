(ns example.macros)

(defmacro reverse-eval [form]
  (reverse form))
