(ns user
  (:require [clojure.java.io :as io]
            [clojure.tools.namespace.repl :refer [refresh]]
            [com.stuartsierra.component :as component]
            [logprocessor.es :as es])
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
          result {:source (->> entry (.getInputStream zipfile) slurp)
                  :name (.getName entry)}]
      (cons result
            (walk-over-file zipfile (rest entries)))))))

(defn start []
  (swap!
   app-state
   update :es
   (fn [_] (component/start (es/->ES "http://localhost:9200/")))))


(defn reset
  "Reset whole app"
  []
  (swap! app-state component/stop)
  (refresh :after 'user/start))

;; (reset)

;; (map :name (take 3 (walk-over-file "examples.zip")))
