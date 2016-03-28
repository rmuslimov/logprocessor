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
   (fn [{key :key m :message uid :uid}]
     (if (steps key)
       (swap! state update-in [:counters uid key] (partial + m))
       (println m)))
   report))

;; (defn create-system
;;   ""
;;   []
;;   (cmp/system-map
;;    :loader (prc/map->ESLoading (select-keys @state [:queue :report]))))

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

;; (def a (ms/stream 10))
;; (ms/put! a 1)
;; (def b (ms/stream 10))
;; (def e (ms/stream 10))

;; (ms/consume prn e)

;; (ms/consume-async
;;  (fn [x] (d/catch (d/future (/ 1 x)) #(ms/put! e %)))
;;  b)
;; (ms/put! b 0)

;; (swap! state update :found inc)
;; @(es/create-indices y m :report report)


;; (ms/downstream (:queue @state))
;; (def ccc (ms/stream 10))
;; (ms/connect (:queue @state) ccc)
;; (ms/close! ccc)
;; state
;; (read-reports (:report @state))
;; (def system (create-system))
;; (alter-var-root #'system cmp/start)
;; (alter-var-root #'system cmp/stop)
;; system
;; (ms/put! (:queue @state) (dev/walk-over-file "examples.zip"))
;; (ms/put! (:queue @state) (u/walk-over-s3 :bcd1 :fokker 2016 2 1))
;; (prc/load-documents! (dev/walk-over-file "examples.zip") (:report @state) "as333")
;; (add-task (take 5 (dev/walk-over-file "examples.zip")))
