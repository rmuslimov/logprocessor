(ns client.db
  (:require [reagent.core :as reagent]))

(def columns
  [{:field :utc :name "UTC" :width 3}
   {:field :soap :name "SOAP" :width 2}
   {:field :session-id :name "Session ID" :width 7}
   {:field :pnr :name "PNR" :width 2}])

(def state
  (reagent/atom
   {:query ""
    :rows
    [{:id 1 :utc "12 Nov, 14:55:56" :soap "OTA_PingRQ"
      :session-id "efaf295e-895e-11e5-9fb8-0eebf1123529" :pnr "ZENYOU"}
     {:id 2 :utc "14 Nov, 14:55:56" :soap "OTA_PingRQ"
      :session-id "efaf295e-895e-11e5-9fb8-0eebf1123529" :pnr "ZENYOU"}
     {:id 3 :utc "15 Nov, 14:55:56" :soap "OTA_PingRQ"
      :session-id "efaf295e-895e-11e5-9fb8-0eebf1123529" :pnr "ZENYOU"}]
    :status :ready   ;; or waiting
    }))
