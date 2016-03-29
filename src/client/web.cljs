(ns client.web
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "Let's rock!")

(defonce app-state (atom {:text "Hello app!"}))

(defn hello-world []
  [:div
   [:h1 (:text @app-state)]
   [:h2 "here test"]])

(reagent/render-component [hello-world]
                          (. js/document (getElementById "app")))

;; @app-state
;; change state example (do C-x C-e here, see browser)
;; (swap! app-state assoc :text "Hellow world!!!")
