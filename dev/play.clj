(ns play
  (:require [manifold
             [deferred :as d]
             [stream :as ms]]))

;; (def a (ms/stream 10))
;; (def err (ms/stream 1e3))
;; (defn msg! [s kw m] (ms/put! s (list kw m)))

;; (defn divider
;;   ""
;;   [x]
;;   (Thread/sleep 1000) x)

;; (def out (||process a 5 divider (partial msg! err)))
;; (ms/consume println out)
;; (ms/close! a)
;; (doall (map (partial ms/put! a) (list :a :b :c :d :e :r)))
;; (ms/put! a 4)
