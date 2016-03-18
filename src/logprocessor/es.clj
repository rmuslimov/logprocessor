(ns logprocessor.es
  (:require [clj-time
             [core :as t]
             [format :as f]]
            [clojure
             [set :as set]
             [string :as string]]
            [clojure.data.json :as json]
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
     :name {:type :string :index :not_analyzed}
     :service {:type :string :index :not_analyzed}
     :session-id {:type :string :index :not_analyzed}
     :timestamp {:type :date}
     :Ind {:type :boolean}
     }}})

(defn es-url
  "Simple formatting for ES queries."
  [slug]
  (format "%s/%s" elastic-url slug))

(defn get-existing-indices
  ""
  []
  (->
   @(http/get (es-url "_aliases")) :body json/read-str keys))

(defn create-index!
  "Create index if it doesn't exists."
  [name]
  @(http/put
    (es-url name)
    {:body
     (json/write-str
      {:settings {:number_of_shards 1}
       :mappings es-mapping})}))

(defn put-bulk-items!
  "Use bulk api for putting many items. Return items inserted."
  [items]
  (->
   @(http/put (es-url "_bulk") {:body items})
   :body json/read-str (get "items") count))

(defn prepend-each-item-with
  [prepend-func items]
  (loop [items items result []]
    (if (empty? items)
      result
      (recur
       (rest items)
       (conj result (prepend-func (first items)) (first items))))))

(defn get-index-name
  "Create index name from date object."
  [sdate]
  (format
   "%s-%s" "titan"
   (f/unparse (f/formatter "Y.M") sdate)))

(defn create-operation-header
  "Create ES bulk api consistent header for json doc given."
  [x]
  {:index {:_id (:message-id x)
           :_type "sabre"
           :_index (get-index-name (:date x))}})

(defn iter-es-bulk-documents
  "Generates seq for ES bulk API, param should lazy-seq"
  [items]
  (->>
   items
   (prepend-each-item-with create-operation-header)
   (map #(dissoc % :date))
   (map json/write-str)
   (partition-all es-bulk-size)
   (map #(str (string/join "\n" %) "\n"))))

(defn create-required-indices!
  "Based on list of items, gets unique list of indices required and creates them."
  [items]
  (let [existing (set (get-existing-indices))
        uniq-pairs
        (set
         (map (fn [x] [(t/year (:date x)) (t/month (:date x))]) items))
        indices
        (set
         (map
          #(get-index-name (t/date-time (first %) (second %) 1)) uniq-pairs))
        to-create (set/difference indices existing)]
    (doall (map create-index! to-create))
    to-create))


;; (create-index! "titan-2015.11")
;; (defn put-documents-to-es-index
;;   "Put documents to index"
;;   [docs]
;;   (let [pool (utils/intensive-processing-items docs)]
;;     (->>
;;      (remove :exception pool)
;;      ;; generate proper ES documents
;;      (iter-es-bulk-documents))))

;;     ;; {:inserted
;;     ;;  (reduce +
;;     ;;           ;; run bulk API inserts
;;     ;;           ;; (cp/upmap utils/net-pool put-bulk-items!)))
;;     ;;  :exceptions (filter :exception pool)
;;     ;;  }))
;; @(reported-processing (dev/walk-over-file "examples.zip"))
;; (require '[logprocessor.utils :as utils])
;; (require '[user :as dev])
;; (time (put-documents-to-es-index (utils/walk-over-s3 :bcd2 :fokker 2016 2)))
;; (time (put-documents-to-es-index vvv))
;; (def vvv (utils/walk-over-s3 :bcd2 :fokker 2016 2))
;; (count (doall vvv))
;; @(http/delete (es-url "titan-2015.11"))
