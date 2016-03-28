(ns logprocessor.processing
  (:require [logprocessor
             [es :as es]
             [utils :as u]]
            [manifold
             [deferred :as d]
             [stream :as ms]]
            [user :as dev]))

(def psize 100) ;; default size

(defn batch
  "Batch messages from input."
  [in msg! size]
  (let [out (ms/stream 1e3)]
    (d/loop [acc []]
      (let [drop? (= (count acc) size)
            restv (if drop? [] acc)
            put-out! (fn [v] (msg! (count v)) (ms/put! out v))]
        (when drop? (put-out! acc))
        (->
         (d/chain
          (ms/take! in)
          #(if %
             (d/recur (conj restv %))
             (if-not (empty? restv)
               (d/chain
                (put-out! restv)
                (fn [x] (when x (ms/close! out)))
                ))))
         (d/catch #(println "err" %))
         )))
    out))

(defn- connect
  ""
  [source tmp sink ufn msg! & [kw]]
  (d/loop []
    (d/chain
     (ms/take! source)
     (fn [v]
       (when v
         (ms/put! tmp v)
         (->
          (d/future (ufn v))
          (d/chain
           (fn [v] (do (when kw (msg! kw (count v))) v))
           (fn [v] (ms/put! sink v))
           (fn [_] @(ms/take! tmp)))
          (d/catch #(msg! :error %)))
         (d/recur))))))

(defn ||process
  ""
  [in N xf msg! & [kw]]
  (let [out (ms/stream 100)
        ls (repeatedly N ms/stream)
        live (atom N)]
    (doseq [s' ls]
      (connect in s' out xf msg! kw)
      (ms/on-drained
       s'
       (fn []
         (swap! live dec)
         (when (= 0 @live) (ms/close! out))))
      ;; close all listeners whem in down
      (ms/on-drained in #(doseq [s' ls] (ms/close! s'))))
    out))

(defn- update-kw-async [kw fn pool]
  @(apply d/zip (map #(d/future (update % kw fn)) pool)))

(defn create-streams
  "Create system for processing messages to ES."
  [report uid]
  (let [in (ms/stream psize)
        msg! (partial u/msg! report uid)
        update-source-fn (partial update-kw-async :source (fn [f] (f)))
        out (-> in
                (batch (partial msg! :found) psize)
                (||process 1 update-source-fn msg! :dwn)
                (||process 1 #(doall (map u/process-item %)) msg! :prc)
                (||process 1 es/iter-es-bulk-documents msg! :toes)
                (||process 1 es/put-bulk-items! msg! :blk))]
    (ms/on-closed out #(msg! :message "Process finished"))
    (list in out)))

(def report (ms/stream))
(ms/consume #(println "rep:" %) report)
(def docs (dev/walk-over-file "examples.zip"))
(def broken (take 3 (dev/walk-over-file "broken.zip")))

;; (def strs (create-streams report "asdas"))
;; (ms/consume identity (second strs))
;; (ms/connect (ms/->source broken) (first strs))
;; (ms/connect (ms/->source docs) (first strs))

;; (map u/process-item (list {:source "adssad", :name "d=12/soap-request-1a32ctfFVgwFjF8Kg9mc4H.xml"}))

;; org.xml.sax.SAXParseException
;; (defn load-documents!
;;   "Execute loading using system and docs lazy sequence."
;;   [docs report uid]
;;   (let [[in out] (create-streams report uid)]
;;     (ms/consume identity out)
;;     (ms/connect (ms/->source docs) in)))

;; (defrecord ESLoading [queue report]
;;   component/Lifecycle
;;   (start [self]
;;     (let [l (ms/stream) {r :report q :queue} self
;;           msg! (partial u/msg! r)]
;;       (println "Hey!")
;;       (ms/connect q l)
;;       (ms/consume
;;        (fn [{docs :docs uid :uid}]
;;          (try
;;            (do
;;              (msg! uid :message "Loading process has started...")
;;              (load-documents! docs r uid))
;;            (catch Exception exc (msg! uid :error exc))))
;;        l)
;;       (assoc self :listener l)))

;;   (stop [self]
;;     (let [{l :listener} self]
;;       (ms/close! l)
;;       (dissoc self :listener))))
