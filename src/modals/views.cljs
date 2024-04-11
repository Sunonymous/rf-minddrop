(ns modals.views
  (:require
   ;; Material UI
   [reagent-mui.material.dialog               :refer [dialog]]
   [reagent-mui.material.button               :refer [button]]
   [reagent-mui.material.icon-button          :refer [icon-button]]
   [reagent-mui.material.dialog-title         :refer [dialog-title]]
   [reagent-mui.material.dialog-actions        :refer [dialog-actions]]
   [reagent-mui.material.dialog-content       :refer [dialog-content]]
   [reagent-mui.material.dialog-content-text  :refer [dialog-content-text]]
   [reagent-mui.material.text-field           :refer [text-field]]
   ;; MUI Icons
   [reagent-mui.icons.add                     :refer [add]]
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

(defn add-drop-dialog
  []
  (let [open?          (r/atom false)
        toggle-modal! #(swap! open? not)
        new-label      (r/atom "")
        close-modal!   (fn [] (reset! open? false)
                         (reset! new-label ""))
        ;; _ (js/console.log "add-drop-dialog source: " source-id)
        ]
    (fn []
      (let [source-id @(rf/subscribe [::subs/source])
            submit-fn  (fn [e] (.preventDefault e)
                         (when (seq @new-label)
                           (let [next-drop (drop/new-drop @new-label @(rf/subscribe [::subs/next-id]) source-id)]
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