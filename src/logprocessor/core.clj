(ns logprocessor.core
  (:gen-class)
  (:require [com.climate.claypoole :as cp]
            [logprocessor.utils :as utils]
            [manifold.bus :as mb]
            [manifold.stream :as ms]
            [user :as dev]))

(def psize 100)
(def cpool (cp/threadpool (+ 2 (cp/ncpus))))
(def net-pool (cp/threadpool psize))
(def state (atom (zipmap #{:input :found :dwn :prc :sent} (repeat 0))))

(defn consume-reports
  "Listen all messages about processing."
  [bus]
  (let [events (keys @state)]
    (doall
     (for [event events]
      (let [sub (mb/subscribe bus event)]
        (ms/consume #(swap! state update event (partial + %)) sub))))))

(defn build-streams-map
  "Build proper map of stream for processing"
  []
  (let [smap {:input (ms/stream psize)
              :found (ms/stream psize)
              :dwn (ms/stream psize)
              :prc (ms/stream psize)
              :bus (mb/event-bus)}]
    (ms/connect-via
     (:input smap)
     (fn [m] (swap! state update :found inc) (ms/put! (:found smap) m))
     (:found smap))
    (consume-reports (:bus smap))
    smap))

(defn download
  "Properly download messages from in stream and put to out."
  [in out bus]
  (let [chunked (ms/batch psize in)]
    (loop [items @(ms/take! chunked)]
      (when items
        (do
          (cp/future
            cpool
            (let [res (cp/pmap psize #(update % :source (fn [f] (f))) items)]
              (mb/publish! bus :dwn (count res))
              @(ms/put-all! out res)))
          (recur @(ms/take! chunked)))))))

(defn process
  "Reads an input with raw items and parses to our map."
  [in out bus]
  (loop [item @(ms/take! in)]
    (when item
      (do
        (ms/put! out (utils/process-item item))
        (mb/publish! bus :prc 1)
        (recur @(ms/take! in))))))

(defn close-all!
  "Switch off all streams."
  [smap]
  (map ms/close! (vals (select-keys smap #{:input :found :dwn :prc})))
  (map
   #(let [s (mb/downstream (:bus smap) %)] (when s (map ms/close! s)))
   (keys @state))
  true)

;; (def smap (build-streams-map))
;; (def downloader (future (download (:found smap) (:dwn smap) (:bus smap))))
;; (def processor (future (process (:dwn smap) (:prc smap) (:bus smap))))

;; smap state
;; (def ex (dev/walk-over-file "examples.zip"))
;; (ms/connect (ms/->source ex) (:input smap))
;; (ms/close! (:input smap))
;; (close-all! smap)
;; (for [s messages] (ms/close! s))
;; @(mb/publish! (:bus smap) :found 1)
;; (mb/active? (:bus smap) :found)
