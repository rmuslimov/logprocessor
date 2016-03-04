(ns user
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.repl :refer [refresh]]
            [com.stuartsierra.component :as component]
            [org.httpkit.client :as http])
  (:import java.util.zip.ZipFile))

(def app-state (atom {:es nil}))

(defn walk-over-file
  "Iter over files in zip"
  ([filename]
   (let
       [zipfile (-> filename io/resource io/file ZipFile.)
        entries
        (remove #(or (.isDirectory %) (-> % .getName (.endsWith "~")))
        (enumeration-seq (.entries zipfile)))]
     (walk-over-file zipfile entries)))
  ([zipfile entries]
   (lazy-seq
    (let [entry (first entries)
          result {:source (fn [] (->> entry (.getInputStream zipfile) slurp))
                  :name (.getName entry)}]
      (if (empty? (rest entries))
        (list result)
        (cons result
              (walk-over-file zipfile (rest entries))))))))

(defn reset
  "Reset whole app"
  []
  (refresh))
