(ns logprocessor.parsers
  (:require [clj-xpath.core :as xp]))

(defn parse-method-name
  "Extract node with internal structure"
  [xmldoc]
  (xp/$x:tag ".//*[local-name()='Body']/*" xmldoc))

(defn extract-body-node
  "Sabre xml has internal xmlns with specific details inside."
  [xmldoc]
  (xp/$x:node ".//*[local-name()='Body']/*" xmldoc))

(defn- extract-mh-subtext
  [tagname xmldoc]
  (xp/$x:text
   (format ".//MessageHeader//*[local-name()='%s']/text()" tagname) xmldoc))

(defn parse-header-info
  "Get header information for sabre files"
  [xmldoc]
  {:session-id (extract-mh-subtext "ConversationId" xmldoc)
   :message-id (extract-mh-subtext "MessageId" xmldoc)
   :refto (extract-mh-subtext "RefToMessageId" xmldoc)
   :service (extract-mh-subtext "Action" xmldoc)
   :timestamp (extract-mh-subtext "Timestamp" xmldoc)
   :pcc (extract-mh-subtext "CPAId" xmldoc)})

(defn parse-error-info
  "Parse error tags if they exist."
  [subnode]
  (xp/with-namespace-context (xp/xmlnsmap-from-node subnode)
    (xp/$x:text* ".//Message/text()" subnode)))

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
        subdoc (extract-body-node xmldoc)
        errors (parse-error-info subdoc)]
    (if errors
      (merge header {:errors errors})
      (merge header (or (parse-details subdoc) {})))))
