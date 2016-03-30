(ns logprocessor.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [com.stuartsierra.component :as cmp]
            [compojure
             [core :refer [ANY defroutes]]
             [route :as route]]
            [liberator.core :refer [defresource]]
            [logprocessor
             [processing :as p]
             [utils :as u]]
            [ring.middleware.params :refer [wrap-params]]
            [system.components.http-kit :refer [new-web-server]]))

(def allowed-levels #{"bcd1" "bcd2" "stage" "dev" "release"})
(def allowed-apps #{"fokker" "cessna"})

(defn add-task
  "Add task."
  [{{sp :params} :request}]
  (let [params (u/kws-map keyword sp)]
    (println params)))

(defresource tasks
  :available-media-types ["application/json"]
  :allowed-methods [:get :put]
  :malformed?
  (fn [{{{:strs [level app y m & d] :as all} :params m :method} :request}]
    (if (= m "PUT")
      (cond
        (->> (list level app y m) (filter nil?) empty? not)
        {:message "Keys are wrong or incorrect.\n"}
        (not (contains? allowed-levels level))
        {:message (format "Wrong level: %s.\n" level)}
        (not (contains? allowed-apps app))
        {:message (format "Wrong app: %s.\n" app)}
        :else false)
      false))
  :handle-ok (fn [v] (json/write-str @p/state))
  :put!
  (fn [{{{:strs [level app y m & d] :as all} :params} :request}]
    {:message
     (p/process
      (u/walk-over-s3
       (keyword level) (keyword app)
       (Integer. y) (Integer. m) (when d (Integer. d))) p/msg!)}))


(defroutes app
  (ANY "/" [] tasks)
  (route/resources "/")
  (route/not-found "<h2>Page not found.</h2>"))


(defn main-system []
  (cmp/system-map
   :web (new-web-server 7800 (-> app wrap-params))))

(defn -main
  ""
  []
  (alter-var-root #'main-system cmp/start)
  (println "Started."))

;; (set/difference (set (keys {:k 1})) #{2})
