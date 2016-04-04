(ns client.db
  (:require [cljs-http.client :as http]
            [cljs-time.format :as f]
            [cljs.core.async :as a]
            [reagent.core :as reagent]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(declare search-page-route)

(def es-url "http://lf:9200//titan-2015.11/_search")
(defn es-qsq [query]
  {:query
   {:query_string {:query query}}
   :size 20 :sort {:timestamp {:order :asc}}})

(def columns
  [{:field :timestamp :name "UTC" :width 1 :link false}
   {:field :service :name "Method" :width 2 :link true}
   {:field :session-id :name "Session ID" :width 3 :link true}
   {:field :message-id :name "Message ID" :width 3 :link false}
   {:field :pcc :name "PCC" :width 1 :link false}])

(defonce state
  (reagent/atom
   {:query "timestamp:[2015-11-12T19:50:00 TO 2015-11-12T19:55:00]"
    :status :ready   ;; or waiting
    :rows []}))

(def cfmt (f/formatter "yyyy-MM-ddTHH:mm:ss"))
(def rfmt (f/formatter "d yy, HH:mm:ss"))

(defn process-es-response
  ""
  [rsp]
  (let [{{{hits :hits} :hits} :body} rsp]
    (for [h hits]
      (let [{{:keys [session-id message-id service timestamp pcc]} :_source
             id :_id} h]
        {:id id
         :session-id session-id
         :message-id message-id
         :service service
         :timestamp (->> timestamp (f/parse cfmt) (f/unparse rfmt))
         :pcc pcc}))))

(defn run-search
  "Run ES simple query string query and update state."
  []
  (go
    (let [{query :query} @state
          rsp (a/<! (http/post es-url {:json-params (es-qsq query)}))
          rows (process-es-response rsp)]
      (swap! state assoc :status :ready)
      (swap! state assoc :rows rows))))

;; routing
(defroute search-page-route "/" {:as params}
  (let [{{q :q} :query-params} params]
    (when q
      (swap! state assoc :query q)
      (swap! state assoc :status :waiting))))

;; watching state
(add-watch
 state :dispatch
 (fn [key atom old new]
   (when (and (= (:status new) :waiting) (= (:status old) :ready))
     (run-search))))
