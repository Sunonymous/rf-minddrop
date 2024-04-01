(ns minddrop.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::pool
 (fn [db] (:pool db)))

(rf/reg-sub
 ::source
 (fn [db] (:source db)))

(rf/reg-sub
 ::user
 (fn [db] (:user db)))