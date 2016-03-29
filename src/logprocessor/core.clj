(ns logprocessor.core
  (:gen-class)
  (:require [com.stuartsierra.component :as cmp]
            [logprocessor
             [processing :as prc]
             [utils :as u]]
            [manifold.stream :as ms]
            [user :as dev]))

(def steps #{:found :dwn :prc :toes :blk})

(def state
  (atom {:busy nil ;; when yes have queue here
         :report (ms/stream 1e5)
         :queue (ms/stream 1e2)
         :counters {}}))

(defn read-reports
  "Reading stream for reports."
  [report]
  (ms/consume
   prn report)
   ;; (fn [{key :key m :message uid :uid}]
   ;;   (prn key m uid)
   ;;   (if (steps key)
   ;;     (swap! state update-in [:counters uid key] (partial + m))
   ;;     (println m)))
   report)

(defn add-task
  "Add task to calculation queue, return uid."
  [docs & {:keys [description]}]
  (let [uid (subs (u/uuid) 0 7)
        {q :queue} @state]
    (swap! state assoc-in [:counters uid] (zipmap steps (repeat 0)))
    (ms/put! q {:uid uid :docs docs})
    uid))

(defn main
  ""
  [])


(def docs (dev/walk-over-file "examples.zip"))
;; (ms/close! ccc)
;; state
;; (read-reports (:report @state))
;; (def strs (prc/create-streams (:report @state) "asdsad"))
;; (ms/consume identity (second strs))
;; (ms/connect (ms/->source (take 10 docs)) (first strs))

;; system
