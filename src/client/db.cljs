(ns client.db
  (:require [cljs-http.client :as http]
            [cljs-time.format :as f]
            [cljs.core.async :as a]
            [reagent.core :as reagent]
            [secretary.core :as secretary :refer-macros [defroute]])
  (:require-macros
   [cljs.core.async.macros :refer [go]]
   [logprocessor.utils :as env :refer [cljs-env]]))

;; use js host temporarily
(def es-url
  (str "http://" (-> js/window .-location .-hostname) ":9200" "/titan-*/_search"))

(defn es-qsq [query]
  {:query
   {:query_string
    {:query query :analyze_wildcard true :default_field :raw
     :default_operator "AND"}}
   :size 120 :sort {:timestamp {:order :asc}, :service {:order :asc}}
   :_source {:exclude [:raw]}})

(def columns
  [{:field "View" :name "#" :width 1 :link false :href true}
   {:field :timestamp :name "UTC" :width 3 :link false}
   {:field :service :name "Method" :width 6 :link true}
   {:field :session-id :name "Session ID" :width 9 :link true}
   {:field :pcc :name "PCC" :width 3 :link false}])

;; @state
;; :query "timestamp:[2015-11-12T19:50:00 TO 2015-11-12T19:55:00]"
(defonce state
  (reagent/atom
   {:query "NAK3"
    :status :ready   ;; or waiting
    :total nil
    :rows []}))

(def cfmt (f/formatter "yyyy-MM-ddTHH:mm:ss"))
(def rfmt (f/formatter "d MMM yy, HH:mm:ss"))

(defn process-es-response
  ""
  [rsp]
  (let [{{{t :total hits :hits} :hits} :body} rsp]
    {:total t
     :rows
    (for [{{:keys [timestamp pcc message-id] :as source} :_source} hits]
      (merge
       source
       {:id message-id
        :timestamp (->> timestamp (f/parse cfmt) (f/unparse rfmt))}))}))

(defn run-search
  "Run ES simple query string query and update state."
  []
  (go
    (let [{q :query} @state
          rsp (a/<! (http/post es-url {:json-params (es-qsq q)}))
          {:keys [rows total]} (process-es-response rsp)]
      (swap! state assoc :status :ready)
      (swap! state assoc :total total)
      (swap! state assoc :rows rows))))

(defn update-query
  "Replace query field in db with given value and run search"
  [query]
  (swap! state assoc :query query)
  (swap! state assoc :status :waiting))

;; routing
(defroute search-page-route "/" {:as params}
  (let [{{q :q} :query-params} params]
    (when q (update-query q))))

;; watchin for state changes
(add-watch
 state :dispatch
 (fn [key atom old new]
   ;; listen status change
   (let [{oldstatus :status} old {newstatus :status} new]
     (when (and (= newstatus :waiting) (= oldstatus :ready))
       (run-search)))))
