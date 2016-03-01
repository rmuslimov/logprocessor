(ns logprocessor.core-test
  (:require [clj-time.core :as t]
            [com.climate.claypoole :as cp]
            [logprocessor
             [core :refer :all]
             [parsers-test :as pt]
             [utils :as utils]]
            [midje.sweet :refer :all]
            [user :as dev]))

(facts "Resulting xml process functions."
  (pt/$> "rsp-error" process-file) => (contains {:errors anything}))

(facts "Processing xml from zip file"
  (let [calls 2]
    (count
     (map
      process-item
      (take calls (dev/walk-over-file "examples.zip"))))) => 2

  ;; Typical example of parsed xml
  (pt/$> "rq-retrieve" process-file) =>
  {:id "JIHENT"
   :message-id "8edb358c-88d2-11e5-a341-0eebf1123529",
   :pcc "0O0G",
   :refto nil,
   :service "TravelItineraryReadLLSRQ",
   :session-id "d8da88d4-88bb-11e5-a341-0eebf1123529",
   :timestamp "2015-11-11T16:16:02Z"})

(fact :slow "Loading S3 files, is slow for obvious reasins"
  (let [items (utils/walk-over-s3 :bcd1 :fokker (t/date-time 2016 2 2))]
    (count
     (cp/pmap net-pool
              #(update % :source (fn [c] (c)))
              (take 8 items)))) => 8)
