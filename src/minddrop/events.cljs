(ns minddrop.events
  (:require
   [re-frame.core :as rf]
   [minddrop.db   :as db]
   [cljs.reader :refer [read-string]]
   [drop.core :as drop]
   [pool.core :as pool]))

;;;;;;;;;;;;;;;;;;;
;; DB Validation ;

;; TODO reimplement check and throw

;;;;;;;;;;;;;;;;;;;;;;;
;; DB Initialization ;

;; TODO reimplement local storage

(rf/reg-event-db
 ::initialize-db
   (fn [_ _] db/default-db))

;;;;;;;;;;;
;; Drop ;

(rf/reg-event-db
 ::touch-drop
 (fn [db [_ drop-id]]
   (update-in db [:pool drop-id] drop/touch)))

(rf/reg-event-db
 ::relabel-drop
 (fn [db [_ drop-id next-label]]
   (update-in db [:pool drop-id] drop/relabel next-label)))

(rf/reg-event-db
 ::renote-drop
 (fn [db [_ drop-id next-notes]]
   (update-in db [:pool drop-id] drop/renote next-notes)))

;;;;;;;;;;
;; Pool ;

(rf/reg-event-db
 ::add-drop
 (fn [db [_ new-drop]]
   (assoc-in db [:pool (:id new-drop)] new-drop)))

(rf/reg-event-db
 ::remove-drop
 (fn [db [_ drop-id]]
   (update db :pool dissoc drop-id)))

(rf/reg-event-db
 ::refresh-source
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
 (fn [db [_ drop-id]] ;; TODO test this again
   (update db :focused-ids #(remove #{drop-id} %))))

(rf/reg-event-db
 ::rotate-focused-ids
 (fn [db [_]]
   (update db :focused-ids #(flatten (list (rest %) (first %))))))

;;;;;;;;;;;
;; Debug ;

(rf/reg-event-db
 ::debug-set-pool
     (fn [db [_ next-pool]]
       (let [data (read-string next-pool)]
         (assoc db :pool data))))

(rf/reg-event-db
 ::debug-insert-notes
 (fn [db [_ drop-id]]
   (update-in db [:pool drop-id] drop/renote "Inserted notes. These notes are long. Like really long.")))