(ns logprocessor.utils
  (:require [amazonica.aws.s3 :as s3]
            [amazonica.core :as aws]
            [clj-time
             [core :as t]
             [format :as f]]
            [clj-yaml.core :as yaml]
            [com.climate.claypoole :as cp]
            [fs.core :as fs]
            [logprocessor.parsers :as p]
            [manifold
             [deferred :as d]
             [stream :as ms]]))

(def s3-root "lboeing_xml")
(def sabre-ts (f/formatters :date-hour-minute-second))

(def
  ^{:doc "Each SOAP may have extension with returns specific information."}
  details-mapping
  {:EndTransactionRQ p/parse-et-rq
   :TravelItineraryReadRQ p/parse-retrieve-rq})

(defn- load-creds []
  (let [{{ac :access_key sc :secret_key} :aws}
        (-> "~/.eagle" fs/expand-home slurp yaml/parse-string)]
    (list ac sc)))

(def get-creds
  "Return list with access,secret keys from ~/.eagle"
  (memoize load-creds))

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
      {:exception e :filepath (:name item)})))

(defn dates-range
  "Iter over days in particular year's month."
  [y m]
  (map #(t/date-time y m (inc %))
       (range (t/number-of-days-in-the-month y m))))

(defn gen-s3-prefix
  "Get getgoing styled s3 path for given params"
  [level appname date]
  (format
   "%s/%s/y=%d/m=%02d/d=%02d/" (name level) (name appname) (t/year date) (t/month date) (t/day date)))

(defn list-s3-objects-for-date
  "Get full list of available xml on s3 for certain date."
  [level app date]
  (aws/with-credential (get-creds)
    (let [prefix (gen-s3-prefix level app date)]
      (loop [items [] marker nil]
        (let [rsp
              (s3/list-objects
               :bucket-name s3-root :prefix prefix :marker marker)]
          (if-not (:truncated? rsp)
            (apply conj items (:object-summaries rsp))
            (recur (apply conj items (:object-summaries rsp))
                   (:next-marker rsp))))))))

(defn list-s3-objects
  "Get lazy-seq over all files stored with given prefix."
  [level app y m & [d]]
  (let [dates (if d (list (t/date-time y m d)) (dates-range y m))]
    @(d/chain
      (apply
       d/zip
       (map #(d/future (list-s3-objects-for-date level app %)) dates))
      (partial mapcat identity))))

;; (count (list-s3-objects :bcd1 :cessna 2016 2))

(defn get-s3-object
  ""
  [key]
  (aws/with-credential (get-creds)
    (->> key (s3/get-object s3-root) :object-content slurp)))

(defn walk-over-s3
  "Lasy seq, iterating over s3 file and returning future for loaded s3 file"
  ([level app y m & [d]]
   (walk-over-s3
    (map :key (list-s3-objects level app y m d))))
  ([entities]
   (lazy-seq
    (let [entry (first entities)
          result {:source (fn [] (get-s3-object entry)) :name entry}]
      (if-not (empty? (rest entities))
        (cons result (walk-over-s3 (rest entities))))))))

;; (time (def l (walk-over-s3 :bcd1 :fokker 2016 2)))
;; (count l)
