(ns logprocessor.processing
  (:require [com.climate.claypoole :as cp]
            [com.stuartsierra.component :as component]
            [logprocessor
             [es :as es]
             [utils :as utils]]
            [manifold
             [deferred :as d]
             [stream :as ms]])
  (:import java.util.concurrent.TimeUnit))

(def psize 100) ;; default size

(defn inc-rep
  "Inform state about made changes"
  [stream kw c]
  (ms/put! stream {:step kw :inc c}))

(defn batch
  "Batch messages from input."
  [in report]
  (let [batch (ms/batch psize in)
        out (ms/stream psize)]
    (ms/connect-via
     batch #(do (inc-rep :found (count %)) (ms/put! out %)) out)
    out))

(defn >go>
  "Reads an input with raw items and parses to our map."
  [in process-fn inc-fn]
  (let [out (ms/stream psize)
        cpool (cp/threadpool (+ (cp/ncpus) 2))]
    (future
      (loop [items @(ms/take! in)]
        (if items
          (do
            (cp/future cpool (do
                               (inc-fn (count items))
                               (ms/put! out (process-fn items))))
            (recur @(ms/take! in)))
          (do
            (cp/shutdown cpool)
            (.awaitTermination cpool 10 TimeUnit/SECONDS)
            (ms/close! out)))))
    out))

(defn- update-kw-async [kw fn pool]
  @(apply d/zip (map #(d/future (update % kw fn)) pool)))

(defn create-system
  "Create system for processing messages to ES."
  [report]
  (let [in (ms/stream psize)
        out (-> in
                (batch report)
                (>go>
                 (partial update-kw-async :source (fn [f] (f)))
                 (partial inc-rep report :dwn))
                (>go> (partial map u/process-item) (partial inc-rep report :prc))
                (>go> es/iter-es-bulk-documents (partial inc-rep report :toes))
                (>go> es/put-bulk-items! (partial inc-rep report :blk)))]
    (list in out report)))

(defn load-documents!
  "Execute loading using system and docs lazy sequence."
  [docs report]
  (let [[in out] (create-system report)
        p (promise)]
    (ms/consume identity out)
    (ms/on-closed out #(deliver p out))
    (ms/connect (ms/->source docs) in)
    p))

(defn consume-tasks
  [report [level app y m & d]]
  @(es/create-indices y m)
  (load-documents! (utils/walk-over-s3 level app y m d) report))

(defrecord ESLoading [tasks-queue]
  component/Lifecycle
  (start [self]
    (let [report (ms/stream 1e5)]
      (ms/consume (partial consume-tasks report) tasks-queue)
      (assoc self :report report)))

  (stop [self]
    (ms/close! (:report self))))
