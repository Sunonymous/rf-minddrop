(ns minddrop.subs
  (:require
   [re-frame.core :as rf]
   [pool.core :refer [pool-filters->predicate]]
   [pool.core :as pool]
   [drop.core :as drop]))

(rf/reg-sub
 ::pool
 (fn [db] (:pool db)))

(rf/reg-sub
 ::drop-labels
 (fn [] (rf/subscribe [::pool]))
 (fn [pool] (map (fn [[id drop]] [id (:label drop)]) pool)))

(rf/reg-sub
 ::drop
 (fn [[_ drop-id]]
   (rf/subscribe [::pool]))
 (fn [pool [_ drop-id]]
   (pool drop-id)))

(rf/reg-sub
 ::source
 (fn [] (rf/subscribe [::view-params]))
 (fn [view-params] (:source view-params)))

(rf/reg-sub
 ::queue
 (fn [] [(rf/subscribe [::pool])
         (rf/subscribe [::view-params])])
 (fn [[pool view-params]] ;; always sorted by descending resonance
   (let [predicate (pool-filters->predicate view-params)]
     (sort-by (fn [id] (:resonance @(rf/subscribe [::drop id]))) >
              (pool/pool->queue predicate pool)))))

(rf/reg-sub
 ::next-id
 (fn [] (rf/subscribe [::pool]))
 (fn [pool] (pool/next-id pool)))

(rf/reg-sub
 ::user
 (fn [db] (:user db)))

(rf/reg-sub
 ::focus-mode
 (fn [db]
   (:focused (:view-params db))))

(rf/reg-sub
 ::view-params
 (fn [db] (:view-params db)))

(rf/reg-sub
 ::priority-id
 (fn [db] (:priority-id db)))

(rf/reg-sub
 ::first-in-queue
 (fn []
   [(rf/subscribe [::priority-id])
    (rf/subscribe [::queue])])
 (fn [[priority-id queue]]
   (if priority-id
     priority-id
     (first queue))))

;; Provides the drops considered immediate children of the drop with the given id.
(rf/reg-sub
 ::immediate-children
 (fn [[_ drop-id]]
   (rf/subscribe [::pool]))
 (fn [pool [_ drop-id]]
   (pool/immediate-children drop-id pool)))

(rf/reg-sub
 ::parent-drop
 (fn [[_ drop-id]]
   (rf/subscribe [::pool]))
 (fn [pool [_ drop-id]]
   (pool (:source (pool drop-id)))))

(rf/reg-sub
 ::focused-drops
 (fn [db]
   (->> db
        :pool
        (filter (fn [[id drop]] (drop/is-focused? drop)))
        (map first)
        vec)))

;; Provides a set of all the links contained throughout the pool.
(rf/reg-sub
 ::all-drop-links
 (fn [] (rf/subscribe [::pool]))
 (fn [pool] (->> (map (fn [[_ drop]] (:links drop)) pool)
                 (remove empty?)
                 (map vec)
                 flatten
                 set)))