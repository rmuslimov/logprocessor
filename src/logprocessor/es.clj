(ns logprocessor.es
  (:require [clj-time.core :as t]
            [clojure.data.json :as json]
            [clojure.string :as string]
            [logprocessor.utils :as utils]
            [org.httpkit.client :as http]))

(def elastic-url "http://lf:9200")
(def es-bulk-size 100)

(def es-mapping
  {"request"
   {:_all {:enabled false}
    :properties
    {:id {:type :string :index :not_analyzed}
     :pcc {:type :string :index :not_analyzed}
     :message-id {:type :string :index :not_analyzed}
     :service {:type :string :index :not_analyzed}
     :session-id {:type :string :index :not_analyzed}
     :timestamp {:type :date}
     :Ind {:type :boolean}
     }}})

(defn es-url
  "Simple formatting for ES queries"
  [slug]
  (format "%s/%s" elastic-url slug))

(defn rewrite-index!
  "Drop index if exists before, add it again with new mapping"
  [name]
  (let [iurl (es-url name)
        exists? (= (-> @(http/head iurl) :status) 200)]
    ;; drop if exists
    (when exists? @(http/delete iurl))
    ;; create index
    (http/put
     iurl
     {:body
      (json/write-str
       {:settings {:number_of_shards 1}
        :mappings es-mapping})})))

(defn put-item!
  "Put one item to index"
  [idx _type item]
  (let [id (:message-id item)]
    (http/put
     (es-url (format "%s/%s/%s" idx _type id))
     {:body (json/write-str (dissoc item :message-id))})))

(defn put-bulk-items!
  "Use bulk api for putting many items"
  [idx _type items]
  (http/put
   (es-url (format "%s/%s/_bulk" idx _type)) {:body items}))

(defn prepend-each-item-with
  [prepend-func items]
  (loop [items items result []]
    (if (empty? items)
      result
      (recur
       (rest items)
       (conj result (prepend-func (first items)) (first items))))))

(defn iter-es-bulk-documents
  "Generates seq for ES bulk API, param should lazy-seq"
  [data]
  (->>
   data
   utils/intensive-processing-items
   (map #(assoc % :id (:message-id %)))
   (prepend-each-item-with (fn [x] {:index {:_id (:id x)}}))
   (map json/write-str)
   (partition-all es-bulk-size)
   (map #(string/join "\n" %))))

;; Insert all documents from zip
(->>
 (dev/walk-over-file "examples.zip")
 ;; (utils/walk-over-s3 :bcd2 :cessna (t/date-time 2016 2 22))
 iter-es-bulk-documents
 (map #(put-bulk-items! "l5" "sabre" %)))

;; (utils/intensive-processing-items
;;  (utils/walk-over-s3 :bcd2 :cessna (t/date-time 2016 2 22)))

;; @(rewrite-index! "l5")
;; @(http/delete (es-url "l5"))
;; (-> @(http/head (es-url "l1")) :status)
