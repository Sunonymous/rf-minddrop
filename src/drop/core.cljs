(ns drop.core
  (:require [clojure.string :refer [includes? lower-case]]))

;; Sensible Defaults
(def constants
  {:initial-resonance 0
   :master-id         1
   :master-label      "All"
   :focus-boost       0.05
   :notes-boost       0.25
   :inner-boost       0.15}) ;; for adding a drop within a source

;;;;;;;;;;;;;;;;;;;
;; Drop Creation ;
(defn new-drop
  [label id source-id]
  {:id         id
   :label      label
   :source     source-id
   :resonance  (constants :initial-resonance)
   :focused    false
   :notes      ""
   :tags      #{}
   :touched    false})

;;;;;;;;;;;
;; Views ;

(defn drop->property-and-id
  "Reduces a drop to a vector of it's property (provided as
   a keyword for lookup) and its ID."
  [kw drop]
  [(drop kw) (drop :id)])

;;;;;;;;;;;;;;;;
;; Predicates ;

(defn has-source?
  "Returns true only if drop's source matches a given ID.
   If provided only the source-id, returns a curried function
   which takes a drop."
  ([source-id drop]
   (= source-id (:source drop)))
  ([source-id] (fn [drop] (has-source? source-id drop))))

(defn untouched? [drop]
  (not (:touched drop)))

(def touched? (complement untouched?))

(defn is-focused? [drop] (:focused drop))

(defn label-includes?
  "True if a drop's label includes a certain substring.
   (Case-insensitive)"
  [s drop]
  (includes? (lower-case (:label drop)) (lower-case s)))

;;;;;;;;;;;;;
;; Actions ;

;; Touched drops are filtered from the queue upon generation.
(defn touch [drop]
  (assoc drop :touched true))

;; Used in cases where a drop needs to be presented and has
;; already been touched.
(defn untouch [drop]
  (assoc drop :touched false))

;; Changes the label of a drop.
(defn relabel [drop new-label]
  (assoc drop :label new-label))

;; Set a drop's notes.
(defn renote [drop next-notes]
  (assoc drop :notes next-notes))

;; Boost a drop's resonance.
(defn resonate [drop amount]
  (update drop :resonance + amount))

;; Used primarily to reset focus filters.
(defn unfocus [drop]
  (assoc drop :focused false))

(defn add-tag [drop tag]
  (update drop :tags conj tag))

(defn remove-tag [drop tag]
  (update drop :tags disj tag))
