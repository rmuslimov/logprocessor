(ns logprocessor.core-test
  (:require [clj-time.core :as t]
            [com.climate.claypoole :as cp]
            [logprocessor
             [core :as core :refer :all]
             [parsers-test :as pt]
             [utils :as utils]]
            [midje.sweet :refer :all]
            [user :as dev]))

;; (facts "process-file checks"
;;   ;; Typical example of parsed xml
;;   (pt/$> "rq-retrieve" process-file) =>
;;   {:id "JIHENT"
;;    :message-id "8edb358c-88d2-11e5-a341-0eebf1123529",
;;    :pcc "0O0G",
;;    :refto nil,
;;    :service "TravelItineraryReadLLSRQ",
;;    :session-id "d8da88d4-88bb-11e5-a341-0eebf1123529",
;;    :timestamp "2015-11-11T16:16:02"
;;    :date (t/date-time 2015 11 11 16 16 2)}

;;   (fact "Check having name as field"
;;     (process-item {:source (pt/$> "rq-retrieve") :name ..x..})=>
;;     (contains {:name ..x..}))

;;   ;; Failing may happen
;;   (process-item {:source (pt/$> "rq-invalid") :name "x"}) =>
;;   (contains {:exception anything}))

;; (fact "Check walk-over-file"
;;   (->
;;    (take 3 (dev/walk-over-file "examples.zip"))
;;    core/intensive-processing-items
;;    second) => (contains {:date anything}))

(facts "Check list s3 objects..."
  (utils/list-s3-objects-par :bcd1 :fokker 2016 2) => (repeat 29 {:source 1 :name 1})
  (provided
    (utils/list-s3-objects anything anything anything) => [{:source 1 :name 1}])

  (utils/list-s3-objects-par :bcd1 :fokker 2016 2 1) => (list {:source 1 :name 1})
  (provided
    (utils/list-s3-objects anything anything anything) => [{:source 1 :name 1}]))
