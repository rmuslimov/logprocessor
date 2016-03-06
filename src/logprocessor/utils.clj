(ns logprocessor.utils
  (:require [amazonica.aws.s3 :as s3]
            [amazonica.core :as aws]
            [clj-time.core :as t]
            [clj-yaml.core :as yaml]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [com.climate.claypoole :as cp]
            [fs.core :as fs]
            [logprocessor.core :as core]
            [user :as dev]))

(def s3-root "lboeing_xml")
(def psize 100)

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

(defn get-s3-object
  ""
  [key]
  (aws/with-credential (get-creds)
    (->> key (s3/get-object s3-root) :object-content slurp)))

(defn walk-over-s3
  "Lasy seq, iterating over s3 file and returning future for loaded s3 file"
  ([level app date]
   (walk-over-s3
    (map :key (list-s3-objects level app date))))
  ([entities]
   (lazy-seq
    (let [entry (first entities)
          result {:source (fn [] (get-s3-object entry)) :name entry}]
      (if (empty? (rest entities))
        (list result)
        (cons result (walk-over-s3 (rest entities))))))))

(defn process
  "Get list of item to process, execute f in :source of each item using th"
  [items]
  (flatten
   (doall
    (map
     (fn [it] (cp/pmap core/net-pool #(update % :source (fn [f] (f))) it))
     (partition psize psize nil items)))))

;; Pull all xml for date, run processing on them and count number
;; (time (reduce + (map count (process (walk-over-s3 :bcd2 :cessna (t/date-time 2016 2 22))))))
;; (walk-over-s3 :bcd2 :cessna (t/date-time 2016 2 22))

;; Parallel version with processing xml: 2.4 sec
;; (time
;;  (doall
;;   (cp/pmap
;;    core/cpu-pool
;;    core/process-item
;;    (process (dev/walk-over-file "examples.zip")))))

;; Single thread processing 9.4 sec
;; (time (count
;;        (map core/process-item
;;          (process (dev/walk-over-file "examples.zip")))))

(defn intensive-processing-items
  "Process files and prepare for ES"
  [data]
  (cp/upmap core/cpu-pool core/process-item (process data)))
