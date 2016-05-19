(ns reagento.tutor)

(comment "Primitives")

(type 1)

(type "hello")

(type #inst"1980-03-05")

(comment "Compound Types")

(type :key)

(type [1 2 3])

(type {:a 1 :b 2})

(type #{"a" "b" "c"})

(def a [1 2 3])

(comment "Composition")

(defn myfn [x]
  (* x x))

(* "a" 1)

(myfn 4)

(comment "Interop")

(js->clj (.. js/window -location))

(comment
  (js/alert "Hello")
  )
