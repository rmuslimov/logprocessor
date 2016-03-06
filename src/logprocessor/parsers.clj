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

(defn clean-ts
  "Drop Z if exists in the end."
  [ts]
  (if (.endsWith ts "Z")
    (subs ts 0 (-> ts .length dec))
    ts))

(defn parse-header-info
  "Get header information for sabre files"
  [xmldoc]
  {:session-id (extract-mh-subtext "ConversationId" xmldoc)
   :message-id (extract-mh-subtext "MessageId" xmldoc)
   :refto (extract-mh-subtext "RefToMessageId" xmldoc)
   :service (extract-mh-subtext "Action" xmldoc)
   :timestamp (clean-ts (extract-mh-subtext "Timestamp" xmldoc))
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
