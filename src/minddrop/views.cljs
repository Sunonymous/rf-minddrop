(ns minddrop.views
  (:require
   [goog.functions  :refer [debounce]]
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

(defn delete-drop-btn
  "Because deleting a drop can have more serious effects, this
   component needs to be passed additional data. It removes all
   inner drops as well, and then moves back to the master source
   if it happened to remove the active source drop."
  [drop-id source pool]
  [:button {:on-click (fn [_] (let [dependents (pool/dependent-drops @pool drop-id)
                                          confirmation-msg (str "Are you sure you want to delete this drop?\n"
                                                                (when (seq dependents)
                                                                  (str (count dependents) " inner drop(s) will be deleted.")))
                                          confirmed? (js/confirm confirmation-msg)]
                                      (when confirmed?
                                        (doseq [id (conj dependents drop-id)]
                                          (rf/dispatch [::events/remove-drop id]))
                                        (when (nil? (@pool @source)) ;; move back to master if necessary
                                          (rf/dispatch [::events/enter-drop (drop/constants :master-id)])))))}
         "Delete Drop"])

;;;;;;;;;;;;;;;
;; Open Drop ;

(defn open-drop-display
  [drop-id]
  (let [pool        (rf/subscribe [::subs/pool])
        notes-open? (reagent/atom false)
        live-notes  (reagent/atom (get-in @pool [drop-id :notes]))
        save-fn     (debounce (fn [_] (rf/dispatch-sync [::events/renote-drop drop-id @live-notes])
                                (js/console.log "Notes Saved!")) 2500)]
    (fn [drop-id]
      (let [drop (@pool drop-id)
            child-count (count (pool/immediate-children drop-id @pool))]
        [:div#drop-content
         [:div#drop-header
          [:h3#drop-label (:label drop)]
          (when (and
                 (not @focus-mode?)
                 (seq (drop :notes)))
            [:button#drop-note-toggle.clean
             {:on-click #(swap! notes-open? not)}
             (if @notes-open? "v" "...")])
          (when (and
                 (not @focus-mode?)
                 (pos? child-count))
             [:p.drop-indicator (repeat child-count "🌢 ")])]
         (if @focus-mode?
           [:textarea#drop-notes-editable
            {:value @live-notes
             :on-input (fn [e] (save-fn)
                         (reset! live-notes (-> e .-target .-value)))}]
           (when @notes-open?
             [:p#drop-notes  (:notes drop)]))
         ]))))

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
       [:p.drop-indicator (repeat (count queue) "🌢 ")]]
      [:div#source-buttons
       (when (not @focus-mode?)
         [add-drop-btn (pool/next-id @pool) @source])
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
        [open-drop-display open-id]
        [:div#no-drops
         [:p "No drop to display!"]
         (when (pool/source-needs-refresh? @source @pool)
           [:button {:on-click #(rf/dispatch [::events/refresh-source @source])}
            "Refresh Drops"])])]
;; Drop Controls
     (when open-drop
       [:div#drop-controls
        [delete-drop-btn open-id source pool]
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
        [relabel-drop-btn open-id]])]))
