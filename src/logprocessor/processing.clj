(ns logprocessor.processing
  (:require [com.climate.claypoole :as cp]
            [com.stuartsierra.component :as component]
            [logprocessor
             [es :as es]
             [utils :as u]]
            [manifold
             [deferred :as d]
             [stream :as ms]])
  (:import java.util.concurrent.TimeUnit))

(def psize 100) ;; default size

(defn batch
  "Batch messages from input."
  [in report]
  (let [batch (ms/batch psize in)
        out (ms/stream psize)]
    (ms/connect-via
     batch #(do (u/msg! report :found (count %)) (ms/put! out %)) out)
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

(defn create-streams
  "Create system for processing messages to ES."
  [report]
  (let [in (ms/stream psize)
        out (-> in
                (batch report)
                (>go>
                 (partial update-kw-async :source (fn [f] (f)))
                 (partial u/msg! report :dwn))
                (>go> (partial map u/process-item) (partial u/msg! report :prc))
                (>go> es/iter-es-bulk-documents (partial u/msg! report :toes))
                (>go> es/put-bulk-items! (partial u/msg! report :blk)))]
    (list in out)))

(defn load-documents!
  "Execute loading using system and docs lazy sequence."
  [docs report]
  (let [[in out] (create-streams report)
        p (promise)]
    (ms/consume identity out)
    (ms/on-closed out #(deliver p out))
    (ms/connect (ms/->source docs) in)
    p))

(defrecord ESLoading [queue report]
  component/Lifecycle
  (start [self]
    (let [s (ms/stream)]
      (ms/connect (:queue self) s)
      (ms/consume #(load-documents! % (:report self)) s)
      (assoc self :listener s)))

  (stop [self]
    (ms/close! (:listener self))
    (dissoc self :listener)))
