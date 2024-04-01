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
(s/def ::source      pos-int?)
; Drop-full
(s/def ::drop     (s/keys :req-un [::id ::label ::source ::resonance ::notes ::links ::touched]))

; DB
(s/def ::focused-ids list?)
(s/def ::pool (s/coll-of (fn [[id drop]] (s/and
                                           (s/valid? ::id   id)
                                           (s/valid? ::drop drop)))))
(s/def ::queue     (s/coll-of ::id))
; DB-full
;; TODO revise with new format
(s/def ::db (s/keys :req-un [::source ::pool ::focused-ids ::queue]))

;;;;;;;;;;;;;;;;
;; Default DB ;

(def default-db
  {:user   "Me"
   :source  (constants :master-id)
   :pool   {(constants :master-id) (new-drop
                                    (constants :master-label)
                                    (constants :master-id)
                                    (constants :master-id))}
   :queue        []
   :focused-ids '()
  })