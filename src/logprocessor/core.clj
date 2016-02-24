(ns logprocessor.core
  (:gen-class)
  (:require [clj-xpath.core :as xp]
            [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.java.io :as io]
            [clojure.zip :as zip]
            [logprocessor.parsers :as parsers]))

(defn get-zipper
  [fileobj]
  (-> fileobj io/input-stream xml/parse zip/xml-zip))

(defn get-xml-method-name
  "Just test mode only get example xml file."
  [zipper]
  (let [body (zx/xml1-> zipper :Body)]
    (-> body zip/down zip/node :tag)))

(defn get-header-information
  "Get information about session and whatever if exists."
  [zipper]
  (let [header (zx/xml1-> zipper :Header :MessageHeader)]
    {:pcc (parsers/get-node-text header :CPAId)
     :conversation_id (parsers/get-node-text header :ConversationId)
     :timestamp (parsers/get-node-text header :MessageData :Timestamp)}))



(defn -main
  "Do nothing for now."
  [& args])
