(ns client.web
  (:require [client.search :refer [search-page]]
            [reagent.core :as reagent]))

(enable-console-print!)

(reagent/render-component
 [search-page]
 (. js/document (getElementById "app")))

;; @app-state
;; change state example (do C-x C-e here, see browser)
;; (swap! app-state assoc :text "Hellow world!!!")
