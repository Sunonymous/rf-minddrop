(ns pool.core
  (:require [drop.core :as drop]))

;; Functions for working with a collection of drops, ie. a "pool".

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
    (drop/constants :master-id)))

;; TODO test me more thoroughly!
(defn pool-filters->predicate
  "Given a map containing kw options for filtering, returns the
   predicate for use in the filter function. Filters may be:
   :untouched -- true / false
   :source-id -- (optional) <source id>
   :label     -- substring included in label"
  [filters focused-ids]
    (fn [drop]
      (and
       (not= (drop :id) (drop/constants :master-id))
       (if (filters :untouched)
         (drop/untouched? drop)
         (drop/touched? drop))
       (if (filters :source)
         (drop/has-source? (:source filters) drop)
         true) ;; equivalent of searching all sources
       (if (seq (filters :label))
         (drop/label-includes? (filters :label) drop)
         true)
       (when (not= (drop :id) (filters :source))
         true))))

(defn pool->queue
  [predicate pool]
  (mapv (fn [[id _]] id) ;; reduce to a vector of ids only
        (filter (fn [[_ drop]] (predicate drop)) pool)))

;;;;;;;;;;;;;;;;
;; Predicates ;

(defn- min-num-of-inner-drops
  "Helper which returns 1 if given the master source ID,
   and 0 otherwise."
  [source-id]
  (if (= source-id (:master-id drop/constants))
    1
    0))

(defn source-needs-refresh?
  "Returns true if a given source ID contains at least one drop,
   and every drop within that source has been touched."
  [source-id pool]
  (let [drops-in-source (filter
                         (fn [[_ drop]]
                           (and ;; don't count the master id
                            (not= (drop :id) (drop/constants :master-id))
                            (drop/has-source? source-id drop)))
                         pool)]
    (and
     (< 0 (count drops-in-source))
     (every? (fn [[_ drop]] (drop/touched? drop)) drops-in-source))))

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

(defn refresh-source
  "Untouches all drops matching a provided source id."
  [source-id pool]
  (do-to-drops drop/untouch (drop/has-source? source-id) pool))