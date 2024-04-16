(ns modals.views
  (:require
   ;; Material UI
   [reagent-mui.material.dialog               :refer [dialog]]
   [reagent-mui.material.button               :refer [button]]
   [reagent-mui.material.icon-button          :refer [icon-button]]
   [reagent-mui.material.dialog-title         :refer [dialog-title]]
   [reagent-mui.material.dialog-actions       :refer [dialog-actions]]
   [reagent-mui.material.dialog-content       :refer [dialog-content]]
   [reagent-mui.material.dialog-content-text  :refer [dialog-content-text]]
   [reagent-mui.material.text-field           :refer [text-field]]
   [reagent-mui.material.select               :refer [select]]
   [reagent-mui.material.form-control         :refer [form-control]]
   [reagent-mui.material.form-control-label   :refer [form-control-label]]
   [reagent-mui.material.input-label          :refer [input-label]]
   [reagent-mui.material.menu-item            :refer [menu-item]]
   [reagent-mui.material.drawer               :refer [drawer]]
   [reagent-mui.material.switch               :refer [switch]]
   ;; MUI Icons
   [reagent-mui.icons.add                     :refer [add]]
   [reagent-mui.icons.link                    :refer [link]]
   [reagent-mui.icons.close-outlined          :refer [close-outlined]]
   [reagent-mui.icons.settings                :refer [settings]]
   [reagent-mui.icons.control-camera          :refer [control-camera]]
   [reagent-mui.icons.manage-search           :refer [manage-search]]
   [reagent-mui.icons.sell                    :refer [sell]]
   ;; Minddrop
   [clojure.string  :refer [includes? lower-case]]
   [minddrop.config :as config]
   [minddrop.events :as events]
   [minddrop.subs   :as subs]
   [minddrop.util   :as util]
   [pool.core       :as pool]
   [drop.core       :as drop :refer [constants]]
   [re-frame.core   :as rf]
   [reagent.core :as r]))

;; This file contains hiccup for particular dialogs and modals
;; that the user needs to interact with.

;; General functions for reusability
(defn open-modal!  [modal*] (reset! modal* true))
(defn close-modal! [modal*] (reset! modal* false))

