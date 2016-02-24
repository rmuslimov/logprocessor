(ns logprocessor.parsers
  (:require [clj-xpath.core :as xp]))

(defn parse-method-name
  "Extract node with internal structure"
  [xmldoc]
  (xp/$x:tag ".//*[local-name()='Body']/*" xmldoc))

(defn parse-header-info
  "Get header information"
  [xmldoc]
  (let [namespaces (xp/xmlnsmap-from-root-node xmldoc)
        cnv-xpath ".//MessageHeader//*[local-name()='ConversationId']/text()"
        pcc-xpath ".//MessageHeader/*[local-name()='CPAId']/text()"
        service-xpath ".//MessageHeader/*[local-name()='Service']/text()"
        refto-xpath ".//MessageHeader//*[local-name()='RefToMessageId']/text()"]
    {:session-id (xp/$x:text cnv-xpath xmldoc)
     :refto (xp/$x:text refto-xpath xmldoc)
     :service (xp/$x:text service-xpath xmldoc)
     :pcc (xp/$x:text pcc-xpath xmldoc)}))

(defn parse-error-tags
  "Parse error tags if they exist."
  [fileobj]
  nil)

(defn parse-details
  "Parse specific node information based on method type"
  [xmldoc]
  (let [method-name (parse-method-name xmldoc)]
    (case method-name
      :EndTransactionRS {:data 1}
      :OTA_PingRQ {:data 2}
      nil)))

(defn process-file
  "Processing xmldoc return map representing it's structure."
  [xmldoc]
  (let [header (parse-header-info xmldoc)
        errors (parse-error-tags xmldoc)]
    (if errors
      (merge header errors)
      (merge header (or (parse-details xmldoc) {})))))
