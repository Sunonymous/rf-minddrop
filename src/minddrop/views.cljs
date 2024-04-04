(ns minddrop.views
  (:require
   ;; Material UI
   [reagent-mui.material.text-field        :refer [text-field]]
   [reagent-mui.material.icon-button       :refer [icon-button]]
   [reagent-mui.material.card              :refer [card]]
   [reagent-mui.material.card-header       :refer [card-header]]
   [reagent-mui.material.card-content      :refer [card-content]]
   [reagent-mui.material.card-actions      :refer [card-actions]]
   [reagent-mui.material.accordion         :refer [accordion]]
   [reagent-mui.material.accordion-summary :refer [accordion-summary]]
   [reagent-mui.material.accordion-details :refer [accordion-details]]
   ;; MUI Icons
   [reagent-mui.icons.add                 :refer [add]]
   [reagent-mui.icons.delete              :refer [delete]]
   [reagent-mui.icons.edit                :refer [edit]]
   [reagent-mui.icons.expand-more         :refer [expand-more]]
   [reagent-mui.icons.visibility          :refer [visibility]]
   [reagent-mui.icons.visibility-off      :refer [visibility-off]]
   [reagent-mui.icons.visibility-off-outlined      :refer [visibility-off-outlined]]
   [reagent-mui.icons.zoom-in             :refer [zoom-in]]
   [reagent-mui.icons.zoom-out            :refer [zoom-out]]
   [reagent-mui.icons.arrow-right-alt     :refer [arrow-right-alt]]
   [reagent-mui.icons.autorenew-outlined  :refer [autorenew-outlined]]
   ;; Minddrop
   [goog.functions  :refer [debounce]]
   [minddrop.config :as config]
   [minddrop.events :as events]
   [minddrop.subs   :as subs]
   [minddrop.util   :as util]
   [pool.core       :as pool]
   [drop.core       :as drop]
   [re-frame.core   :as rf]
   [reagent.core :as r]))

;;;;;;;;;;;;;;;;;
;; Local State ;

;; Focus mode shows users only the drops that are currently focused.
;; Because this is local state, it resets upon reload.
(defonce focus-mode? (r/atom false))
(defn toggle-focus-mode! [] (swap! focus-mode? not))

;; Pool Filters
;; Users can filter drops by label.
(defonce label-query (r/atom ""))
(defn update-label-query! [event]
  (reset! label-query (-> event .-target .-value)))

;; These searches may be within a particular source drop or the full pool.
(defonce search-by-source? (r/atom true))
(defn toggle-search-scope! [] (swap! search-by-source? not))

;;;;;;;;;;;;;;;;;;;;
;; Action Buttons ;

(defn add-drop-btn
  [next-id source]
  [icon-button
   {:on-click (fn [_]
                (let [label (util/prompt-string "Drop Label:")
                      next-drop (drop/new-drop label next-id source)]
                  (when (seq label)
                    (rf/dispatch [::events/add-drop next-drop]))))
    :aria-label "add new drop"}
   [add {:font-size "large"}]])

(defn delete-drop-btn
  [drop-id]
  (let [pool   (rf/subscribe [::subs/pool])
        source (rf/subscribe [::subs/source])]
    (fn [drop-id]
      [icon-button
       {:on-click (fn [_] (let [dependents (pool/dependent-drops @pool drop-id)
                                confirmation-msg (str "Are you sure you want to delete this drop?\n"
                                                      (when (seq dependents)
                                                        (str (count dependents) " inner drop(s) will be deleted.")))
                                confirmed? (js/confirm confirmation-msg)]
                            (when confirmed?
                              (doseq [id (conj dependents drop-id)]
                                (rf/dispatch [::events/remove-drop id]))
                              (when (nil? (@pool @source)) ;; move back to master if necessary
                                (rf/dispatch [::events/enter-drop (drop/constants :master-id)])))))
        :size "small"
        :aria-label "delete drop"}
       [delete]])))

;;;;;;;;;;;;;;;
;; Open Drop ;

