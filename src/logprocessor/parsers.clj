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
  (first
   (xp/$x:text*
    (format ".//MessageHeader//*[local-name()='%s']/text()" tagname) xmldoc)))

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

(defn parse-et-rq
  "Parse details of EndTransactionRQ"
  [subnode]
  (xp/with-namespace-context (xp/xmlnsmap-from-node subnode)
    (let [ind (->> subnode (xp/$x:attrs ".//EndTransaction[@Ind]") :Ind)]
      {:Ind (= ind "true")})))

(defn parse-retrieve-rq
  "Parsing TravelItineraryReadRQ request"
  [subnode]
  (xp/with-namespace-context (xp/xmlnsmap-from-node subnode)
    (let [id (->> subnode (xp/$x:attrs ".//UniqueID[@ID]") :ID)]
      {:id id})))

(defn parse-details
  "Parse specific node information based on method type"
  [subdoc method-name]
  (case method-name
    :EndTransactionRQ (parse-et-rq subdoc)
    :TravelItineraryReadRQ (parse-retrieve-rq subdoc)
    nil))

(defn process-file
  "Processing xmldoc return map representing it's structure."
  [xmldoc]
  (let [header (parse-header-info xmldoc)
        method-name (parse-method-name xmldoc)
        subdoc (extract-body-node xmldoc)
        errors (parse-error-info subdoc)]
    (if-not (empty? errors)
      (merge header {:errors errors})
      (merge header
             (or (parse-details subdoc method-name) {})))))
