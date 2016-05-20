(ns reagento.web
  (:require [org.httpkit.server :as http]
            [garden.core :as css]
            [garden.units :as u]
            [route-map.core :as route]
            [reagento.formats :as fmt]
            [hiccup.core :as html]
            [ring.middleware
             [defaults :refer [site-defaults wrap-defaults]]
             [resource :as rmr]])
  (:gen-class))

(defn css [href]
  [:link {:href href :rel "stylesheet" :type "text/css"}])

(defn js [href]
  [:script {:src href :type "text/javascript"}])

(defn style [grd]
  [:style (css/css grd)])


(defn layout [cnt]
  (html/html
   [:html
    [:head
     (css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css")
     (style [:body {:padding (u/px 15)}])]
    [:body
     [:div#app]
     (js "/js/out/goog/base.js")
     (js "/js/app.js")
     [:script {:type "text/javascript"} "console.log('init'); goog.require('reagento.dev');"]]]))

(defn $index [req]
  {:headers {"Content-Type" "text/html"}
   :body (layout (pr-str req))})

(defn $data [req]
  {:headers {"Content-Type" "text/html"}
   :body "data"})


(defonce clients (atom #{}))

(defn broad-cast [msg]
  (doseq [cl @clients]
    (try (http/send! cl msg))))

(defn eval-clj [str]
  (with-out-str
    (try
      (println (clojure.core/eval (read-string str)))
      (catch Exception e
        (println (pr-str e))))))

(comment
  (broad-cast (fmt/to-transit {:any #inst"1980-01-01"}))
  )

(defn on-receive [msg]
  (let [msg (fmt/from-transit msg)]
    (broad-cast (fmt/to-transit (merge msg {:id (str (gensym))
                                            :result (eval-clj (:expr msg))})))))

(defn $eval [req]
  (http/with-channel req ch
    (println "Incomming connection: " ch)
    (swap! clients conj ch)
    (http/on-receive ch (fn [msg] (on-receive msg)))
    (http/on-close ch (fn [_] (swap! clients disj ch)))))

(def routes {:GET #'$index
             "eval" {:GET #'$eval}
             "data" {:GET #'$data}})


(defn dispatch [{uri :uri meth :request-method :as req}]
  (if-let [r  (route/match [meth uri] routes)]
    ((:match r) req)
    {:headers {"Content-Type" "text/html"}
     :status 404
     :body (str "Page " meth " " uri " not found")}))

(def app (-> dispatch
             (wrap-defaults site-defaults)
             (rmr/wrap-resource "public")))

(def server (atom nil))

(defn start []
  (when-let [srv @server] (srv))
  (reset! server (http/run-server #'app {:port 8080})))

(defn -main [] (start))

(comment
  (start))
