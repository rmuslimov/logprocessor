(ns client.web
  (:require [client.ui :refer [search-page]]
            [reagent.core :as reagent]))

(enable-console-print!)

(reagent/render-component
 [search-page] (. js/document (getElementById "app")))
