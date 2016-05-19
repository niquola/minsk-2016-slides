(ns reagento.web
  (:require
   [org.httpkit.server :as http]
   [route-map.core :as rm]
   [hiccup.page :refer [include-js include-css html5]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [ring.middleware.resource :as rmr]
   [prone.middleware :refer [wrap-exceptions]]
   [ring.middleware.reload :refer [wrap-reload]]
   [reagento.formats :as sf]
   [config.core :refer [env]])
  (:gen-class))

(defn home-page [req]
  {:headers {"content-type" "text/html"}
   :body (html5
          [:head
           [:meta {:charset "utf-8"}]
           (include-css "//maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css")
           [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]]
          [:body [:div#app "loading"]
           (include-js "/js/out/goog/base.js")
           (include-js "/js/app.js")
           [:script {:type "text/javascript"} "console.log('here'); goog.require('reagento.dev');"]])})


(defonce clients (atom #{}))

(defn broad-cast [msg]
  (doseq [ch @clients]
    (http/send! ch msg)))

(defn $eval [req]
  (http/with-channel req ch
    (println "Connection: " ch)
    (swap! clients conj ch)
    (http/on-receive ch (fn [msg] (broad-cast msg)))
    (http/on-close ch (fn [st] (swap! clients disj ch)))))

(def routes {:GET #'home-page
             "channel" {:GET #'$eval}})

(defn notfound [meth uri]
  {:status  404
   :headers {"Content-Type" "text"}
   :body    (str " Not found " meth " " uri)})

(defn dispatch [{meth :request-method uri :uri :as req}]
  (if-let [rt (rm/match [meth uri] routes)]
    ((:match rt) req)
    (notfound meth uri)))

(def defaults (merge site-defaults {:security {:anti-forgery false}}))

(defn process-body [req]
  (if (and
       (or (= (get-in req [:headers "content-type"]) "application/transit+json"))
       (not (nil? (:body req))))
    (assoc req :data (sf/from-transit (:body req)))
    req))

(defn wrap-magic-format [h]
  (fn [req]
    (let [req (process-body req)
          res (h req)]
      (if (coll? (:body res))
        (-> res
            (update-in [:body] sf/to-transit)
            (update-in [:headers "Content-Type"] (constantly "application/transit+json")))
        res))))

(def app
  (let [handler (-> #'dispatch
                    (wrap-magic-format)
                    (wrap-defaults defaults)
                    (rmr/wrap-resource "public"))]
    (if (env :dev) (-> handler wrap-exceptions wrap-reload) handler)))

(def server (atom nil))

(defn start []
  (when-let [srv @server] (srv))
  (let [port (Integer/parseInt (or (env :port) "3333"))]
    (reset! server (http/run-server #'app {:port port}))))

(defn -main [] (start))

(comment
  (start)
  )
