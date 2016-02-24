(ns logprocessor.parsers
  (:require [clj-xpath.core :as xp]
            [midje.sweet :refer [fact]]))

;; (defn get-node-text
;;   [node & paths]
;;   "Executes paths from node and get it's text by calling (:content first)."
;;   (-> (apply zx/xml1-> node paths) zip/node :content first))

;; (defn parse-error-info
;;   "Parser for error in happened."
;;   [zipper]
;;   (let [error
;;         (zx/xml1-> zipper :Body zip/down zip/down :Error :SystemSpecificResults)
;;         status (zx/xml1-> zipper :Body zip/down :ApplicationResults)]
;;     {:message (get-node-text error :Message)
;;      :short-text (get-node-text error :ShortText)
;;      :status (-> status zip/node :attrs :status)}))

;;; clj-xpath oriented methods

;; (defn get-method-node
;;   "Extract node with internal structure"
;;   [fileobj]
;;   (let [namespaces (xp/xmlnsmap-from-root-node fileobj)]
;;     (xp/with-namespace-context namespaces
;;       (xp/$x:node ".//*[local-name()='Body']" fileobj))))

;; (defn execute-xpath
;;   "Execute clj-xpath search with namespace applied."
;;   [fn node path]
;;   (xp/with-namespace-context (xp/xmlnsmap-from-node node)
;;     (fn path node)))

;; (defn parse-end-transaction-mode
;;   "Special parser for EndTransactionRQ."
;;   [node]
;;   (let [attrs (execute-xpath xp/$x:attrs node "./EndTransaction")]
;;     {:end (->> attrs :Ind (= "true"))}))

;; proto oriented code

(defprotocol SabreXml
  "Basic Sabre XML representation protocol"
  (parse [x]))

(deftype EndTransaction [xmldoc]
  SabreXml
  (parse [x]
    (merge {:b 2} {:a 1})))
