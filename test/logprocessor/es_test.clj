(ns logprocessor.es-test
  (:require [clj-time.core :as t]
            [logprocessor
             [core :as core]
             [es :as es]
             [utils :as utils]]))

;; (fact :slow "Test adding row to_daeparture index"
;; @(put-item! "l5" "request" ($> "rsp-error" process-file)) => 1)
;; @(es/put-item! "l5" "request" (pt/$> "rsp-error" parsers/process-file))
;; (def item ($> "rsp-error" process-file))
;; (json/write-str item)

;; (fact :slow :integrational "add bulk"
;;   (dev/walk-over-file "examples.zip") => 1)
;; (update (dev/walk-over-file "examples.zip") :source)

(defn load-example [items-seq]
  (let [items (map core/process-item items-seq)]
    (for [item (remove #(contains? % :exception) items)]
      (es/put-item! "l5" "request" item)
      )))

;; (load-example (dev/walk-over-file "examples.zip"))
;; (count (load-example (utils/walk-over-s3 :bcd1 :fokker (t/date-time 2016 2 4))))
;; (utils/walk-over-s3 :bcd1 :fokker (t/date-time 2016 2 2))
;; (def vvv (utils/walk-over-s3 :bcd1 :fokker (t/date-time 2016 2 1)))
;; (future-done? (:source (last vvv)))

;; (def exf (future (load-example)))
;; (future-done? exf)
;; (-> @exf last realized?)
;; (count (filter realized? @exf))
