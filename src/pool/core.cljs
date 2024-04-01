(ns pool.core
  (:require [drop.core :refer [constants untouch drop->property-and-id has-source? untouched?]]))

;; Functions for working with a collection of drops, ie. "pools".

;;;;;;;;;;;
;; Views ;

(defn next-id
  "Given a map of drops, returns the proper integer ID
   value for a newly created drop"
  [pool]
  (if (seq pool)
    (->> pool
         keys
         (apply max)
         inc)
    (constants :master-id)))

(defn group-with-id-and
  [kw drops]
  (into {}
        (map (fn [[_ drop]] (drop->property-and-id kw drop)) drops)))

;; TODO: function needs refactored to receive a dynamic filtering predicate
(defn pool->queue
  "Forms a vector of drop IDs for use in a queue for explore mode.
   If focused IDs are provided, gives only drops matching those IDs."
  [focused-ids source-id pool]
  (mapv (fn [[id _]] id)
        (filter (fn [[_ drop]] (if (seq focused-ids)
                                 (focused-ids (:id drop))
                                 (and
                                  (has-source? drop source-id)
                                  (untouched? drop)
                                  (not= (drop :id) source-id))))
                pool)))

;;;;;;;;;;;;;;;;
;; Predicates ;

(defn- min-num-of-inner-drops
  "Helper which returns 1 if given the master source ID,
   and 0 otherwise."
  [source-id]
  (if (= source-id (:master-id constants))
    1
    0))

(defn need-refresh?
  "Returns true if a given source ID contains at least one drop,
   and every drop within that source has been touched."
  [source-id drops]
  (< (min-num-of-inner-drops source-id)
     (count (filter
             (fn [[_ drop]] (has-source? source-id drop))
             drops))))

;;;;;;;;;;;;;
;; Actions ;

(defn do-to-drops
  "Maps a function over all drops. If given a drop predicate
  function, maps over the drops which return true."
  ([f condition pool]
   (into {}
         (map (fn [[id drop]] (if (condition drop)
                                [id (f drop)]
                                [id drop]))
              pool)))
  ([f pool] (do-to-drops f true pool)))

(defn refresh
  "Untouches all drops in pool."
  [source-id drops]
  (do-to-drops untouch #(= (:source %) source-id) drops))