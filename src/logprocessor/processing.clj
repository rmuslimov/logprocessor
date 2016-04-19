(ns logprocessor.processing
  (:require [clj-time.core :as t]
            [com.climate.claypoole :as cp]
            [logprocessor
             [es :as es]
             [utils :as u]]
            [manifold
             [deferred :as d]
             [stream :as ms]]))

(def psize 100)             ;; default size
(def rp (ms/stream 1e3))    ;; reporting stream

;; (ms/consume println rp)

(def state (atom {:busy nil :tasks {}}))

(defn msg!
  "Send message to reporting stream."
  [uid kw & [body]]
  (ms/put! rp (merge {:uid uid :kw kw} (or body {}))))


(defn state-consume
  [{uid :uid kw :kw, :as v}]
  (case kw
    :started
    (do
      (swap! state assoc :busy uid)
      (swap! state assoc-in [:tasks uid]
             {:size (:size v) :processed 0})
      (if-let [d (:description v)]
        (swap! state assoc-in [:tasks uid :description] d))
      (println (format "Started task (%s): %s." (:description v) uid)))
    :chunked
    (do
      (swap! state update-in [:tasks uid :processed] (partial + (:msg v))))
    :finished
    (do
      ;; (swap! state assoc-in [:tasks uid :timef] (:time v))
      (swap! state assoc :busy nil)
      (println (format "Finished task: %s." uid)))
    (prn v)))

;; Catch events coming from processing
(ms/consume #(u/wrap-exc (state-consume %)) rp)

(defn- update-kw-async [kw fn pool]
  "Downloading xmls in async mode."
  @(apply d/zip (map #(d/future (update % kw fn)) pool)))

(defn load-items
  [pool]
  (update-kw-async :source (fn [x] (x)) pool))

(defn process-chunk
  [pool msg!]
  (-> pool
      load-items
      (u/process-items msg!)
      (#(remove :exception %))
      (es/iter-es-bulk-documents msg!)
      (es/put-bulk-items! msg!)))

(defn process
  "Process docs gotten from somewhere and paste to ES."
  [pool msg! & {:keys [description]}]
  (let [uid (subs (u/uuid) 0 7)
        msg! (partial msg! uid)
        fp (promise)]
    (future
      (cp/with-shutdown! [cpool (cp/threadpool (+ (cp/ncpus) 2))]
        (msg! :started
              {:size (count pool) :time (t/now)
               :description description :promise fp})
        (let [futures
              (for [chunk (partition-all psize pool)]
                (let [f (cp/future
                          cpool
                          (process-chunk chunk msg!))]
                  (d/chain
                   f #(msg! :chunked {:msg (count %)}))))]

          ;; wait until calculation finishes
          (doall @(apply d/zip futures)))
        (deliver fp true)
        (msg! :finished {:time (t/now)})))
    uid))

;; (def docs (u/walk-over-file "examples.zip"))
;; (def b1 (u/walk-over-file "broken.zip"))
;; (def real (u/walk-over-s3 :bcd1 :fokker 2016 2 1))
;; (process (u/walk-over-file "retrieve.zip") msg!)
;; (process b1 msg! :description "broken")
;; (process (take 3 docs) msg!)
;; @state
