(ns logprocessor.parsers-test
  (:require [clj-time.core :as t]
            [clj-xpath.core :as xp]
            [clojure.java.io :as io]
            [logprocessor
             [core :as core]
             [parsers :refer :all]
             [utils :as utils]]
            [midje.sweet :refer :all]
            [user :as dev]))

(defmacro $>
  "Short macro to load test xml. ($> name <do-whatever>...)."
  [filename & fns]
  (let [fname (format "test/%s.xml" filename)]
    `(-> ~fname io/resource slurp ~@fns)))

(facts "Local functions tests"
  (map parse-method-name (core/walk-over-local-files "test")) =>
  (contains [:EndTransactionRS :EndTransactionRQ] :in-any-order :gaps-ok))

(facts "Parsing basic info"
  ;; header info
  ($> "rsp-error" parse-header-info) =>
  {:session-id "4f7869aa-8940-11e5-9fb8-0eebf1123529",
   :refto "dbb5be0c-8965-11e5-9fb8-0eebf1123529",
   :message-id "a02217bd-4326-4831-9287-e5fa0a92a62a@87",
   :service "EndTransactionLLSRS",
   :timestamp "2015-11-12T17:50:27",
   :pcc "0O0G"}

  ;; parse-method-name
  ($> "rsp-error" parse-method-name) => :EndTransactionRS)

(facts "Parsing error info"
  (xp/xmlnsmap-from-node
   ($> "rsp-error" extract-body-node)) => (contains {"stl" anything})

  ($> "rsp-error" extract-body-node parse-error-info) =>
  '("PREVIOUS ENTRY IN PROGRESS, PLEASE WAIT"))

(facts "Resulting xml process functions."
  ($> "rsp-error" process-file) => (contains {:errors anything}))

(facts "Processing xml from zip file"
  (let [calls 10]
    (count
     (map
      process-item
      (take calls (dev/walk-over-file "examples.zip")))) => calls)

  ;; Typical example of parsed xml
  ($> "rq-retrieve" process-file) =>
  {:id "JIHENT"
   :message-id "8edb358c-88d2-11e5-a341-0eebf1123529",
   :pcc "0O0G",
   :refto nil,
   :service "TravelItineraryReadLLSRQ",
   :session-id "d8da88d4-88bb-11e5-a341-0eebf1123529",
   :timestamp "2015-11-11T16:16:02Z"})

(facts "Parsing SABRE trivial methods"
  ($> "rq-et" extract-body-node parse-et-rq) => {:Ind true})

(fact "Parsing retrive PNR request"
  ($> "rq-retrieve" extract-body-node parse-retrieve-rq) => {:id "JIHENT"}
  ($> "rq-retrieve" parse-method-name) => :TravelItineraryReadRQ)

(fact :slow "Loading S3 files, is slow for obvious reasins"
  (count
   (map
    process-item
    (take 10 (utils/walk-over-s3 :bcd1 :fokker (t/date-time 2016 2 2))))) => 10)
