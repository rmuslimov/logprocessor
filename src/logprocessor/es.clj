(ns logprocessor.es
  (:require [clojure.data.json :as json]
            [org.httpkit.client :as http]))

(def elastic-url "http://lf:9200")

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

(defn put-items!
  "Use bulk api for putting many items"
  []
  )


;; @(rewrite-index! "l5")
;; @(http/delete (es-url "l5"))
;; (-> @(http/head (es-url "l1")) :status)
