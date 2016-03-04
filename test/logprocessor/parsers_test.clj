(ns logprocessor.parsers-test
  (:require [clj-xpath.core :as xpec]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [logprocessor.parsers :refer :all]
            [midje.sweet :refer :all]))

(defmacro $>
  "Short macro to load test xml. ($> name <do-whatever>...)."
  [filename & fns]
  (let [fname (format "test/%s.xml" filename)]
    `(-> ~fname io/resource slurp ~@fns)))

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
  (xpec/xmlnsmap-from-node
   ($> "rsp-error" extract-body-node)) => (contains {"stl" anything})

  ($> "rsp-error" extract-body-node parse-error-info) =>
  '("PREVIOUS ENTRY IN PROGRESS, PLEASE WAIT"))

(facts "Parsing SABRE trivial methods"
  ($> "rq-et" extract-body-node parse-et-rq) => {:Ind true})

(fact "Parsing retrive PNR request"
  ($> "rq-retrieve" extract-body-node parse-retrieve-rq) => {:id "JIHENT"}
  ($> "rq-retrieve" parse-method-name) => :TravelItineraryReadRQ)
