(ns logprocessor.es
  (:require [clj-time.format :as f]
            [clojure
             [set :as set]
             [string :as string]]
            [clojure.data.json :as json]
            [logprocessor.utils :as u]
            [manifold.deferred :as d]
            [org.httpkit.client :as http]))

(def elastic-url "http://lf:9200")
(def es-bulk-size 100)

(def es-index-conf
  {:settings {:number_of_shards 1}
   :mappings
    {:request
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
       }}}})

(defn es-url
  [slug]
  (format "%s/%s" elastic-url slug))

(defn get-existing-indices
  []
  (d/chain (http/get (es-url "_aliases")) :body json/read-str keys))

(defn get-indices-names
  "Get names of required to create months indices."
  [y m]
  (let [months
        (case m
          1 [12 1 2]
          12 [11 12 13]
          (for [fn (list dec identity inc)] (fn m)))]
    (map #(format "titan-%d.%02d" y %) months)))

(defn- create-index!
  "Create index if it doesn't exists. Returns manifold/deferred item."
  [name]
  (http/put (es-url name) {:body (json/write-str es-index-conf)}))

(defn create-indices
  [y m & {:keys [rewrite] :or {:rewrite false}}]
  (let [names (set (get-indices-names y m))
        existing (set @(get-existing-indices))
        indices (set/difference names existing)]
    (when rewrite
      @(apply d/zip (map #(http/delete (es-url %)) indices)))
    (apply d/zip (map create-index! indices))))

(defn put-bulk-items!
  "Use bulk api for putting many items. Return items inserted."
  [items]
  (d/chain
   (http/put (es-url "_bulk") {:body items}) :body
   json/read-str #(get % "items")))

(defn prepend-each-item-with
  [prepend-func items]
  (loop [items items result []]
    (if (empty? items)
      result
      (recur
       (rest items)
       (conj result (prepend-func (first items)) (first items))))))

(defn create-operation-header
  "Create ES bulk api consistent header for json doc given."
  [m]
  {:index
   {:_id (:message-id m) :_type "sabre"
    :_index (->> m :date (f/unparse (f/formatter "Y.M")) (format "titan-%s"))}})

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
   ;; #(str (string/join "\n" %) "\n")))

;; (create-index! "titan-2015.11")
