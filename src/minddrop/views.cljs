(ns minddrop.views
  (:require
   ;; Material UI
   [reagent-mui.material.text-field          :refer [text-field]]
   [reagent-mui.material.icon-button         :refer [icon-button]]
   [reagent-mui.material.card                :refer [card]]
   [reagent-mui.material.card-actions        :refer [card-actions]]
   [reagent-mui.material.accordion           :refer [accordion]]
   [reagent-mui.material.accordion-summary   :refer [accordion-summary]]
   [reagent-mui.material.accordion-details   :refer [accordion-details]]
   [reagent-mui.material.drawer              :refer [drawer]]
   [reagent-mui.material.form-control-label  :refer [form-control-label]]
   [reagent-mui.material.switch              :refer [switch]]
   [reagent-mui.material.button              :refer [button]]
   ;; MUI Icons
   [reagent-mui.icons.add                     :refer [add]]
   [reagent-mui.icons.delete                  :refer [delete]]
   [reagent-mui.icons.edit                    :refer [edit]]
   [reagent-mui.icons.expand-more             :refer [expand-more]]
   [reagent-mui.icons.visibility              :refer [visibility]]
   [reagent-mui.icons.visibility-off          :refer [visibility-off]]
   [reagent-mui.icons.visibility-off-outlined :refer [visibility-off-outlined]]
   [reagent-mui.icons.zoom-in                 :refer [zoom-in]]
   [reagent-mui.icons.zoom-out                :refer [zoom-out]]
   [reagent-mui.icons.arrow-right-alt         :refer [arrow-right-alt]]
   [reagent-mui.icons.autorenew-outlined      :refer [autorenew-outlined]]
   [reagent-mui.icons.notes                   :refer [notes]]
   [reagent-mui.icons.save                    :refer [save]]
   [reagent-mui.icons.close                   :refer [close]]
   [reagent-mui.icons.close-outlined          :refer [close-outlined]]
   [reagent-mui.icons.backspace               :refer [backspace]]
   ;; Minddrop
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

;; Pool Filters
;; Users can filter drops by label.(defonce label-query (r/atom ""))

;; These searches may be within a particular source drop or the full pool.
;; TODO these are shadowed by locals within the filter drawer... check on that!
(defonce search-by-source? (r/atom true))
(defn toggle-search-scope! [] (swap! search-by-source? not))
(defn search-local! [] (reset! search-by-source? true))

;; Open/Close Filter Drawer
(defonce filter-drawer-open? (r/atom false))
(defn toggle-filter-drawer! [] (swap! filter-drawer-open? not))

;; Are drop notes being edited?
(defonce editing-notes? (r/atom false))
(defn toggle-note-edit! [] (swap! editing-notes? not))
(defn stop-editing-notes! [] (reset! editing-notes? false))

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
    :disabled @(rf/subscribe [::subs/focus-mode])
    :aria-label "add new drop"}
   [add {:font-size "large"}]])

;; TODO why this this a function-2 component?
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