(defn open-drop-display
  [drop-id is-focused?]
  (let [pool        (rf/subscribe [::subs/pool])
        focused-ids (rf/subscribe [::subs/focused-ids])
        live-notes  (r/atom (get-in @pool [drop-id :notes]))
        _ (js/console.log "drop-id given to open drop display: " drop-id)
        save-fn     (debounce (fn [_] (rf/dispatch-sync [::events/renote-drop drop-id @live-notes])
                                (js/console.log "Notes Saved!")) 2500)]
    (fn [drop-id is-focused?]
      (let [drop (@pool (if @focus-mode? (first @focused-ids) drop-id))
            child-count (count (pool/immediate-children drop-id @pool))]
        (if @focus-mode?
          ;; Focused Drop
          [card {:variant "outlined"
                 :sx {:min-width "275px"}}
           [card-header {:title (:label drop)}]
           [card-content
            [text-field
             {:placeholder "Notes"
              :multiline true
              :value @live-notes
              :on-input (fn [e] (save-fn)
                          (reset! live-notes (-> e .-target .-value)))}]]]
          ;; Unfocused Drop
          [card
           {:variant "outlined"
            :sx {:min-width "275px"
                 :max-width "75%"}}
           [accordion
            [accordion-summary
             {:expand-icon (r/as-element [expand-more])}
             (:label drop)
             [:p.drop-indicator (repeat child-count "• ")]]
            [accordion-details
             (if (seq (:notes drop))
               (:notes drop)
               [:p "(no notes)"])]
            [card-actions
             [delete-drop-btn drop-id]
             [icon-button
              {:on-click #(rf/dispatch [(if is-focused?
                                          ::events/unfocus-drop
                                          ::events/focus-drop) drop-id])
               :size "small"}
              (if is-focused?
                [visibility]
                [visibility-off-outlined])]
             [icon-button
              {:on-click (fn [_]
                           (let [next-label (util/prompt-string "New label?")]
                             (when (seq next-label)
                               (rf/dispatch [::events/relabel-drop drop-id next-label]))))
               :size "small"}
              [edit]]]]
           ])

         ))))

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
      (when (not @focus-mode?)
        [:div
         [:input {:type "text" :placeholder "Label Contains:"
                  :on-change update-label-query!}]
         [:button {:on-click toggle-search-scope!}
          (str "Search " (if @search-by-source? "In Source" "All"))]])
        [icon-button
         {:on-click toggle-focus-mode!
          :color (if @focus-mode? "primary" "secondary")
          :size "large"
          :disabled (not (seq @focused-ids))
          :aria-label "toggle focus mode"}
         (if @focus-mode?
           [visibility {:font-size "large"}]
           [visibility-off-outlined {:font-size "large"}])]]
;; Open Drop Display
     [:div#drop-display
      (if open-drop
        [open-drop-display open-id
         (some #{open-id} @focused-ids)]
        [:div#no-drops
         (if (pool/source-needs-refresh? @source @pool)
           [icon-button
            {:on-click #(rf/dispatch [::events/refresh-source @source])
             :aria-label "refresh drops"}
            [autorenew-outlined {:font-size "large"}]]
           [:p "No drop to display!"]
           )])]
;; Drop Controls
       [:div#drop-controls
        (when (not @focus-mode?) ;; don't change source when focused
          [icon-button
           {:on-click #(rf/dispatch [::events/enter-drop (:source (@pool @source))])
            :disabled (= @source (drop/constants :master-id))
            :aria-label "exit drop"}
           [zoom-out {:font-size "large"}]])
        [add-drop-btn (pool/next-id @pool) @source]
        [icon-button
         {:on-click (if @focus-mode?
                      #(rf/dispatch [::events/rotate-focused-ids])
                      #(rf/dispatch [::events/touch-drop open-id]))
          :disabled (if @focus-mode?
                      (>= 1 (count @focused-ids))
                      (not open-drop))
          :aria-label "skip to next drop"}
         [arrow-right-alt {:font-size "large"}]]
        (when (not @focus-mode?)
          [icon-button {:on-click #(rf/dispatch [::events/enter-drop open-id])
                        :size "large"
                        :disabled (not open-drop)
                        :aria-label "enter drop"}
           [zoom-in {:font-size "large"}]])]]))
