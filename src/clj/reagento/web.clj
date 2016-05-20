(ns reagento.web
  (:require [org.httpkit.server :as http]
            [ring.middleware
             [defaults :refer [site-defaults wrap-defaults]]
             [resource :as rmr]])
  (:gen-class))

(defn app [req]
  {:headers {"Content-Type" "text/html"}
   :body "<h1>Hello, Minsk</h1>"})

(def server (atom nil))

(defn start []
  (when-let [srv @server] (srv))
  (reset! server (http/run-server #'app {:port 8080})))

(defn -main [] (start))

(comment
  (start))
