(ns minddrop.core
  (:require
   [minddrop.db     :as db :refer [default-view-params]]
   [re-frame.core   :as re-frame]
   [minddrop.events :as events]
   [minddrop.config :as config]
   [minddrop.views  :as views]
   [reagent.dom     :as rdom]
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
