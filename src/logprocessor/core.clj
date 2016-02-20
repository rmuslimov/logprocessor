(ns logprocessor.core
  (:gen-class)
  (:require [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.java.io :as io]
            [clojure.zip :as zip]
            [criterium.core :refer [bench]]))

(defn get-extractors-for-method
  [method]
  (case method
    :EndTransactionRQ '(lcore/get-header-information lcore/get-endtransaction-mode)
    'lcore/get-header-information))

(defn get-xml-method-name
  [zipper]
  "Just test mode only get example xml file."
  (let [body (zx/xml1-> zipper :Body)]
    (-> body zip/down zip/node :tag)))

(defn parse-endtransaction-mode
  "Special parser for EndTransactionRQ."
  [zipper]
  (let [tag (zx/xml1-> zipper :Body :EndTransactionRQ :EndTransaction zip/node)]
    {:end (->> tag :attrs :Ind (= "true"))}))

(defn parse-error-info
  [zipper]
  "Parser for error in happened."
  (let [error
        (zx/xml1-> zipper :Body zip/down zip/down :Error :SystemSpecificResults)
        status (zx/xml1-> zipper :Body zip/down :ApplicationResults)]
    {:message (get-node-text error :Message)
     :short-text (get-node-text error :ShortText)
     :status (-> status zip/node :attrs :status)}))

(defn get-node-text
  [node & paths]
  "Executes paths from node and get it's text by calling (:content first)."
  (-> (apply zx/xml1-> node paths) zip/node :content first))

(defn get-header-information
  "Get information about session and whatever if exists."
  [zipper]
  (let [header (zx/xml1-> zipper :Header :MessageHeader)]
    {:pcc (get-node-text header :CPAId)
     :conversation_id (get-node-text header :ConversationId)
     :timestamp (get-node-text header :MessageData :Timestamp)}))

(defn extract-data
  "Run all known zipper for certain type of xml structure."
  [zipper]
  (let [method (get-xml-method-name zipper)
        extractors (get-extractors-for-method method)]
    (apply
     merge
     {:method-name method}
     (map #((eval %) zipper) extractors))))

(defn process-xml
  "Read file, run processors"
  [fileobj]
  (with-open [stream (io/input-stream fileobj)]
    (let [zipper (-> stream xml/parse zip/xml-zip)]
      {:filename (.getCanonicalPath fileobj)
       :content (extract-data zipper)})))

(defn -main
  "do nothing for now."
  [& args])
