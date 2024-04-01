(ns minddrop.views
  (:require
   [re-frame.core   :as rf]
   [reagent.core    :as reagent]
   [minddrop.config :as config]
   [minddrop.events :as events]
   [minddrop.subs   :as subs]))

;;;;;;;;;;;;;;;;;
;; Local State ;

;; Focus mode shows users only the drops that are currently focused.
;; Because this is local state, it resets upon reload.
(defonce focus-mode? (reagent/atom false))
(defn toggle-focus-mode! [] (swap! focus-mode? not))

;; Pool Filters
;; Users can filter drops by label.
(defonce label-query (reagent/atom ""))
(defn update-label-query! [event]
  (reset! label-query (-> event .-target .-value)))

;; These searches may be within a particular source drop or the full pool.
;; FIXME which is a better default??
(defonce search-by-source? (reagent/atom true))
(defn toggle-search-scope! [] (swap! search-by-source? not))

;;;;;;;;;
;; App ;

(defn main-panel []
  (let [pool   (rf/subscribe [::subs/pool])
        source (rf/subscribe [::subs/source])
        user   (rf/subscribe [::subs/user])]
    [:div
;; Queue Parameters
     [:button {:on-click toggle-focus-mode!}
      (str "Focused Mode " (if @focus-mode? "ON" "OFF"))]
     [:br]
     (when (not @focus-mode?)
       [:div
        [:input {:type "text" :placeholder "Label Contains:"
                 :on-change update-label-query!}]
        [:button {:on-click toggle-search-scope!}
         (str "Search " (if @search-by-source? "In Source" "All"))]])
     [:h1 {:style {:position "absolute" :bottom 0 :right 0}}
      "Minddrop for " @user]
     (when config/debug?
       [:p (vals @pool)])
     ]))
