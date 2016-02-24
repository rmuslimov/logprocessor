(ns logprocessor.parsers-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [logprocessor
             [core :as core]
             [parsers :refer :all]]))

(def example-err-xml "test/soap-response-4YQ7mABXSVTnllMUtIHb6b.xml")

(defn get-xml-example
  "Just get xml example"
  [name]
  (-> example-err-xml io/resource slurp))

(deftest check-getting-method-name
  (is
   (=
    (let [example (get-xml-example example-err-xml)]
      (get-method-name example)) :EndTransactionRS)))

(deftest check-iterating-over-locals
  (is
   (=
    (map get-method-name (core/walk-over-local-files))
    (list :EndTransactionRQ :OTA_PingRQ :EndTransactionRS))))

(deftest check-parsing-header-information
  (is
   (=
    (let [example (get-xml-example example-err-xml)]
      (parse-header-info example))
    {:session-id "4f7869aa-8940-11e5-9fb8-0eebf1123529",
     :refto "dbb5be0c-8965-11e5-9fb8-0eebf1123529",
     :service "EndTransactionLLSRQ",
     :pcc "0O0G"})))
