(ns ^:figwheel-no-load reagento.dev
  (:require [reagent.core :as reagent :refer [atom]]
            [figwheel.client :as figwheel :include-macros true]
            [reagento.core :as app]
            [reagent.session :as session]))

(enable-console-print!)

(figwheel/watch-and-reload
 :websocket-url "ws://localhost:3449/figwheel-ws"
 :jsload-callback app/mount-root)

(app/init!)
