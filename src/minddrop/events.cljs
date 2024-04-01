(ns minddrop.events
  (:require
   [re-frame.core :as rf]
   [minddrop.db   :as db]
   ))

;;;;;;;;;;;;;;;;;;;
;; DB Validation ;

;; TODO reimplement check and throw

;;;;;;;;;;;;;;;;;;;;;;;
;; DB Initialization ;

(rf/reg-event-db
 ::initialize-db
   (fn [_ _] db/default-db))