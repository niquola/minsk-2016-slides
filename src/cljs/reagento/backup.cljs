(ns reagento.backup
  (:require [reagent.core :as reagent :refer [atom]]
            [route-map.core :as route]
            [reagento.formats :as fmt]
            [clojure.string :as str]
            [garden.core :as css]))

(defn style [grd]
  [:style (css/css grd)])

(def state (atom {:items (list)}))

(defn eval-href []
  (-> js/window
      (.. -location -origin)
      (str/replace "http" "ws")
      (str "/eval")))

(defn init-socket []
  (let [soc (js/WebSocket. (eval-href))]
    (aset soc "onmessage" (fn [ev]
                            (let [data (fmt/from-transit (.-data ev))]
                              (println "Client recieved " (.-data ev))
                              (swap! state update-in [:items] conj data))))
    soc))

(def socket (atom (init-socket)))

(defn send [msg]
  (when-let [soc @socket]
    (.send soc msg)))

(defn eval-expr [expr]
  (send (fmt/to-transit {:id (str (gensym)) :expr expr})))

(defn $index []
  (let [on-change (fn [ev]
                    (swap! state assoc :input (.. ev -target -value)))

        on-key-press (fn [ev]
                       (when (and (= 13 (.-which ev))
                                  (.-shiftKey ev))
                         (eval-expr (:input @state))
                         (aset (.. ev -target) "value" "")
                         (.preventDefault ev)))]
    (fn []
      [:div
       (style [:body {:padding "25px"}
               [:.item {:border-bottom "1px solid #ddd" :padding "1em 0"}]
               [:#input {:width "100%"
                         :display "block"
                         :height "200px"}]])
       [:textarea {:id "input"
                   :on-change on-change
                   :on-key-press on-key-press}]
       [:pre (:input @state)]
       [:hr]
       (for [x (:items @state)]
         [:div.item {:key (:id x)}
          (pr-str x)])])))

(defonce current-page (atom #'$index))

(defn $data []
  [:div
   [:h1 "Data"]
   [:a {:href "#/"} "Home"]])

(def routes {:GET #'$index
             "data" {:GET #'$data}})

(defn current-url []
  (str/replace (.. js/window -location -hash) #"^#" ""))

(defn not-found []
  [:h3 "Page not found"])

(defn dispatch [ev]
  (if-let [route (route/match [:GET (current-url)] routes)]
    (reset! current-page (:match route))
    (reset! current-page not-found)))

(defn $current-page []
  [:div [@current-page]])

(defn mount-root []
  (reagent/render [$current-page]
                  (.getElementById js/document "app")))

(defn init! []
  (.addEventListener js/window "hashchange" #(dispatch %))
  (mount-root))
