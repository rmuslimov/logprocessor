(ns logprocessor.parsers
  (:require [clj-xpath.core :as xp]))

(defn parse-method-name
  "Extract node with internal structure"
  [xmldoc]
  (xp/$x:tag ".//*[local-name()='Body']/*" xmldoc))

(defn parse-header-info
  "Get header information"
  [xmldoc]

  (defn- extract-text
    [tagname]
    (xp/$x:text
     (format ".//MessageHeader//*[local-name()='%s']/text()" tagname)
     xmldoc))

  {:session-id (extract-text "ConversationId")
   :refto (extract-text "RefToMessageId")
   :service (extract-text "Service")
   :pcc (extract-text "CPAId")})

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
