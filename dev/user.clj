(ns user
  (:require [clojure.java.io :as io]
            [midje.sweet :refer [facts]])
  (:import java.util.zip.ZipFile))

(def example-zip-file "examples.zip")

(defn walk-over-file
  "Iter over files in zip"
  ([filename]
   (let
       [zipfile (-> example-zip-file io/resource io/file ZipFile.)
        entries
        (remove #(or (.isDirectory %) (-> % .getName (.endsWith "~")))
        (enumeration-seq (.entries zipfile)))]
     (walk-over-file zipfile entries)))
  ([zipfile entries]
   (lazy-seq
    (let [entry (first entries)
          result {:source (->> entry (.getInputStream zipfile) slurp)
                  :name (.getName entry)}]
      (cons result
            (walk-over-file zipfile (rest entries)))))))

;; (take 3 (walk-over-file example-zip-file))
