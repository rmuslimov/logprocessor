(ns logprocessor.parsers-test
  (:require [clojure.java.io :as io]
            [clj-xpath.core :as xp]
            [logprocessor
             [core-test :as tcore]
             [parsers :as sut]]
            [midje.sweet :refer :all]))

(defn get-xml-example
  "Just get xml example"
  [name]
  (-> tcore/test-error-xml io/resource slurp))

(background
 (around
  :facts
  (let [err-example (get-xml-example tcore/test-error-xml)
        et-example (get-xml-example tcore/test-et-xml)]
    ?form)))

(fact
  (let [xmldoc (-> err-example xp/xml->doc)]
    (.parse (sut/map->EndTransaction xmldoc))) => 1)

;; (fact
;;   (sut/execute-xpath xp/$x:text method-node-err "//Message/text()") =>
;;   "PREVIOUS ENTRY IN PROGRESS, PLEASE WAIT")

;; (fact
;;   (sut/parse-end-transaction-mode method-node-et) =>
;;   {:end true})

;; (fact
;;   (sut/parse-error-info zipper-err) =>
;;   {:status "NotProcessed" :message "PREVIOUS ENTRY IN PROGRESS, PLEASE WAIT"
;;    :short-text "ERR.SWS.HOST.ERROR_IN_RESPONSE"})
