(ns logprocessor.core
  (:gen-class)
  (:require [com.stuartsierra.component :as cmp]
            [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [liberator.core :refer [defresource]]
            [system.components.http-kit :refer [new-web-server]]))

(defresource example
  :available-media-types ["text/html"]
  :handle-ok "<html><h2>Hello, Internet<!>!</h2></html>")

(defroutes app
  (GET "/" [] example)
  (route/resources "/")
  (route/not-found "<h2>Page not found.</h2>"))


(defn main-system []
  (cmp/system-map
   :web (new-web-server 7800 app)))

(defn -main
  ""
  []
  (alter-var-root #'main-system cmp/start)
  (println "Started."))
