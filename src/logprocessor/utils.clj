(ns logprocessor.utils
  (:require [amazonica.aws.s3 :as s3]
            [amazonica.core :as aws]
            [clj-time.core :as t]
            [clj-yaml.core :as yaml]
            [fs.core :as fs]))

(def s3-root "lboeing_xml")

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
    (s3/list-objects
     s3-root
     (get-path-by-params level app date))))

;; (get-path-by-params :bcd1 :fokker (t/date-time 2016 2 2))
;; (list-s3-objects :bcd1 :fokker (t/date-time 2016 2 2))
;; (s3/list-objects s3-root "bcd1/fokker/y=2016/m=02/d=02/")

(defn get-s3-object
  ""
  [key]
  (aws/with-credential (get-creds)
    (->> key (s3/get-object s3-root) :object-content slurp)))

(defn walk-over-s3
  ([level app date]
   (walk-over-s3
    (let [{objs :object-summaries} (list-s3-objects level app date)]
      (map :key objs))))
  ([entities]
   (lazy-seq
    (cons {:source (get-s3-object (first entities))
           :name (first entities)}
          (walk-over-s3 (rest entities))))))
