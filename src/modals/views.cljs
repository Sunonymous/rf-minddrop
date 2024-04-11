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
   ;; MUI Icons
   [reagent-mui.icons.add                     :refer [add]]
   [reagent-mui.icons.link                    :refer [link]]
   [reagent-mui.icons.close-outlined          :refer [close-outlined]]
   ;; Minddrop
   [minddrop.config :as config]
   [minddrop.events :as events]
   [minddrop.subs   :as subs]
   [minddrop.util   :as util]
   [pool.core       :as pool]
   [drop.core       :as drop]
   [re-frame.core   :as rf]
   [reagent.core :as r]))

;; This file contains hiccup for particular dialogs and modals
;; that the user needs to interact with.

;; General functions for reusability
(defn open-modal!  [modal*] (reset! modal* true))
(defn close-modal! [modal*] (reset! modal* false))

(defn add-drop-dialog []
  (let [open?          (r/atom false)
        toggle-modal! #(swap! open? not)
        new-label      (r/atom "")
        close-modal!   (fn [] (reset! open? false)
                              (reset! new-label ""))]
    (fn []
      (let [source-id @(rf/subscribe [::subs/source])
            submit-fn  (fn [e] (.preventDefault e)
                         (when (seq @new-label)
                           (let [next-drop (drop/new-drop
                                            @new-label
                                            @(rf/subscribe [::subs/next-id])
                                            source-id)]
                             (rf/dispatch [::events/add-drop next-drop]))
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
           :Paper-Props {:component "form"
                         :on-submit submit-fn}}
          [dialog-title "Add Drop"]
          [dialog-content
           [dialog-content-text
            (str "Drop will be added inside " (:label @(rf/subscribe [::subs/drop source-id])) ".")
            [:br]
            "How should we label it?"]
           [text-field
            {:sx          {:margin "1em 2em"}
             :auto-focus   true
             :variant     "standard"
             :size        "small"
             :value       @new-label
             :placeholder "Drop Label"
             :on-change   #(reset! new-label (-> % .-target .-value))}]]
          [dialog-actions
           [button
            {:on-click   submit-fn
             :aria-label "add new drop"}
            "Add"]
           [button
            {:on-click #(close-modal!)
             :aria-label "cancel adding new drop"}
            "Close"]]]]))))

(defn drop-link-dialog []
  (let [open?         (r/atom false)
        open-modal!  #(open-modal! open?)
        close-modal! #(close-modal! open?)
        selected-link (r/atom nil)
        link-to-add   (r/atom "")]
    (fn []
      (let [drop    @(rf/subscribe [::subs/drop @(rf/subscribe [::subs/first-in-queue])])]
        [:<>
         [icon-button
          {:on-click open-modal!
           :size "small"}
          [link]]
         [dialog
          {:open     @open?
           :on-close close-modal!}
          [dialog-title "Edit Drop Links"]
          [dialog-content
           [dialog-content-text "Your drop contains the following links:"]
           (for [link (:links drop)]
             [button
              {:key link
               :variant "outlined"
               :sx    {:display "inline-block"
                       :margin  "4px"
                       :padding "0.125em 0.5em"
                       :border  "1px solid black"
                       :border-radius "8px"}
               :on-click #(reset! selected-link link)
               :aria-label (str "select link " link)} link])
           (when @selected-link
             [:div {:style {:margin-top "1em"}}
              [dialog-content-text
               "Selected Link: " [:span {:style {:font-weight "bold"}} @selected-link]]
              [button
               {:variant "contained"
                :color   "error"
                :size    "small"
                :on-click (fn [_] (rf/dispatch [::events/unlink-drop (:id drop) @selected-link])
                            (reset! selected-link nil))}
               "Remove Link"]])
           [:hr {:style {:margin-block "1em"}}]
           [dialog-content-text "Add Link"]
           [text-field
            {:variant "outlined"
             :size    "small"
             :on-change #(reset! link-to-add (-> % .-target .-value))}]
           [button
            {:sx {:position "relative" :top "0.25em" :left "0.5em"}
             :variant    "outlined"
             :size       "small"
             :aria-label (str "add link " @link-to-add " to drop")
             :on-click (fn [_] (when (seq @link-to-add)
                                 (rf/dispatch [::events/link-drop (:id drop) @link-to-add]))
                         (reset! link-to-add ""))}
            "Add"]
           [dialog-actions
            [button {:on-click close-modal!} "Close"]]]
          ]]))))