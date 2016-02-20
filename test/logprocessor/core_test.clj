(ns logprocessor.core-test
  (:require [clojure.zip :as zip]
            [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.java.io :as io]
            [logprocessor.core :as lcore]
            [midje.sweet :refer :all]))

(def test-response-xml
  "test/soap-request-11tECVXBzAQt4gQ2Oli4WE.xml")

(defn get-test-zipper
  [test-xml]
  (-> test-xml io/resource io/input-stream xml/parse zip/xml-zip))

(against-background
  [(around :facts
           (let [zipper (get-test-zipper test-response-xml)]
             ?form))]

  "Check basic parsing function and EndTransacion XML"
  (fact "check header information parsing - check pcc"
    (lcore/get-header-information zipper) => (contains {:pcc "0O0G"}))
  (fact "check how we get conversation id"
    (lcore/get-header-information zipper) =>
    (contains {:conversation_id "d8da88d4-88bb-11e5-a341-0eebf1123529"}))
  (fact "check how we get timestamp"
    (lcore/get-header-information zipper) =>
    (contains {:timestamp "2015-11-11T16:30:42Z"}))

  (fact "parse xml method name"
    (lcore/get-xml-method-name zipper) => :EndTransactionRQ)

  (fact "Test how this merging extractors works"
    (lcore/extract-data ..zipper..) => {:a 1, :d 2}
    (provided
      (lcore/get-xml-method-name ..zipper..) => ..x..
      (lcore/PARSING-MAPPING-RULES ..x..) =>
      '(lcore/get-endtransaction-mode lcore/get-header-information)
      (lcore/get-header-information anything) => {:a 1}
      (lcore/get-endtransaction-mode anything) => {:d 2}))

  (fact "test end_transaction parser"
    (lcore/get-endtransaction-mode zipper) => {:end true})

  (fact "test allable zipper"
    (lcore/extract-data zipper) =>
    (contains {:conversation_id "d8da88d4-88bb-11e5-a341-0eebf1123529",
               :end true, :pcc "0O0G", :timestamp "2015-11-11T16:30:42Z"})))
