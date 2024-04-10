(ns minddrop.db
  (:require
   [drop.core :refer [constants new-drop]]
   [clojure.spec.alpha :as s]))

;;;;;;;;;;;;;
;; DB Spec ;

; Drop
(s/def ::id        pos-int?)
(s/def ::label     string?)
(s/def ::resonance number?)
(s/def ::notes     string?)
(s/def ::links     set?)
(s/def ::touched   boolean?)
(s/def ::source    pos-int?)

; Drop-full
(s/def ::drop (s/keys :req-un
                      [::id ::label ::source ::resonance
                       ::notes ::links ::focused ::touched]))

; View Parameters
(s/def ::focused boolean?)
(s/def ::untouched boolean?)
(s/def ::view-params (s/keys :req-un [::source ::focused ::untouched ::label]))

; DB
(s/def ::focused-ids list?)
(s/def ::pool (s/coll-of (fn [[id drop]]
                           (s/and
                            (s/valid? ::id   id)
                            (s/valid? ::drop drop)))))
; DB-full
(s/def ::db (s/keys :req-un [::pool ::view-params]))

;;;;;;;;;;;;;;
;; Defaults ;

;; Default Search Parameters
(def default-view-params
  {:source    (constants :master-id)
   :focused   false
   :untouched true
   :label     ""})

(def default-db
  {:user   "me"
   :pool   {(constants :master-id) (new-drop
                                    (constants :master-label)
                                    (constants :master-id)
                                    (constants :master-id))}
   :view-params default-view-params})