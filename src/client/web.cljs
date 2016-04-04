(ns client.web
  (:require [client.ui :refer [search-page]]
            [reagent.core :as reagent]
            [secretary.core :as secretary]))

(enable-console-print!)

(reagent/render-component
 [search-page] (. js/document (getElementById "app")))

;; trivial usage, to handle on initial load only
(secretary/dispatch!
 (str
   (-> js/window .-location .-pathname)
   (-> js/window .-location .-search)))
