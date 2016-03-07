(ns logprocessor.utils
  (:require [amazonica.aws.s3 :as s3]
            [amazonica.core :as aws]
            [clj-time.core :as t]
            [clj-yaml.core :as yaml]
            [com.climate.claypoole :as cp]
            [fs.core :as fs]))

(def s3-root "lboeing_xml")
(def psize 100)
(def net-pool (cp/threadpool 100))
(def cpu-pool (cp/threadpool (cp/ncpus)))

(defn days-range
  "Iter over days in particular year's month."
  [y m]
  (map #(t/date-time y m (inc %))
       (range
        (t/number-of-days-in-the-month y m))))

(defn get-path-by-params
  "Get getgoing styled s3 path for given params"
  [level appname date]
  (format
   "%s/%s/y=%d/m=%02d/d=%02d/"
   (name level) (name appname)
   (t/year date) (t/month date) (t/day date)))

(defn get-creds
  "Return list with access,secret keys from ~/.eagle"
  []
  (let [{{ac :access_key sc :secret_key} :aws}
        (-> "~/.eagle" fs/expand-home slurp yaml/parse-string)]
    (list ac sc)))

(defn list-s3-objects
  "Get full list of available xml on s3."
  [level app date]
  (aws/with-credential (get-creds)
    (let [prefix (get-path-by-params level app date)]
      (loop [items [] marker nil]
        (let [rsp
              (s3/list-objects
               :bucket-name s3-root :prefix prefix :marker marker)]
          (if-not (:truncated? rsp)
            (apply conj items (:object-summaries rsp))
            (recur (apply conj items (:object-summaries rsp))
                   (:next-marker rsp))))))))

(defn list-s3-objects-par
  "Run intensively S3 list-objects function"
  [level app year month]
  (let [items (cp/pmap
               net-pool
               #(list-s3-objects level app %)
               (days-range year month))]
    (loop [pool items acc []]
      (if (empty? pool)
        acc
        (recur (rest pool) (apply conj acc (first pool)))))))

(defn get-s3-object
  ""
  [key]
  (aws/with-credential (get-creds)
    (->> key (s3/get-object s3-root) :object-content slurp)))

(defn walk-over-s3
  "Lasy seq, iterating over s3 file and returning future for loaded s3 file"
  ([level app year month]
   (walk-over-s3
    (map :key (list-s3-objects-par level app year month))))
  ([entities]
   (lazy-seq
    (let [entry (first entities)
          result {:source (fn [] (get-s3-object entry)) :name entry}]
      (if-not (empty? (rest entities))
        (cons result (walk-over-s3 (rest entities))))))))
