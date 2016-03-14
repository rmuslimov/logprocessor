(ns logprocessor.core
  (:gen-class)
  (:require [clj-time.format :as f]
            [clojure.core.async :as a]
            [com.climate.claypoole :as cp]
            [logprocessor
             [parsers :as p]
             [utils :as utils]]))

(def sabre-ts (f/formatters :date-hour-minute-second))

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
        parse-details (details-mapping (p/parse-method-name xmldoc))
        header (p/parse-header-info xmldoc)
        date (->> header :timestamp)]
    (when-not date
      (throw (Exception. (format "Incorrect date: %s" (->> header :timestamp)))))
    (merge
     header
     {:date (f/parse sabre-ts date)}
     (if-not (empty? errors)
       {:errors errors}
       (if parse-details
         (parse-details subdoc))))))

(defn process-item
  "Item should be dict with name and source"
  [item]
  (try
    (-> item :source process-file (assoc :name (:name item)))
    (catch Exception e
      {:exception e
       :filepath (:name item)})))

(defn process
  "Get list of item to process, execute f in :source of each item using th"
  [items]
  (mapcat
   identity
   (for [item (partition utils/psize utils/psize nil items)]
     (cp/upmap utils/net-pool #(update % :source (fn [f] (f))) item))))

(defn intensive-processing-items
  "Process files and prepare for ES"
  [chan data]
  (if-let [count (count data)]
    (a/>!! chan (format "Intense Received: %s" count)))
  (cp/upmap utils/cpu-pool process-item (process data)))

(defn -main
  "Do nothing for now."
  [& args])
