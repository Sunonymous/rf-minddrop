(ns minddrop.core
  (:require
   [reagent.dom     :as rdom]
   [re-frame.core   :as re-frame]
   [minddrop.events :as events]
   [minddrop.views  :as views]
   [minddrop.config :as config]

   [pool.core :as pool]))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch-sync [::events/rebuild-queue (pool/pool-filters->predicate views/default-pool-filters)])
  (dev-setup)
  (mount-root))
