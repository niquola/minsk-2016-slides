(ns reagento.transport
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<!] :as async]))

(defn xhr [req] (http/request req))

(defonce connection (atom nil))

(defn open []
  (when-let [conn @connection] (.close conn))
  (let [conn (js/WebSocket. "ws://localhost:3333/channel")]
    (aset conn "onmessage" (fn [ev] (.log js/console "incommint" (.-data ev))))
    (aset js/window "connection" conn)
    (reset! connection conn)))


(defn send [msg]
  (when-let [conn @connection] (.send conn msg)))

(comment
  (go (println (:body
                (<! (xhr {:url "/data" :method "GET"})))))

  (go
    (let [req-1 (xhr {:url "/data" :method "GET"})
          req-2 (xhr {:url "/ups" :method "GET"})
          res-1 (<! req-1)
          res-2 (<! req-2)]
      (println "Both are here" res-1 res-2)))

  (go
    (println "res-1" (<! (xhr {:url "/data" :method "GET"})))
    (println "res-2" (<! (xhr {:url "/data" :method "GET"}))))

  (go
    (alt!
      (xhr {:url "/data" :method "GET"}) ([x] (println "first win" x))
      (xhr {:url "/data" :method "GET"})  ([x] (println "second win" x))))

  (open)
  (send "Hello server")
  )


