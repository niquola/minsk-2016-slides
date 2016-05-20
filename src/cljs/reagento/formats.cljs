(ns reagento.formats
  (:require [cognitect.transit :as t]))

(defn from-transit [msg]
  (t/read (t/reader :json) msg))

(defn to-transit [msg]
  (t/write (t/writer :json) msg))


(comment
  (from-transit (to-transit {:a 1})))

