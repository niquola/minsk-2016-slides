(ns reagento.repl
  (:require [figwheel-sidecar.repl-api :as ra]))

(defn cljs [] (ra/cljs-repl "app"))

(defn start-figwheel []
  (ra/start-figwheel!))

(defn start-fw []
  (ra/start-figwheel!)
  (cljs))

(defn stop-fw []
  (ra/stop-figwheel!))
