(ns user
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.repl :refer [refresh]]
            [com.stuartsierra.component :as component]
            [org.httpkit.client :as http]
            [fs.core :as fs]
            [clojure.string :as string]
            [logprocessor.core :as core])
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
          result {:source (fn [] (do (->> entry (.getInputStream zipfile) slurp)))
                  :name (.getName entry)}]
      (if (empty? (rest entries))
        (list result)
        (cons result
              (walk-over-file zipfile (rest entries))))))))

(defn save-all-xmls
  [items]
  (for [item (mapcat identity (core/process items))]
    (let [maindir (.getPath (fs/expand-home "~/projects/logs/amadeus"))
          filepath (format "%s/%s" maindir (:name item))
          subfolder (string/join "/" (butlast (string/split filepath #"/")))]
      (fs/mkdirs subfolder)
      (spit filepath (:source item))
      filepath)))

(defn reset
  "Reset whole app"
  []
  (refresh))
