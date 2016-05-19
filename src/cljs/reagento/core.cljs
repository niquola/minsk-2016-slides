(ns reagento.core
  (:require-macros [cljs.core.async.macros :as m :refer [go alt! go-loop]])
  (:require [reagent.core :as reagent :refer [atom]]
            [clojure.string :as str]
            [cljs.core.async :refer [<! >!] :as async]
            [garden.core :as garden]
            [route-map.core :as rm]
            [goog.events :as events]
            [goog.history.EventType :as EventType])
  (:import goog.History))

(defn style [grdn] [:style (garden/css grdn)])


(defn bind [state pth]
  (fn [ev]
    (swap! state assoc-in pth (.. ev -target -value))))


(def eval-chan (async/chan))

(def result-chan (async/chan))

(def url (-> js/window
              (.. -location -href)
              (str/replace "http:" "ws:")
              (str "channel")))

(def socket (atom nil))

(defn on-message [ev]
  (println "Recieved " (.-data ev))
  (go (>! result-chan {:id (str (gensym)) :expr (.-data ev)})))

(defn init-socket []
  (when-let [soc @socket]
    (.close soc))

  (let [soc (js/WebSocket. url)]
    (reset! socket soc)
    (set! (.-onmessage soc) #(on-message %))

    (go-loop []
      (let [expr (<! eval-chan)]
        (.send soc expr))
      (recur))))

(init-socket)


(defn $index [params]
  (let [state (atom {:items [{:id "1" :expr "1" :result "1"}]})
        submit (fn [ev]
                 (when (= 13 (.-which ev))
                   (go (>! eval-chan (:expr @state)))))]
    (go-loop []
      (swap! state update-in [:items] conj (<! result-chan))
      (recur))

    (fn []
      [:div#main
       (style [:body {:padding "20px"
                      :background-color "#f1f1f1"}
               [:#input {:width "100%"}]])
       [:textarea {:id "input"
                   :on-change (bind state [:expr])
                   :on-key-down submit}]
       (for [i (:items @state)]
         [:div.item {:key (:id i)}
          [:pre (:expr i) "=>" (:result i)]])])))

(def routes
  {:GET  #'$index})

(defonce current-page (atom #'$index))

(defn dispatch [event]
  (if-let [m (rm/match [:GET (.-token event)] routes)]
    (reset! current-page (:match m))
    (reset! current-page (fn [& args] [:h1 (str "Paget " (.-token event) " not found")]))))

(defn $current-page []
  [:div [@current-page]])

(defn mount-root []
  (reagent/render [$current-page]
                  (.getElementById js/document "app")))

(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen EventType/NAVIGATE dispatch)
    (.setEnabled true)))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))

