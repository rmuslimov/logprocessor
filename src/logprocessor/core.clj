(ns logprocessor.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clj-xpath.core :as xp]))

(defn walk-over-local-files
  "Local test only local files interator"
  []
  (let [store (-> "test" io/resource io/file)
        files (->> store file-seq rest)]
    (map #(-> % slurp xp/xml->doc) files)))

(defn -main
  "Do nothing for now."
  [& args])
