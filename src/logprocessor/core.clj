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

(defn -main
  "Do nothing for now."
  [& args])
