(ns logprocessor.core-test
  (:require [clj-time.core :as t]
            [com.climate.claypoole :as cp]
            [logprocessor
             [core :refer :all]
             [parsers-test :as pt]
             [utils :as utils]]
            [midje.sweet :refer :all]
            [user :as dev]))

(facts "process-file checks"
  (pt/$> "rsp-error" process-file) => (contains {:errors anything})

  ;; Typical example of parsed xml
  (pt/$> "rq-retrieve" process-file) =>
  {:id "JIHENT"
   :message-id "8edb358c-88d2-11e5-a341-0eebf1123529",
   :pcc "0O0G",
   :refto nil,
   :service "TravelItineraryReadLLSRQ",
   :session-id "d8da88d4-88bb-11e5-a341-0eebf1123529",
   :timestamp "2015-11-11T16:16:02"
   :date (t/date-time 2015 11 11 16 16 2)})

(facts "Check walk-over-file"
  (->
   (take 2 (dev/walk-over-file "examples.zip"))
   utils/intensive-processing-items
   first) => (contains {:date anything}))

(fact :slow "Check walk-over-s3."
  (let [items (utils/walk-over-s3 :bcd1 :fokker (t/date-time 2016 2 2))]
    (count
     (cp/pmap net-pool
              #(update % :source (fn [c] (c)))
              (take 8 items)))) => 8)
