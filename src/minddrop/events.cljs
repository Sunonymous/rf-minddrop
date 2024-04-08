(ns minddrop.events
  (:require
   [cljs.reader        :refer [read-string]]
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

;;;;;;;;
;; DB ;

(rf/reg-event-db
 ::enter-drop
 (fn [db [_ drop-id]]
   (assoc db :source drop-id)))

(rf/reg-event-db
 ::focus-drop
 (fn [db [_ drop-id]]
   (update db :focused-ids conj drop-id)))

(rf/reg-event-db
 ::unfocus-drop
 (fn [db [_ drop-id]]
   (update db :focused-ids #(remove #{drop-id} %))))

(rf/reg-event-db
 ::rotate-focused-ids
 (fn [db [_]]
   (update db :focused-ids #(flatten (list (rest %) (first %))))))