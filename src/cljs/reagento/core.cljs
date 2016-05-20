(ns reagento.core
  (:require [reagent.core :as reagent :refer [atom]]))


(defn $index []
  [:h1 "Hello, Minsk"])

(defonce current-page (atom #'$index))

(defn $current-page []
  [:div [@current-page]])

(defn mount-root []
  (reagent/render [$current-page]
                  (.getElementById js/document "app")))

(defn init! []
  (.addEventListener js/window "hashchange" #(dispatch %))
  (mount-root))

