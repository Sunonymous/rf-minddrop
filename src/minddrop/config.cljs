(ns minddrop.config
  (:require [re-frame.core :as rf]
            [minddrop.subs :as subs]))

(def debug?
  ^boolean goog.DEBUG)

(def config-local-storage-key "minddrop_config")

;; Keep these next three vars in sync!

(def names
  {:user                        "Username"
   :prefill-relabel             "Prefill Drop Relabeling"
   :navigate-from-focused-drops "Navigate from Focused Drops"
   :confirm-before-action       "Confirm Before Actions"})

(def descriptions
  {:user                        "Currently unused. String name of user."
   :prefill-relabel             "Prefills the relabel dialog with the current label when true."
   :navigate-from-focused-drops "Allows entering and exiting drops from focused mode."
   :confirm-before-action       "Ensures the user wishes to perform an action before proceeding."})

(def default-config {:user                        "me"
                     :prefill-relabel             true
                     :navigate-from-focused-drops false
                     :confirm-before-action       false})

(defn of
  "Given a setting keyword, this function returns a user value for a
   setting if one exists. Otherwise, the default setting is returned."
  [setting]
  (let [default default-config
        users   @(rf/subscribe [::subs/config])]
  ((merge default users) setting)))