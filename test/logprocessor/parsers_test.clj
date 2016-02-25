(ns logprocessor.parsers-test
  (:require [clj-xpath.core :as xp]
            [clojure.java.io :as io]
            [logprocessor
             [core :as core]
             [parsers :refer :all]]
            [midje.sweet :refer :all]
            [user :as dev]))

(def example-err-xml "test/rsp-error.xml")

(defn get-xml-example
  "Just get xml example"
  [name]
  (-> example-err-xml io/resource slurp))

(facts "Local functions tests"
  (map parse-method-name (core/walk-over-local-files "test")) =>
  (contains [:EndTransactionRS :EndTransactionRQ :OTA_PingRQ] :in-any-order))

(facts "Parsing basic info"
  ;; header info
  (-> example-err-xml get-xml-example parse-header-info) =>
  {:session-id "4f7869aa-8940-11e5-9fb8-0eebf1123529",
   :refto "dbb5be0c-8965-11e5-9fb8-0eebf1123529",
   :message-id "a02217bd-4326-4831-9287-e5fa0a92a62a@87",
   :service "EndTransactionLLSRS",
   :timestamp "2015-11-12T17:50:27",
   :pcc "0O0G"}

  ;; parse-method-name
  (-> example-err-xml get-xml-example parse-method-name) => :EndTransactionRS)

(facts "Parsing error info"
  (xp/xmlnsmap-from-node
   (extract-body-node (get-xml-example example-err-xml))) =>
  (contains {"stl" anything})

  (-> example-err-xml get-xml-example extract-body-node parse-error-info) =>
  '("PREVIOUS ENTRY IN PROGRESS, PLEASE WAIT")

  (inc 1) => 2)

(facts "Resulting xml process functions."
  (-> example-err-xml get-xml-example process-file) =>
  (contains {:errors anything}))

(facts "Processing xml from zip file"
  (let [calls 10]
    (count
     (map
      #(update-in % [:source] process-file)
      (take calls (dev/walk-over-file "examples.zip")))) => calls))
