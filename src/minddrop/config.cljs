(ns minddrop.config)

(def debug?
  ^boolean goog.DEBUG)

(def config-local-storage-key "minddrop_config")

(def settings-description
  {:user                        "Currently unused. String name of user."
   :prefill-relabel             "Prefills the relabel dialog with the current label when true."
   :navigate-from-focused-drops "Allows entering and exiting drops from focused mode."
   :confirm-before-action       "Ensures the user wishes to perform an action before proceeding."})

(def default-config {:user                        "me"
                     :prefill-relabel             true
                     :navigate-from-focused-drops false
                     :confirm-before-action       false})