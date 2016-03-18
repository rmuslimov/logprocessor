(ns logprocessor.core
  (:gen-class)
  (:require [com.climate.claypoole :as cp]
            [logprocessor.utils :as p]
            [logprocessor.es :as es]
            [manifold.stream :as ms]
            [user :as dev])
  (:import java.util.concurrent.TimeUnit))

(def psize 100) ;; default size
(def net-pool (cp/threadpool psize))
(def state (atom (zipmap #{:found :dwn :prc :toes :blk} (repeat 0))))

(defn inc-rep
  "Inform state about made changes"
  [kw c]
  (swap! state update kw (partial + c)))

(defn do-&-rep
  "Process items and report about messages were done."
  [process-fn kw]
  (fn [items]
    (inc-rep kw (count items))
    (process-fn items)))

(defn batch
  "Batch messages from input."
  [in & {:keys [size] :or {size psize}}]
  (let [batch (ms/batch size in)
        out (ms/stream psize)]
    (ms/connect-via batch #(do (inc-rep :found (count %)) (ms/put! out %)) out)
    out))

(defn >go>
  "Reads an input with raw items and parses to our map."
  [in process-fn]
  (let [out (ms/stream psize)
        cpool (cp/threadpool (+ (cp/ncpus) 2))]
    (future
      (loop [items @(ms/take! in)]
        (if items
          (do
            (cp/future cpool (ms/put! out (process-fn items)))
            (recur @(ms/take! in)))
          (do
            (cp/shutdown cpool)
            (.awaitTermination cpool 1 TimeUnit/MINUTES)
            (ms/close! out))
          )))
    out))

(defn download
  [in]
  (>go> in
        (do-&-rep
         (partial cp/pmap net-pool #(update % :source (fn [f] (f)))) :dwn)))

(defn process
  [in]
  (>go>
   in
   (do-&-rep (partial map p/process-item) :prc)))

(defn to-es
  [in]
  (>go>
   in
   (do-&-rep es/iter-es-bulk-documents :toes)))

(defn bulk
  [in]
  (>go>
   in
   (do-&-rep es/put-bulk-items! :blk)))

(defn create-system
  "Create system for processing messages to ES."
  []
  (let [in (ms/stream psize)]
    (list in (-> in batch download process to-es bulk))))

(defn exec!
  ""
  [system docs]
  (if (ms/closed? (first system))
    (throw (Exception. "System is closed.")))

  (let [[in out] system
        p (promise)]
    (ms/connect (ms/->source docs) in)
    (ms/consume identity out)
    (ms/on-closed out #(deliver p out))
    p))

(def docs (dev/walk-over-file "examples.zip"))

;; state
;; (def system (create-system))
;; (time @(exec! system docs))
