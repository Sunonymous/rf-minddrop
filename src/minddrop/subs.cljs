(ns minddrop.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::pool
 (fn [db] (:pool db)))

(rf/reg-sub
 ::drop
 (fn [[_ drop-id]]
   (rf/subscribe [::pool]))
 (fn [pool [_ drop-id]]
   (pool drop-id)))

(rf/reg-sub
 ::source
 (fn [db] (:source db)))

(rf/reg-sub
 ::queue
 (fn [db] (:queue db)))

(rf/reg-sub
 ::user
 (fn [db] (:user db)))

(rf/reg-sub
 ::focused-ids
 (fn [db] (:focused-ids db)))