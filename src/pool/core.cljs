(ns pool.core
  (:require [drop.core :as drop]))

;; Functions for working with a collection of drops, ie. a "pool".

(defn immediate-children
  "Returns the immediate inner drops given a particular source-id."
  [source-id pool]
  (filter
   (fn [[_ drop]]
     (and ;; don't count the master id
      (not= (drop :id) (drop/constants :master-id))
      (drop/has-source? source-id drop)))
   pool))

(defn source-needs-refresh?
  "Returns true if a given source ID contains at least one drop,
   and every drop within that source has been touched."
  [source-id pool]
  (let [drops-in-source (immediate-children source-id pool)]
    (and
     (< 0 (count drops-in-source))
     (every? (fn [[_ drop]] (drop/touched? drop)) drops-in-source))))

(defn- is-nested-within?
  "A very inefficient utility function which determines if,
   somewhere along the chain of sources, a given drop ID is
   within a particular source."
  [query-id drop-id pool]
  (let [source-id (:source (pool drop-id))
        master-id (drop/constants :master-id)]
    (cond
      (or
       (= source-id master-id)
       (nil? (pool source-id))) ;; just in case
      false

      (= source-id query-id)
      true

      :otherwise
      (recur query-id source-id pool))))

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

(defn- if-else-true
  "Used in the pool filter predicate function to test
   for certain conditions or return true if the condition
   does not need to be tested."
  [pred true-val]
  (if pred
    true-val
    true))

;; TODO test me more thoroughly!
(defn pool-filters->predicate
  "Given a map containing kw options for filtering, returns the
   predicate for use in the filter function. Filters may be:
   :focused   -- true / false
   :untouched -- true / false
   :source-id -- (optional) <source id>
   :label     -- substring included in label"
  [filters]
    (fn [drop]
      (let [focused?        (drop/is-focused? drop)
            untouched?      (drop/untouched? drop)
            matches-label?  (drop/label-includes? (filters :label) drop)
            not-master?     (not= (drop :id) (drop/constants :master-id))
            not-the-source? (not= (drop :id) (filters :source))]
        (if (filters :focused)
          (and ;; only focused drops
           focused?
           untouched?
           (if-else-true (seq (filters :label))
             matches-label?))
          (and ;; any valid drop
           not-master?
           (if-else-true (filters :untouched)
             untouched?) ;; this is currently unreachable
           (if-else-true (filters :source)
             (drop/has-source? (:source filters) drop)) ;; equivalent of searching all sources
           (if-else-true (seq (filters :label))
             (drop/label-includes? (filters :label) drop))
           not-the-source?)))))

(defn pool->queue
  [predicate pool]
  (mapv (fn [[id _]] id) ;; reduce to a vector of ids only
        (filter (fn [[_ drop]] (predicate drop)) pool)))

(defn dependent-drops [pool outer-id]
    (->> pool
         (filter (fn [[id _]] (is-nested-within? outer-id id pool)))
         (map first)
         vec))

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