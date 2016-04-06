(ns logprocessor.core
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.tools.nrepl [server :as nrepl-server]]
            [com.stuartsierra.component :as cmp]
            [compojure
             [core :refer [ANY defroutes GET]]
             [route :as route]]
            [environ.core :refer [env]]
            [liberator.core :refer [defresource]]
            [logprocessor
             [es :as es]
             [processing :as p]
             [utils :as u]]
            [cider.nrepl :refer (cider-nrepl-handler)]
            [ring.middleware.params :refer [wrap-params]]
            [system.components.http-kit :refer [new-web-server]]))

(def allowed-levels #{"bcd1" "bcd2" "stage" "dev" "release"})
(def allowed-apps #{"fokker"})

(defresource rawxml
  :available-media-types ["application/xml"]
  :allowed-methods [:get]
  :handle-ok
  (fn [{{{id :id} :route-params} :request}] @(es/get-item-by-id id)))

(defresource tasks
  :available-media-types ["application/json"]
  :allowed-methods [:get :put]
  :malformed?
  (fn [{{{:strs [level app year month & day]} :params
         method :request-method} :request :as all}]
    (if (not= method :get)
      (cond
        (->> (list level app year month) (filter nil?) empty? not)
        {:message "Keys are wrong or incorrect."}
        (not (contains? allowed-levels level))
        {:message (format "Wrong level: %s." level)}
        (not (contains? allowed-apps app))
        {:message (format "Wrong app: %s." app)}
        :else false)
      false))
  :handle-ok (fn [v] @p/state)
  :put!
  (fn [{{{:strs [level app year month & day] :as all} :params} :request}]
    (let [level (keyword level) app (keyword app)
          y (Integer. year) m (Integer. month) d (if day (Integer. day) nil)
          created (es/create-indices y m)]
      (when (some? created) ;; create indices first
        (apply println "Indices created:" created))

      (let [r (p/process (u/walk-over-s3 level app y m d) p/msg!)]
        {:message {:task-id r}}))))

(defroutes app
  (GET "/" [] (io/resource "public/index.html"))
  (GET "/raw/:id" [id] rawxml)
  (ANY "/tasks" [] tasks)
  (route/resources "/")
  (route/not-found "<h2>Page not found.</h2>"))

(defn main-system []
  (cmp/system-map
   :web (new-web-server 7800 (-> app wrap-params))))

(defn -main
  ""
  []
  (cmp/start (main-system))
  (println "Started.")
  (nrepl-server/start-server :bind "0.0.0.0" :port 7801 :handler cider-nrepl-handler)
  (println "Started repl."))
