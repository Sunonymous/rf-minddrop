(ns minddrop.core
  (:require
   [minddrop.db     :as db :refer [default-view-params]]
   [drop.core              :refer [constants]]
   [re-frame.core   :as re-frame]
   [minddrop.events :as events]
   [minddrop.config :as config]
   [minddrop.views  :as views]
   [minddrop.subs   :as subs]
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

  ;; have to ensure that previous home source is still valid
  ;; removes configuration if it is invalid
  (let [configured-home-id (config/of :home-source)]
    (if @(re-frame/subscribe [::subs/drop configured-home-id])
      (re-frame/dispatch-sync [::events/update-view-params :source configured-home-id])
      (re-frame/dispatch-sync [::events/config-as :home-source (constants :master-id)])))
  (re-frame/dispatch-sync [::events/rebuild-queue
                           (pool/pool-filters->predicate default-view-params)])
  (dev-setup)
  (mount-root))
