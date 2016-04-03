(ns client.db
  (:require [cljs-http.client :as http]
            [cljs-time.format :as f]
            [cljs.core.async :as a]
            [reagent.core :as reagent])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

(def es-url "http://lf:9200//titan-2015.11/_search")
(defn es-qsq [query]
  {:query {:query_string {:query query}} :size 20})


(def columns
  [{:field :timestamp :name "UTC" :width 1}
   {:field :service :name "Method" :width 2}
   {:field :session-id :name "Session ID" :width 3}
   {:field :id :name "Message ID" :width 3}
   {:field :pcc :name "PNR" :width 1}])

(def state
  (reagent/atom
   {:query "timestamp:[2015-11-12T19:50:00 TO 2015-11-12T19:55:00]"
    :rows []
    :status :ready   ;; or waiting
    }))

(def cfmt (f/formatter "yyyy-MM-ddTHH:mm:ss"))
(def rfmt (f/formatter "d yy, HH:mm:ss"))

(defn process-es-response
  ""
  [rsp]
  (let [{{{hits :hits} :hits} :body} rsp]
    (for [h hits]
      (let [{{:keys [session-id message-id service timestamp pcc]} :_source} h]
        {:id message-id
         :session-id session-id
         :service service
         :timestamp (->> timestamp (f/parse cfmt) (f/unparse rfmt))
         :pcc pcc}))))

(defn run-search
  ""
  []
  (go
    (let [{query :query} @state
          rsp (a/<! (http/post es-url {:json-params (es-qsq query)}))
          rows (process-es-response rsp)]
      (swap! state assoc :status :ready)
      (swap! state assoc :rows rows))))

(add-watch
 state :dispatch
 (fn [key atom old new]
   (when (and (= (:status new) :waiting) (= (:status old) :ready))
     (run-search))))
