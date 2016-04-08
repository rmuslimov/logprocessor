(ns logprocessor.parsers
  (:require [clj-time.format :as f]
            [clj-time.core :as t]
            [clj-xpath.core :as xp]))

(def sabre-ts (f/formatters :date-hour-minute-second))

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
  "This is hack for allowance us using PST timezone in request,
   and utc in responses."
  [value]
  (let [parsed (f/parse value)
        ts (if (.endsWith value "Z")
             (t/from-time-zone parsed (t/time-zone-for-id "America/Los_Angeles"))
             parsed)]
    (f/unparse (f/formatter "yyyy-MM-dd'T'HH:mm:ss") ts)))

(defn parse-header-info
  "Get header information for sabre files"
  [xmldoc]
  (let [refto (extract-mh-subtext "RefToMessageId" xmldoc)
        ts (if-let [ts (extract-mh-subtext "Timestamp" xmldoc)] (clean-ts ts))]
    {:session-id (extract-mh-subtext "ConversationId" xmldoc)
     :message-id (extract-mh-subtext "MessageId" xmldoc)
     :service (extract-mh-subtext "Action" xmldoc)
     :timestamp ts :refto refto
     :pcc (extract-mh-subtext "CPAId" xmldoc)}))

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