(defn drop-note-editor
  [txt save-fn]
  (let [live-notes (r/atom txt)]
    (fn [txt save-fn]
      [:div {:style {:display "flex"
                     :flex-direction "column"
                     :align-items "center"}}
       [text-field
        {:placeholder "(Notes)"
         :multiline   true
         :value       @live-notes
         :on-input    #(reset! live-notes (-> % .-target .-value))}]
       [:div {:style {:margin "0.25em" :padding "1em"
                      :border-bottom "1px solid black"}}
        [icon-button
         {:on-click (fn [_]
                      (save-fn @live-notes)
                      (toggle-note-edit!))
          :size "small"
          :aria-label "save notes"}
         [save]]
        [icon-button
         {:on-click toggle-note-edit!
          :size "small"
          :aria-label "cancel editing notes"}
         [close]]]])))

(defn drop-notes-display
  [drop-id]
  (let [drop       @(rf/subscribe [::subs/drop drop-id])
        save-notes-fn (fn [next-notes]
                        (rf/dispatch [::events/resonate-drop (drop :id) (drop/constants :notes-boost)])
                        (rf/dispatch [::events/renote-drop   (drop :id) next-notes]))]
    (if @editing-notes?
      [drop-note-editor (drop :notes) save-notes-fn]
      [:p (drop :notes)])))

;; TODO remove this unused button
(defn toggle-focused-view-button
  []
  (let [is-focused? (:focused @(rf/subscribe [::subs/view-params]))]
    [icon-button
     {:on-click #(rf/dispatch [::events/update-view-params :focused (not is-focused?)])
      :size "medium"
      :aria-label "toggle focus mode"}
     (if is-focused?
       [visibility {:font-size "large"}]
       [visibility-off-outlined {:font-size "large"}])]))

;;;;;;;;;;;;;;;;;;;;;;
;; Major Components ;

(defn filter-drawer
  "This collapsible drawer allows the user to edit
   the parameters with which drops are filtered."
  []
  (let [stop-searching! (fn [_]
                          (rf/dispatch [::events/update-view-params :source (drop/constants :master-id)])
                          (rf/dispatch [::events/update-view-params :label ""])
                          (search-local!))]
    (fn []
      [drawer {:open @filter-drawer-open? :on-close toggle-filter-drawer!}
       [:form {:style {:margin-top "auto"
                       :margin-bottom "auto"
                       :padding "4rem"}}
        [:h2  "Drop Filters"]
        [form-control-label
         {:label "Focused Drops Only"
          :control (r/as-element [switch])
          :checked (:focused @(rf/subscribe [::subs/view-params]))
          :label-placement "start"
          :on-change #(rf/dispatch [::events/update-view-params :focused (-> % .-target .-checked)])}]
        [:br]
        [button
         {:sx {:margin "1em 0 1em 0"}
          :variant "outlined"
          :on-click #(rf/dispatch [::events/unfocus-all-drops])
          :aria-label "unfocus all drops"}
         "Unfocus All Drops"]
        [:br]
        [:hr]
        [:br]
        [form-control-label
         {:label "Search All Drops"
          :control (r/as-element [switch])
          :disabled (not (seq (:label @(rf/subscribe [::subs/view-params]))))
          :checked (not @search-by-source?)
          :label-placement "start"
          :on-change (fn [_]
                       (toggle-search-scope!)
                       (rf/dispatch [::events/update-view-params :source (if @search-by-source?
                                                                           (drop/constants :master-id)
                                                                           nil)]))}]
        [:br] ;; TODO the components on both sides of this BR are extremely coupled
        [:div {:style {:display "flex" :align-items "center"}}
         [text-field
          {:label "Drop Label Contains:"
           :size  "small"
           :sx {:margin-top "0.5em"}
           :value (:label @(rf/subscribe [::subs/view-params]))
           :on-change (fn [e]
                        (let [next-val (-> e .-target .-value)]
                          (rf/dispatch [::events/update-view-params :label next-val])
                          (when (not (seq next-val))
                            (stop-searching! e))))}]
         [button ;; nudge the button into place
          {:style {:position "relative" :top "0.2em"}
           :on-click stop-searching!
           :aria-label "clear search"}
          [backspace]]]]
       [icon-button
        {:sx {:margin-inline "auto"
              :margin-bottom "24px"
              :width "fit-content"}
         :on-click toggle-filter-drawer!}
        [close-outlined]]])))

(defn no-drop-available
  "Displays when no drop is visible for interaction."
  []
  (let [source @(rf/subscribe [::subs/source])]
    [:div#no-drops
     (cond
       (pool/source-needs-refresh? source @(rf/subscribe [::subs/pool]))
       [icon-button
        {:on-click #(rf/dispatch [::events/refresh-source source])
         :aria-label "refresh drops in source"}
        [autorenew-outlined {:font-size "large"}]]

       (@(rf/subscribe [::subs/view-params]) :focused)
       (if (pos? (count @(rf/subscribe [::subs/focused-drops])))
         [icon-button
          {:on-click #(rf/dispatch [::events/refresh-focused-drops])
           :aria-label "refresh focused drops"}
          [autorenew-outlined {:font-size "large"}]]
         [:p "No drops are focused or nothing matches your search."])

       :otherwise
       [:p "No drop to display!"])]))

(defn open-drop-card
  []
  (let [drop @(rf/subscribe [::subs/drop @(rf/subscribe [::subs/first-in-queue])])]
      [card
       {:variant "outlined"
        :sx {:min-width "275px"
             :max-width "75%"}}
       [accordion
        [accordion-summary
         {:expand-icon (r/as-element [expand-more])}
         [:h4 (str (:label drop))]]
        [accordion-details
         [drop-notes-display (:id drop)]]
        [card-actions
         [delete-drop-btn (:id drop)]
         [icon-button
          {:on-click (fn [_]
                       (rf/dispatch [::events/toggle-drop-focus (drop :id)])
                       (rf/dispatch [::events/resonate-drop (drop :id) (drop/constants :focus-boost)]))
           :size "small"}
          (if (:focused drop)
            [visibility]
            [visibility-off-outlined])]
         [icon-button
          {:on-click (fn [_]
                       (let [next-label (util/prompt-string "New label?")]
                         (when (seq next-label)
                           (rf/dispatch [::events/relabel-drop (:id drop) next-label]))))
           :size "small"}
          [edit]]
         (when (not @editing-notes?)
           [icon-button
            {:on-click toggle-note-edit!
             :size "small"}
            (if @editing-notes?
              [save]
              [notes])])
         ]]]))

(defn navigation-controls
  "Allows the user to navigate their pool of drops
   or add a new drop."
  []
  (let [source     @(rf/subscribe [::subs/source])
        drop-id    @(rf/subscribe [::subs/first-in-queue])]
    [:div#navigation_controls
     [icon-button
      {:on-click #(rf/dispatch [::events/update-view-params :source (:source (@(rf/subscribe [::subs/pool]) source))])
       :disabled (or
                  @(rf/subscribe [::subs/focus-mode])
                  (= source (drop/constants :master-id)))
       :aria-label "exit drop"}
      [zoom-out {:font-size "large"}]]
     [add-drop-btn @(rf/subscribe [::subs/next-id]) source]
     [icon-button
      {:on-click (fn [] (stop-editing-notes!)
                   (rf/dispatch [::events/touch-drop drop-id]))
       :disabled (not drop-id)
       :aria-label "skip to next drop"}
      [arrow-right-alt {:font-size "large"}]]
     [icon-button {:on-click #(rf/dispatch [::events/update-view-params :source drop-id])
                   :size "large"
                   :disabled (or
                              (not drop-id)
                              @(rf/subscribe [::subs/focus-mode]))
                   :aria-label "enter drop"}
      [zoom-in {:font-size "large"}]]]))

(defn banner-source
  [source-id]
  [:h2 (let [focused?  @(rf/subscribe [::subs/focus-mode])
             searching? (seq (:label @(rf/subscribe [::subs/view-params])))
             global-search? (nil? source-id)]
         (cond
           (and focused? searching?) "Focused Drop Search"
           focused?                  "Focused Drops"
           searching?                "Search Results"
           global-search?            "Global Search"
           :otherwise                (:label @(rf/subscribe [::subs/drop source-id]))))])

(defn position-banner
  "Displays the current position in the pool.
   Shows the source, the open drop, and the
   number of drops both in the queue and nested
   inside the open drop."
  []
  (let [source     @(rf/subscribe [::subs/source])
        drop-id    @(rf/subscribe [::subs/first-in-queue])
        child-count (count (pool/immediate-children drop-id @(rf/subscribe [::subs/pool])))]
    [:div#position_banner
     [banner-source source]
     [:h4.drop-indicator
      {:style {:font-size "1rem"}}
      (when drop-id (apply str "⟶  " (repeat (count @(rf/subscribe [::subs/queue])) "🌢 ")))
      (:label @(rf/subscribe [::subs/drop drop-id]))
      (when (pos? child-count) (apply str " ⟶  " (repeat child-count "• ")))]]))

;;;;;;;;;
;; App ;

(defn minddrop []
  (let [drop-id     @(rf/subscribe [::subs/first-in-queue])]
    [:div#minddrop
     [:div#queue-controls
      [:button#filter_drawer_button.clean {:on-click toggle-filter-drawer!} "➤"]
      [filter-drawer]]
     [:div#drop-display
      (if drop-id
        [open-drop-card]
        [no-drop-available])]
     [navigation-controls]
     [position-banner]]))
