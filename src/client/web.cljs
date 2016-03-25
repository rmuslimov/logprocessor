(ns client.web
  (:require [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

(println "Privet Nikita!")

(defonce app-state (atom {:text "Hello world!"}))

(defn hello-world []
  [:h1 (:text @app-state)])

(reagent/render-component [hello-world]
                          (. js/document (getElementById "app")))

;; change state example (do C-x C-e here, see browser)
;; (swap! app-state assoc :text "Hello world!!!")
