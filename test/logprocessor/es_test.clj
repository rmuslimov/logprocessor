(ns logprocessor.es-test
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [logprocessor
             [core :as core]
             [es :as es]]
            [midje.sweet :refer :all]))

;; (fact "Check creating proper header json for document."
;;   (es/create-operation-header
;;    {:message-id 1 :date (f/parse core/sabre-ts "2015-11-12T17:50:27")}) =>
;;   {:index {:_id 1, :_index "titan-2015.11", :_type "sabre"}})

(fact "Check prepend documents with operational json"
  (es/prepend-each-item-with #(inc %) (range 3)) => [1 0 2 1 3 2])

(facts "Check generating proper bulk API header"
  (es/iter-es-bulk-documents
   [{:message-id 1 :date (t/date-time 2016 2 2)}]) =>
  (list "{\"index\":{\"_id\":1,\"_type\":\"sabre\",\"_index\":\"titan-2016.2\"}}\n{\"message-id\":1}\n"))

(fact "Check if code creating ES indices properly based on calls list."
  (es/create-required-indices!
   [{:date (t/date-time 2016 2 1)}
    {:date (t/date-time 2016 3 1)}]) => #{"titan-2016.2", "titan-2016.3"}

  ;; Mocking ES calls
  (provided
    (es/create-index! anything) => nil :times 2
    (es/get-existing-indices) => [] :times 1))