(defn add-drop-dialog []
  (let [open?            (r/atom false)
        toggle-modal!    #(swap! open? not)
        new-label         (r/atom "")
        add-to-open-drop? (r/atom false)
        jump-to-drop?     (r/atom false)
        close-modal!   (fn [] (reset! open? false)
                         (reset! new-label "")
                         (reset! add-to-open-drop? false)
                         (reset! jump-to-drop? false))
        ]
    (fn []
      (let [source-id @(rf/subscribe [::subs/source])
            open-id   @(rf/subscribe [::subs/first-in-queue])
            submit-fn  (fn [e] (.preventDefault e)
                         (when (seq @new-label)
                           (let [next-source (if @add-to-open-drop? open-id source-id)
                                 next-drop (drop/new-drop
                                            @new-label
                                            @(rf/subscribe [::subs/next-id])
                                            next-source)]
                             (rf/dispatch [::events/add-drop next-drop])
                             (rf/dispatch [::events/resonate-drop next-source (constants :inner-boost)])
                             (when @jump-to-drop?
                               (rf/dispatch [::events/prioritize-drop (:id next-drop)])))
                           (close-modal!)))]
        [:<>
         [icon-button
          {:on-click toggle-modal!
           :disabled @(rf/subscribe [::subs/focus-mode])
           :aria-label "add new drop"}
          [add {:font-size "large"}]]
         [dialog
          {:open @open?
           :on-close close-modal!
           :sx {:min-width "275px"}
           :Paper-Props {:component "form"
                         :on-submit submit-fn}}
          [dialog-title "Add Drop"]
          [dialog-content
           [dialog-content-text
            (str "Drop will be added inside "
                 (:label @(rf/subscribe [::subs/drop (if @add-to-open-drop?
                                                       open-id
                                                       source-id)])) ".")
            [:br]
            "How should we label it?"]
           [text-field
            {:sx          {:margin "1em 2em"}
             :auto-focus   true
             :variant     "standard"
             :input-props {:max-length 30}
             :size        "small"
             :value       @new-label
             :placeholder "Drop Label"
             :on-change   #(reset! new-label (-> % .-target .-value))}]
           [:br]
           (when open-id
             [:div
              [form-control {:sx {:float "right"}}
               [form-control-label
                {:label (str "Add inside "
                             (:label @(rf/subscribe [::subs/drop open-id]))
                             "?")
                 :label-placement "start"
                 :disabled (nil? open-id)
                 :control (r/as-element [switch
                                         {:checked @add-to-open-drop?
                                          :on-change #(reset! add-to-open-drop? (-> % .-target .-checked))}])}]]
              [:br]
              [form-control {:sx {:float "right"}}
               [form-control-label
                {:label "Jump to drop?"
                 :label-placement "start"
                 :control (r/as-element [switch
                                         {:checked @jump-to-drop?
                                          :on-change #(reset! jump-to-drop? (-> % .-target .-checked))}])}
                ]
               ]])]
          [dialog-actions
           [button
            {:on-click   submit-fn
             :aria-label "add new drop"}
            "Add"]
           [button
            {:on-click #(close-modal!)
             :aria-label "cancel adding new drop"}
            "Close"]]]]))))

(defn drop-tag-dialog []
  (let [open?         (r/atom false)
        open-modal!  #(open-modal! open?)
        close-modal! #(close-modal! open?)
        selected-tag  (r/atom nil)
        tag-to-add    (r/atom "")]
    (fn []
      (let [drop    @(rf/subscribe [::subs/drop @(rf/subscribe [::subs/first-in-queue])])]
        [:<>
         [icon-button
          {:on-click open-modal!
           :size "small"}
          [sell]]
         [dialog
          {:open     @open?
           :on-close close-modal!}
          [dialog-title "Edit Drop Tags"]
          [dialog-content
           (when (seq (:tags drop))
             [:div
              [dialog-content-text "Your drop contains the following tags:"]
              (for [tag (:tags drop)]
                [button
                 {:key tag
                  :variant "outlined"
                  :sx    {:display "inline-block"
                          :margin  "4px"
                          :padding "0.125em 0.5em"
                          :border  "1px solid black"
                          :border-radius "8px"}
                  :on-click #(reset! selected-tag tag)
                  :aria-label (str "select link " tag)} tag])
              [:hr {:style {:margin-block "1em"}}]])
           (when @selected-tag
             [:div {:style {:margin-top "1em"}}
              [dialog-content-text
               "Selected Tag: " [:span {:style {:font-weight "bold"}} @selected-tag]]
              [button
               {:variant "contained"
                :color   "error"
                :size    "small"
                :on-click (fn [_] (rf/dispatch [::events/remove-drop-tag (:id drop) @selected-tag])
                            (reset! selected-tag nil))}
               "Remove Tag"]
              [:hr {:style {:margin-block "1em"}}]])
           [dialog-content-text "Add Tag"]
           [text-field
            {:variant "outlined"
             :size    "small"
             :on-change #(reset! tag-to-add (-> % .-target .-value))}]
           [button
            {:sx {:position "relative" :top "0.25em" :left "0.5em"}
             :variant    "outlined"
             :size       "small"
             :aria-label (str "add link " @tag-to-add " to drop")
             :on-click (fn [_] (when (seq @tag-to-add)
                                 (rf/dispatch [::events/add-drop-tag (:id drop) @tag-to-add]))
                         (reset! tag-to-add ""))}
            "Add"]
           [dialog-actions
            [button {:on-click close-modal!} "Close"]]]
          ]]))))

;; This component is an inner component used in multiple dialogs. It is
;; passed a ratom to store and modify the selected drop id.
(defn drop-selector [selected-id*]
  (let [labels      @(rf/subscribe [::subs/drop-labels])
        label-filter (r/atom "")
        ]
    (fn [selected-id*]
      (let [filtered-labels (filter
                             (fn [[id label]]
                               (and
                                (includes? (lower-case label) (lower-case @label-filter))
                                (not= id (constants :master-id)))) labels)]
        [:div {:style {:margin-block "1em"
                       :display "flex"
                       :flex-direction "column"
                       :align-items "center"}}
         ;; This next line is hacky. I struggled with getting the select
         ;; component to re-render every time the filter changed.
         [:pre {:style {:display "none"}} (str filtered-labels)]
         [:div {:style {:display "flex" :align-items "center"}}
          [text-field
           {:sx {:margin "1em 0"}
            :variant     "outlined"
            :value       @label-filter
            :size        "small"
            :placeholder "Filter Labels"
            :on-change (fn [e]
                         (reset! label-filter (-> e .-target .-value))
                         (reset! selected-id* nil))}]
          [button
           {:sx {:width "1em"}
            :on-click #(reset! label-filter "")
            :aria-label "clear label filter"
            :disabled (empty? @label-filter)
            :size "large"}
           "X"]]
         (if (empty? filtered-labels)
           [dialog-content-text "No drops match your search."]
           [form-control
            {:sx {:display "block"}
             :variant "standard"}
            [input-label {:id "select-drop-input-label"} "Choose a Drop:"]
            [select
             {:sx {:min-width "150px"}
              :label-id  "select-drop-input-label"
              :disabled  (empty? filtered-labels)
              :value     (or @selected-id* "")
              :on-change #(reset! selected-id* (-> % .-target .-value))}
             (for [[id label] filtered-labels]
               [menu-item {:key id :value id} label])]])]))))

(defn jump-to-drop-dialog []
  (let [open?         (r/atom false)
        open-modal!  #(open-modal! open?)
        selected-id   (r/atom nil)
        close-modal! (fn []
                       (reset! selected-id nil)
                       (close-modal! open?))]
    (fn []
      [:<>
       [icon-button
        {:on-click open-modal!}
        [manage-search {:font-size "large"}]]
       [dialog
        {
         :open     @open?
         :on-close close-modal!}
        [dialog-title "Jump to Drop"]
        [dialog-content
         [drop-selector selected-id]
         [dialog-actions
          [button {:on-click (fn [_] ;; arrive at drop and set source to drop's source
                               (rf/dispatch [::events/prioritize-drop @selected-id])
                               (rf/dispatch [::events/update-view-params :source
                                             (:source @(rf/subscribe [::subs/drop @selected-id]))])
                               (close-modal!))
                   :disabled (not @selected-id)} "Jump to"]
          [button {:on-click (fn [_] ;; set source to drop's id and deprioritize any prior drops
                               (rf/dispatch [::events/update-view-params :source @selected-id])
                               (rf/dispatch [::events/discard-prioritized-id])
                               (close-modal!))
                   :disabled (not @selected-id)} "Jump in"]
          [button {:on-click close-modal!} "Close"]]]]])))

(defn settings-drawer []
  (let [open?         (r/atom false)
        open-modal!  #(open-modal! open?)
        close-modal! #(close-modal! open?)]
    (fn []
      [:div
       [icon-button
        {:sx {:position "fixed" :top "2rem" :right "2rem"}
         :on-click open-modal!}
        [settings {:font-size "large"}]]
       [drawer {:open @open?
                :on-close close-modal!
                :anchor "right"}
        [:div {:style {:height "100%"
                       :display "flex"
                       :flex-direction "column"
                       :padding "2em 3em"}}
         [:h2  "Settings"]
         [:h3  "— Drops"]
         [button
          {:sx {:margin "1em 0 1em 0"}
           :variant "outlined"
           :on-click #(rf/dispatch [::events/unfocus-all-drops])
           :aria-label "unfocus all drops"}
          "Unfocus All Drops"]
        ;; TODO -- implement
        ;;  [:h3  "— Save Zone"]
        ;;  [button
        ;;   {:sx {:margin "1em 0 1em 0"}
        ;;    :variant "contained"
        ;;    :color   "primary"
        ;;    :on-click (fn [_] (rf/dispatch [::events/export-user-data]))
        ;;    :aria-label "export user data to file"}
        ;;   "Export My Data"]
         [:h3  "— Danger Zone"]
         [button
          {:sx {:margin "1em 0 1em 0"}
           :variant "contained"
           :color   "error"
           :on-click (fn [_]
                       (let [confirmed? (js/confirm "Are you sure you want to delete all data? This cannot be undone.")]
                         (when confirmed?
                           (rf/dispatch [::events/delete-user-data]))))
           :aria-label "delete user data"}
          "Delete My Data"]
         [icon-button
          {:sx {:margin-top "auto"
                :margin-inline "auto"
                :margin-bottom "24px"
                :width "fit-content"}
           :on-click close-modal!}
          [close-outlined]]]]]
      )))

(defn move-drop-modal []
  (let [open?         (r/atom false)
        open-modal!  #(open-modal! open?)
        selected-id   (r/atom nil)
        close-modal!  (fn []
                        (reset! selected-id nil)
                        (close-modal! open?))]
    (fn []
      (let [drop-id @(rf/subscribe [::subs/first-in-queue])
            drop    @(rf/subscribe [::subs/drop drop-id])]
        [:<>
         [icon-button
          {:on-click   open-modal!
           :size       "small"
           :aria-label "move drop to another source"}
          [control-camera]]
         [dialog
          {:open     @open?
           :on-close close-modal!}
          [dialog-title "Move Drop"]

          [dialog-content
           [dialog-content-text
            (str "Select a drop to place " (:label drop) " within.")]
           [drop-selector selected-id]
           [dialog-actions
            [button {:on-click (fn [_]
                                 (rf/dispatch [::events/rehome-drop drop-id @selected-id])
                                 (close-modal!))
                     :disabled (not @selected-id)
                     :aria-label (str "change source of " (:label drop))}
             "Move Drop"]
            [button {:on-click close-modal!} "Close"]]]]]))))