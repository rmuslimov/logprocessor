(ns logprocessor.core
  (:gen-class)
  (:require [clj-time.core :as t]
            [clj-xpath.core :as xp]
            [clojure.java.io :as io]
            [logprocessor
             [parsers :as parsers]
             [utils :as utils]]))

(defn walk-over-local-files
  "Local test only local files interator"
  [store]
  (let [store (-> store io/resource io/file)
        files (->> store file-seq rest)]
    (map #(-> % slurp xp/xml->doc) files)))

;; (def value
;;   (future
;;     (map
;;      parsers/process-item
;;      (take 2 (utils/walk-over-s3 :bcd1 :fokker (t/date-time 2016 2 2))))))

(defn -main
  "Do nothing for now."
  [& args])
