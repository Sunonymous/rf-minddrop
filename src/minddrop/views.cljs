(ns minddrop.views
  (:require
   [reagent.core    :as reagent]
   [minddrop.config :as config]
   [minddrop.events :as events]
   [minddrop.subs   :as subs]
   [minddrop.util   :as util]
   [pool.core       :as pool]
   [drop.core       :as drop]
   [re-frame.core   :as rf]))

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

;;;;;;;;;;;;;;;;;;;;
;; Action Buttons ;

(defn add-drop-btn
  [next-id source]
  [:button {:on-click
            (fn [_]
              (let [label (util/prompt-string "Drop Label:")
                    next-drop (drop/new-drop label next-id source)]
                (when (seq label)
                      (rf/dispatch [::events/add-drop next-drop]))))}
   "Add Drop"])

(defn relabel-drop-btn
  [drop-id]
  [:button {:on-click
            (fn [_]
              (let [next-label (util/prompt-string "New label?")]
                (when (seq next-label)
                  (rf/dispatch [::events/relabel-drop drop-id next-label]))))}
   "Relabel Drop"])

(defn focus-toggle-btn
  [focused-ids drop-id]
  (let [is-focused? (some #{drop-id} focused-ids)]
    [:button {:on-click #(rf/dispatch [(if is-focused?
                                         ::events/unfocus-drop
                                         ::events/focus-drop) drop-id])}
     (str (if is-focused? "Unfocus" "Focus") " Drop")]))

;;;;;;;;;;;;;;;
;; Open Drop ;

(defn open-drop-display
  []
  (let []))

;;;;;;;;;
;; App ;

(defn main-panel []
  (let [pool        (rf/subscribe [::subs/pool])
        source      (rf/subscribe [::subs/source])
        focused-ids (rf/subscribe [::subs/focused-ids])
        user        (rf/subscribe [::subs/user])
        queue-pred  (pool/pool-filters->predicate
                     {:source (if @search-by-source?
                                @source
                                nil)
                      :focused @focus-mode?
                      :untouched (not @focus-mode?)
                      :label @label-query}
                     @focused-ids)
        queue       (if @focus-mode?
                      (vec @focused-ids)
                      (pool/pool->queue queue-pred @pool))
        open-drop   (@pool (first queue))
        open-id     (when open-drop (:id open-drop))]
    [:div#minddrop
;; Source Parameters
     [:div#queue-controls
      [:div#source-details
       [:h2 (if @focus-mode?
              "( focused )"
              (:label (@pool @source)))]
       [:p (repeat (count queue) "• ")]]
      [:div#source-buttons
       [add-drop-btn (pool/next-id @pool) @source]
       (when (and
              (not @focus-mode?) ;; don't change source when focused
              (not= @source (drop/constants :master-id)))
         [:button {:on-click #(rf/dispatch [::events/enter-drop (:source (@pool @source))])}
          "Exit Drop"])]
      (when (not @focus-mode?)
        [:div
         [:input {:type "text" :placeholder "Label Contains:"
                  :on-change update-label-query!}]
         [:button {:on-click toggle-search-scope!}
          (str "Search " (if @search-by-source? "In Source" "All"))]])
      [:button {:on-click toggle-focus-mode!}
       (str "Focused Mode " (if @focus-mode? "ON" "OFF"))]]
;; Open Drop Display
     [:div#drop-display
      (if open-drop
        [:div#drop-content
         [:h3#drop-label (:label open-drop)]
         [:p#drop-notes  (:notes open-drop)]]
        [:div#no-drops
         [:p "No drop to display!"]
         (when (pool/source-needs-refresh? @source @pool)
           [:button {:on-click #(rf/dispatch [::events/refresh-source @source])}
            "Refresh Drops"])])]
;; Drop Controls
     (when open-drop
       [:div#drop-controls
        [:button {:on-click #(rf/dispatch [::events/remove-drop open-id])}
         "Delete Drop"]
        (if @focus-mode?
          (when (< 1 (count @focused-ids))
            [:button {:on-click #(rf/dispatch [::events/rotate-focused-ids])}
           "Next Drop"])
          [:button {:on-click #(rf/dispatch [::events/touch-drop open-id])}
           "Next Drop"])
        (when (not @focus-mode?)
          [:button {:on-click #(rf/dispatch [::events/enter-drop open-id])}
           "Enter Drop"])
        [focus-toggle-btn @focused-ids open-id]
        [relabel-drop-btn open-id]])
     (when config/debug?
       [:div#debug-controls
        [:button {:on-click #(rf/dispatch [::events/debug-set-pool (js/window.prompt "Set DB pool to value:")])}
         "Set DB"]
       [:button {:on-click #(rf/dispatch [::events/debug-insert-notes open-id "blah blah blah" ])}
         "Insert Notes"]])]))
