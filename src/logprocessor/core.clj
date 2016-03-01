(ns logprocessor.core
  (:gen-class)
  (:require [com.climate.claypoole :as cp]
            [logprocessor.parsers :as p]))

(def net-pool (cp/threadpool 100))

(def
  ^{:doc "Each SOAP may have extension with returns specific information."}
  details-mapping
  {:EndTransactionRQ p/parse-et-rq
   :TravelItineraryReadRQ p/parse-retrieve-rq})

(defn process-file
  "Processing xmldoc return map representing it's structure."
  [xmldoc]
  (let [subdoc (p/extract-body-node xmldoc)
        errors (p/parse-error-info subdoc)
        parse-details (details-mapping (p/parse-method-name xmldoc))]
    (merge
     (p/parse-header-info xmldoc)
     (if-not (empty? errors)
       {:errors errors}
       (if parse-details
         (parse-details subdoc))))))

(defn process-item
  "item should be dict with name and source"
  [item]
  (try
    (process-file (:source item))
    (catch Exception e
      {:exception e
       :filepath (:name item)})))

(defn -main
  "Do nothing for now."
  [& args])
