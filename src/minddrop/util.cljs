(ns minddrop.util)

(defn prompt-string
  "Prompt the user for a string and trim it.
   Returns an empty string for invalid or empty input."
  [query]
  (.trim (or (js/prompt query) "")))