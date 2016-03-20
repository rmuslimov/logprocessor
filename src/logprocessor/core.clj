(ns logprocessor.core
  (:gen-class)
  (:require [com.climate.claypoole :as cp]
            [logprocessor.utils :as p]
            [logprocessor.es :as es]
            [manifold.deferred :as d]
            [manifold.stream :as ms]
            [user :as dev])
  (:import java.util.concurrent.TimeUnit))

(def psize 100) ;; default size
(def state (atom (zipmap #{:found :dwn :prc :toes :blk} (repeat 0))))

(defn inc-rep
  "Inform state about made changes"
  [kw c]
  (swap! state update kw (partial + c)))

(defn batch
  "Batch messages from input."
  [in & {:keys [size] :or {size psize}}]
  (let [batch (ms/batch size in)
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

(defn create-system
  "Create system for processing messages to ES."
  []
  (let [in (ms/stream psize)
        netpool (cp/threadpool psize)
        out (-> in
                batch
                (>go>
                 (partial cp/pmap netpool #(update % :source (fn [f] (f))))
                 (partial inc-rep :dwn))
                (>go> (partial map p/process-item) (partial inc-rep :prc))
                (>go> es/iter-es-bulk-documents (partial inc-rep :toes))
                (>go> es/put-bulk-items! (partial inc-rep :blk)))]
    (ms/on-closed out #(cp/shutdown netpool))
    (list in out)))

(defn exec!
  "Execute loading using system and docs lazy sequence."
  [system docs]
  (if (ms/closed? (first system))
    (throw (Exception. "System is closed.")))

  (let [[in out] system
        p (promise)]
    (ms/consume identity out)
    (ms/on-closed out #(deliver p out))
    (d/chain
     (ms/put-all! in docs)
     (fn [_] (ms/close! in)))
    p))

;; (def docs (dev/walk-over-file "examples.zip"))
;; (time (do @(exec! (create-system) docs) state))
