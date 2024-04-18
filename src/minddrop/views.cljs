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
   [reagent-mui.material.snackbar            :refer [snackbar]]
   [reagent-mui.material.snackbar-content    :refer [snackbar-content]]
   ;; MUI Icons
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
   ;; Minddrop
   [modals.views    :as modals]
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

;; Are drop notes being edited?
(defonce editing-notes? (r/atom false))
(defn toggle-note-edit! [] (swap! editing-notes? not))
(defn stop-editing-notes! [] (reset! editing-notes? false))

;;;;;;;;;;;
;; Toast ;

(defn toast [message]
  (let [open? (r/atom false)
        handle-close! #(reset! open? false)]
    (fn [message]
      [:<>
       [snackbar
        {:open   @open?
         :auto-hide-duration 1500
         :on-close handle-close!
         :message message
         :action (r/as-element
                  [icon-button
                   {:size "small"
                    :aria-label "close"
                    :color "inherit"
                    :on-click handle-close!}
                   [close {:font-size "small"}]])}
        ]])))

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
        {:auto-focus  true
         :placeholder "(Notes)"
         :multiline   true
         :max-rows    20
         :full-width  true
         :value       @live-notes
         :input-props {:max-length 500}
         :on-input    #(reset! live-notes (-> % .-target .-value))}]
       [:div {:style {:margin "0.25em" :padding "1em"
                      :border-bottom "1px solid black"}}
        [icon-button
         {:on-click (fn [_]
                      (if (config/of :confirm-before-action)
                        (when (js/confirm "Save changes?")
                          (save-fn @live-notes)
                          (toggle-note-edit!))
                        (do
                          (save-fn @live-notes)
                          (toggle-note-edit!)))
                      )
          :size "small"
          :aria-label "save notes"
          :sx {:margin-right "1.25em"}}
         [save]]
        [icon-button
         {:on-click (fn [_]
                      (if (config/of :confirm-before-action)
                        (when (js/confirm "Discard changes?")
                          (toggle-note-edit!))
                        (toggle-note-edit!))
                      )
          :size "small"
          :aria-label "cancel editing notes"
          :sx {:margin-left "1.25em"}}
         [close]]]])))

(defn drop-notes-display
  [drop-id]
  (let [drop       @(rf/subscribe [::subs/drop drop-id])
        save-notes-fn (fn [next-notes]
                        (rf/dispatch [::events/resonate-drop (drop :id) (drop/constants :notes-boost)])
                        (rf/dispatch [::events/renote-drop   (drop :id) next-notes]))]
    (if @editing-notes?
      [drop-note-editor (drop :notes) save-notes-fn]
      [:p {:style {:white-space "pre-wrap"}} (drop :notes)])))

;;;;;;;;;;;;;;;;;;;;;;
;; Major Components ;

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
         [:p "No drops are focused."])
       :otherwise
       [:p "No drop to display!"])]))

(defn open-drop-card
  []
  (let [drop @(rf/subscribe [::subs/drop @(rf/subscribe [::subs/first-in-queue])])]
      [card
       {:variant "outlined"
        :sx {:min-width "342px"
             :max-width "75%"}}
       [accordion
        [accordion-summary
         {:expand-icon (r/as-element [expand-more])}
         [:h4 (str (:label drop))]]
        [accordion-details
         [drop-notes-display (:id drop)]]
        [card-actions
         [modals/delete-drop-dialog]
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
                       (let [next-label (util/prompt-string "New label?" (if (config/of :prefill-relabel)
                                                                           (:label drop)
                                                                           ""))]
                         (when (seq next-label)
                           (rf/dispatch [::events/relabel-drop (:id drop) next-label]))))
           :size "small"}
          [edit]]
         (when (not @editing-notes?)
           [icon-button
            {:on-click toggle-note-edit!
             :size "small"}
            [notes]])
         [modals/drop-tag-dialog]
         [modals/move-drop-dialog]
         ]]]))

(defn navigation-controls
  "Allows the user to navigate their pool of drops
   or add a new drop."
  []
  (let [priority-id @(rf/subscribe [::subs/priority-id])
        source     (if priority-id
                     (@(rf/subscribe [::subs/drop priority-id]) :source)
                     @(rf/subscribe [::subs/source]))
        drop-id    @(rf/subscribe [::subs/first-in-queue])]
     [:div#navigation_controls
      [icon-button
       {:on-click (fn []
                    (rf/dispatch [::events/discard-prioritized-id])
                    (rf/dispatch [::events/update-view-params :focused
                                  (not @(rf/subscribe [::subs/focus-mode]))]))
        :aria-label "view focused drops"
        :sx {:margin-left "auto"}}
       (if @(rf/subscribe [::subs/focus-mode])
         [visibility     {:font-size "large"}]
         [visibility-off {:font-size "large"}])]
      [icon-button
       {:on-click (fn [_]
                    (rf/dispatch [::events/update-view-params :source
                                  (:source @(rf/subscribe [::subs/drop source]))])
                    (rf/dispatch [::events/discard-prioritized-id]))
        :disabled (or
                   @(rf/subscribe [::subs/focus-mode])
                   (= source (drop/constants :master-id)))
        :aria-label "exit drop"}
       [zoom-out {:font-size "large"}]]
      [modals/add-drop-dialog]
      [icon-button
       {:on-click (fn []
                    (stop-editing-notes!)
                    (rf/dispatch [::events/touch-drop drop-id])      ;; needs to go first, as the
                    (rf/dispatch [::events/discard-prioritized-id])) ;; queue is reset immediately
        :disabled (not drop-id)
        :aria-label "skip to next drop"}
       [arrow-right-alt {:font-size "large"}]]
      [icon-button {:on-click (fn [_]
                                (rf/dispatch [::events/update-view-params :source drop-id])
                                (rf/dispatch [::events/discard-prioritized-id])
                                (rf/dispatch [::events/update-view-params :focused false]))
                    :size       "large"
                    :disabled   (not drop-id)
                    :aria-label "enter drop"}
       [zoom-in {:font-size "large"}]]
      [modals/jump-to-drop-dialog]]))

(defn drop-banner [drop-id level]
  (let [children @(rf/subscribe [::subs/immediate-children drop-id])
        sorted-children (group-by (fn [[_ drop]] (drop :touched)) children)]
    [:div.drop_banner_wrapper
     [(get [:h3 :h4] level) {:style {:margin-left     (str (* level 0.5) "em")
                                     :text-decoration (get ["underline" "none"] level)}}
      (str (cond
             (pos? level) "↳ "
             (= drop-id (drop/constants :master-id)) "★ ")
           (:label @(rf/subscribe [::subs/drop drop-id])))]
     (when (seq children)
       [:div.drop_indicator_wrapper {:style {:opacity (nth [1 0.25] level)}}
        [:p.drop_indicator         (repeat (count (sorted-children false)) "•")]
        [:p.touched_drop_indicator (repeat (count (sorted-children true))  "◦")]])]))

;;;;;;;;;
;; App ;

(defn minddrop []
  (let [drop-id      @(rf/subscribe [::subs/first-in-queue])
        real-source (or ;; when a drop is open, show its source. otherwise source is in view params
                     (:source @(rf/subscribe [::subs/drop drop-id]))
                     @(rf/subscribe [::subs/source]))]
    [:div#minddrop
     [modals/settings-drawer]
     [:div
      [drop-banner real-source 0]
      (when drop-id
        [drop-banner drop-id 1])]
     [:div#drop-display
      (if drop-id
        [open-drop-card]
        [no-drop-available])]
     [navigation-controls]]))
