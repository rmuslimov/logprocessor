(ns logprocessor.es-test
  (:require [clj-time.core :as t]
            [logprocessor.es :as es]
            [midje.sweet :refer :all]))

(fact
  (es/create-operation-header {:date (t/date-time 2016 2 2)}) =>
  {:index {:_id nil, :_type "sabre", :_index "titan-2016.2"}})

(fact "Check prepend documents with operational json"
  (es/prepend-each-item-with #(inc %) (range 3)) => [1 0 2 1 3 2])

(facts "Check generating proper bulk API header"
  (es/iter-es-bulk-documents
   [{:message-id 1 :date (t/date-time 2016 2 2)}]) =>
  (list "{\"index\":{\"_id\":1,\"_type\":\"sabre\",\"_index\":\"titan-2016.2\"}}\n{\"message-id\":1}\n"))
