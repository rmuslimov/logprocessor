(ns logprocessor.core
  (:gen-class)
  (:require [com.stuartsierra.component :as cmp]
            [logprocessor.processing :as prc]
            [logprocessor.utils :as u]
            [user :as dev]
            [manifold.stream :as ms]))

(def state
  (atom {:busy false
         :queue (ms/stream 10)
         :report (ms/stream 1e5)
         :counter (zipmap #{:found :dwn :prc :toes :blk} (repeat 0))}))

(defn read-reports
  [report]
  (ms/consume
   #(let [{key :key m :message} %]
      (if (contains? (@state :counter) key)
        (swap! state update-in [:counter key] (partial + m))
        (println m)))
   report))

(defn create-system
  ""
  []
  (cmp/system-map
   :loader (prc/map->ESLoading (select-keys @state [:queue :report]))))

(defn main
  ""
  []

  )

;; (swap! state update :found inc)
;; @(es/create-indices y m :report report)

;; (read-reports (:report @state))
;; (def system (create-system))
;; (alter-var-root #'system cmp/start)
;; (alter-var-root #'system cmp/stop)
;; system
;; (ms/put! (:queue @state) (dev/walk-over-file "examples.zip"))
;; (ms/put! (:queue @state) (u/walk-over-s3 :bcd1 :fokker 2016 2 1))
;; (prc/load-documents! (dev/walk-over-file "examples.zip") (:report @state))
