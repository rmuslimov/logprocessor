(ns logprocessor.core
  (:gen-class)
  (:require [manifold.stream :as ms]))

(def state
  (atom {:busy false
         :queue (ms/stream 10)
         :counter (zipmap #{:found :dwn :prc :toes :blk} (repeat 0))}))

;; (def docs (dev/walk-over-file "examples.zip"))
;; (time (do @(exec! (create-system) docs) state))
