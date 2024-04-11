(ns minddrop.events
  (:require
   [cljs.reader        :refer [read-string]]
   [clojure.string     :refer [lower-case]]
   [clojure.spec.alpha :as s]
   [drop.core          :as drop]
   [pool.core          :as pool]
   [re-frame.core      :as rf]
   [minddrop.db        :as db]))

;;;;;;;;;;;;;;;;;;;
;; DB Validation ;

(defn check-and-throw
  "Throws an exception if db doesn't match spec."
  [spec db]
  (when-not (s/valid? spec db)
    (throw (ex-info (str "DB Change Failure: " (s/explain-str spec db)) {}))))

(def check-db-change
  (rf/after (partial check-and-throw ::minddrop.db/db)))

;;;;;;;;;;;;;;;;;;;;;
;; View Parameters ;

(rf/reg-event-db
 ::update-view-params
 (fn [db [_ param next-val]]
   (assoc-in db [:view-params param] next-val)))

;;;;;;;;;;;;;;;;;;;;;;;
;; DB Initialization ;

(def local-storage-key "minddrop_pool")

(defn pool->local-storage
  "Writes the given pool to local storage."
  [db]
  (let [json (pr-str (:pool db))]
    (js/localStorage.setItem local-storage-key json)))

(def ->local-storage (rf/after pool->local-storage))

(rf/reg-cofx
 :local-store
 (fn [coeffects storage-key]
   (assoc coeffects :local-store
          (js->clj (.getItem js/localStorage storage-key)))))

(rf/reg-event-db
 ::rebuild-queue
 (fn [db [_ filter-predicate]]
   (update db :queue (fn [_] (pool/pool->queue filter-predicate (:pool db))))))

(rf/reg-event-db
 ::queue-forward
 (fn [db]
   (update db :queue #(into [] (rest %)))))

(rf/reg-event-fx
 ::initialize-db
 [(rf/inject-cofx :local-store local-storage-key)]
 (fn [cofx _]
   (let [initial-db     db/default-db
         persisted-pool (read-string (:local-store cofx))]
     (if (and persisted-pool
              (s/valid? ::minddrop.db/pool persisted-pool))
       (assoc cofx :db (assoc initial-db :pool persisted-pool))
       (assoc cofx :db initial-db)))))

;; this was used to debug
(rf/reg-event-db
 ::load-pool-from-string
 (fn [db [_ str-data]]
       (let [next-pool (read-string str-data)]
         (if (and next-pool
                  (s/valid? ::minddrop.db/pool next-pool))
           (assoc db :pool next-pool)
           (do
             (js/alert "Invalid pool data.")
             db)))))

;;;;;;;;;;;
;; Drop ;

(rf/reg-event-db
 ::touch-drop
 [->local-storage]
 (fn [db [_ drop-id]]
   (update-in db [:pool drop-id] drop/touch)))

(rf/reg-event-db
 ::relabel-drop
 [->local-storage]
 (fn [db [_ drop-id next-label]]
   (update-in db [:pool drop-id] drop/relabel next-label)))

(rf/reg-event-db
 ::renote-drop
 [->local-storage]
 (fn [db [_ drop-id next-notes]]
   (update-in db [:pool drop-id] drop/renote next-notes)))

(rf/reg-event-db
 ::resonate-drop
 [->local-storage]
 (fn [db [_ drop-id amount]]
   (update-in db [:pool drop-id] drop/resonate amount)))

(rf/reg-event-db
 ::link-drop
 [->local-storage]
 (fn [db [_ drop-id link]]
   (update-in db [:pool drop-id] drop/add-link (lower-case link))))

(rf/reg-event-db
 ::unlink-drop
 [->local-storage]
 (fn [db [_ drop-id link]]
   (update-in db [:pool drop-id] drop/remove-link link)))

;;;;;;;;;;
;; Pool ;

(rf/reg-event-db
 ::add-drop
 [check-db-change
  ->local-storage]
 (fn [db [_ new-drop]]
   (assoc-in db [:pool (:id new-drop)] new-drop)))

(rf/reg-event-db
 ::remove-drop
 [check-db-change]
 (fn [db [_ drop-id]]
   (update db :pool dissoc drop-id)))

(rf/reg-event-db
 ::refresh-source
 [check-db-change
  ->local-storage]
 (fn [db [_ source-id]]
   (update db :pool (partial pool/refresh-source source-id))))

(rf/reg-event-db
 ::unfocus-all-drops
 [check-db-change
  ->local-storage]
 (fn [db]
   (update db :pool (partial pool/do-to-drops drop/unfocus drop/is-focused?))))

(rf/reg-event-db
 ::refresh-focused-drops
 [check-db-change]
 (fn [db]
   (update db :pool (partial pool/do-to-drops drop/untouch #(drop/is-focused? %)))))

;;;;;;;;
;; DB ;

(rf/reg-event-db
 ::enter-drop
 (fn [db [_ drop-id]]
   (assoc db :source drop-id)))

(rf/reg-event-db
 ::toggle-drop-focus
 (fn [db [_ drop-id]]
   (update-in db [:pool drop-id :focused] not)))

;; TODO remove this when happy with focused drop functionality
(rf/reg-event-db
 ::rotate-focused-ids
 (fn [db [_]]
   (update db :focused-ids #(flatten (list (rest %) (first %))))))