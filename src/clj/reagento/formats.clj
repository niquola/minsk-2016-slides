(ns reagento.formats
  (:require
   [cheshire.core :as cc]
   [clj-time.format :as tfmt]
   [clj-time.coerce :as tc]
   [cheshire.generate :as cg]
   [cognitect.transit :as transit])
   (:import (java.io ByteArrayInputStream ByteArrayOutputStream)))

(def date-to-json-formatter
  (tfmt/formatters :date-time))

(cg/add-encoder
 org.joda.time.DateTime
 (fn  [d json-generator]
   (.writeString json-generator
                 (tfmt/unparse date-to-json-formatter d))))

(defn from-json  [str]
  (cc/parse-string str keyword))

(defn to-json  [clj &  [options]]
  (cc/generate-string clj options))


(def joda-time-writer
  (transit/write-handler
   (constantly "m")
   (fn [v] (-> v tc/to-date .getTime))
      (fn [v] (-> v tc/to-date .getTime .toString))))

(def transit-opts
  {:handlers {org.joda.time.DateTime joda-time-writer}})

(defn to-transit-stream [clj & [frmt]]
  (let [out (ByteArrayOutputStream.)]
    (->  out
     (transit/writer :json transit-opts)
     (transit/write clj))
    out))

(defn to-transit [clj & [frmt]]
  (.toString (to-transit-stream clj [frmt])))

(defn from-transit [str & [frmt]]
  (-> (cond
        (string? str) (ByteArrayInputStream. (.getBytes str))
        (= (type str) ByteArrayOutputStream) (ByteArrayInputStream. (.toByteArray str))
        :else str)
      (transit/reader :json)
      (transit/read)))

(defn- transit-request? [request]
  (if-let [type (:content-type request)]
    (let [mtch (re-find #"^application/transit\+(json|msgpack)" type)]
      [(not (empty? mtch)) (keyword (second mtch))])))

(comment
  (from-transit
   (to-transit-stream {:a 1} :json))

  (from-transit
   (to-transit {:a 1} :json)))
