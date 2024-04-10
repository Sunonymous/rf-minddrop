(ns minddrop.core
  (:require
   [minddrop.db     :as db :refer [default-view-params]]
   [reagent.dom     :as rdom]
   [re-frame.core   :as re-frame]
   [minddrop.events :as events]
   [minddrop.views  :as views]
   [minddrop.config :as config]
   [pool.core       :as pool]))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/minddrop] root-el)))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [::events/rebuild-queue
                           (pool/pool-filters->predicate default-view-params)])
  (dev-setup)
  (mount-root))
