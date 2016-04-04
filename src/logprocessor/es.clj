(ns logprocessor.es
  (:require [clj-time.format :as f]
            [clojure
             [set :as set]
             [string :as string]]
            [clojure.data.json :as json]
            [environ.core :refer [env]]
            [logprocessor.utils :as u]
            [manifold.deferred :as d]
            [org.httpkit.client :as http]))

(def es-bulk-size 100)

(def es-index-conf
  {:settings
   {:number_of_shards 1
    :analysis
    {:analyzer
     {:readxml
      {:type :custom
       :char_filter ["html_strip"]
       :tokenizer :classic
       :filter :standard}}}}
   :mappings
    {:request
     {:_all {:enabled false}
      :properties
      {:id {:type :string :index :analyzed}
       :pcc {:type :string :index :analyzed}
       :message-id {:type :string :index :analyzed}
       :name {:type :string :index :analyzed}
       :service {:type :string :index :analyzed}
       :session-id {:type :string :index :analyzed}
       :timestamp {:type :date}
       :raw {:type :string :analyzer :readxml}
       :Ind {:type :boolean}}}}})

;; @(http/delete (es-url "titan-2015.11"))
;; @(create-index! "titan-2015.11")

(defn es-url
  [slug]
  (format "%s/%s" (env :es-url) slug))

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
    @(apply d/zip (map create-index! indices))))

(defn put-bulk-items!
  "Use bulk api for putting many items. Return items inserted."
  [items msg!]
  (try
    (let [rsp @(d/chain
                (http/put (es-url "_bulk") {:body items}) :body
                json/read-str #(get % "items"))]
      (let [errs (filter #(get-in % ["index" "error" "reason"]) rsp)]
        (if (seq errs)
          (throw (Exception. (string/join "\n" errs))))))
    (catch Exception e
      (msg! :exc {:type :es-bulk :err (str e)}))))

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
  [{:keys [message-id date]}]
  {:index
   {:_id message-id :_type "sabre"
    :_index (->> date (f/unparse (f/formatter "Y.M")) (format "titan-%s"))}})

(defn iter-es-bulk-documents
  "Generates seq for ES bulk API, param should lazy-seq"
  [items msg!]
  (try
    (str (->>
          items
          (prepend-each-item-with create-operation-header)
          (map #(dissoc % :date))
          (map json/write-str)
          (string/join "\n")) "\n")
    (catch Exception e
      (msg! :exc {:type :es-prepare :err (str e)}))))


;; @(create-index! "titan-2015.11")
;; @(http/delete (es-url "titan-2015.11"))
