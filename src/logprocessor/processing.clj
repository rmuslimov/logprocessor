(ns logprocessor.processing
  (:require [com.stuartsierra.component :as cmp]
            [logprocessor
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
  (let [out (ms/stream 100)]
    (d/loop [acc []]
      (let [drop? (= (count acc) size)
            restv (if drop? [] acc)
            put-out! (fn [v] (msg! (count v)) (ms/put! out v))]
        (->
         (d/chain
          (d/zip (ms/take! in) (when drop? (put-out! acc)))
          (fn [[v _]]
            (if v
             (d/recur (conj restv v))
             (if-not (empty? restv)
               (d/chain
                (put-out! restv)
                (fn [x] (when x (ms/close! out))))))))
         (d/catch #(println "Err:" %)))))
    out))

(defn- connect
  ""
  [source tmp sink ufn msg! & [kw]]
  (d/loop [prevpush true]
    (d/chain
     (d/zip (ms/take! source) prevpush)
     (fn [[v _]]
       (when v
         (ms/put! tmp :val)
         (->
          (d/future (ufn v))
          (d/chain
           (fn [v] (do (when kw (msg! kw (count v))) v))
           (fn [v] (ms/take! tmp) v)
           (fn [v] (ms/put! sink v)))
          (d/catch #(msg! :error %))
          (d/recur))
         )))))

(defn ||process
  ""
  [in N xf msg! & [kw]]
  (let [out (ms/stream 10)
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
                (||process 10 #(doall (map u/process-item %)) msg! :prc)
                (||process 3 es/iter-es-bulk-documents msg! :toes)
                (||process 3 es/put-bulk-items! msg! :blk))]
    (ms/on-closed out #(msg! :message "Process finished"))
    (list in out)))

(def report (ms/stream))
(ms/consume #(println "rep:" %) report)
(def docs (dev/walk-over-file "examples.zip"))
(def smalldocs (take 1010 docs))
(def megadocs (mapcat identity (repeat 10 docs)))
(def broken (take 3 (dev/walk-over-file "broken.zip")))

;; (def strs (create-streams report "asdas"))
;; (ms/consume identity (second strs))
;; (ms/connect (ms/->source broken) (first strs))
;; (ms/connect (ms/->source docs) (first strs))
;; (ms/connect (ms/->source megadocs) (first strs))
;; (ms/connect (ms/->source smalldocs) (first strs))

(defn load-documents!
  "Execute loading using system and docs lazy sequence."
  [docs report uid]
  (let [[in out] (create-streams report uid)]
    ;; (ms/consume identity out)
    (ms/connect (ms/->source docs) in)
    (list in out)))
