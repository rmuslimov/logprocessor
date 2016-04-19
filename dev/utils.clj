(ns utils
  (:require [clojure.string :as s]
            [fs.core :as fs]
            [logprocessor
             [processing :as p]
             [utils :as u]]
            [manifold.deferred :as d]))

(def psize 100)
(def cpath (fs/expand-home "~/projects/logs/amadeus"))

(defn load-all-xmls
  ""
  [level app y m & [d]]
  (let [data (->>
              (u/walk-over-s3 level app y m d)
              (partition-all psize)
              (map p/load-items)
              (mapcat identity))]
    (doseq [{:keys [source name]} data]
      (let [fname (format "%s/%s" cpath name)
            subfolder (s/join "/" (butlast (s/split fname #"/")))]
        (fs/mkdirs subfolder)
        (spit fname source)))
    (count data)))

;; (load-all-xmls :stage :cessna 2016 4 15)
;; (def ex-name "stage/fokker/y=2016/m=02/d=03/soap-request-12P0HCtP2fz8Gj13F8wlQd.xml")
;; (def v (u/walk-over-s3 :stage :fokker 2016 2 3))
;; (partition-all psize v)
;; (load-all-xmls :stage :fokker 2016 2 3)
