(ns client.ui
  (:require [goog.dom :as dom]
            [re-com.core
             :refer
             [box
              flex-child-style
              gap
              h-box
              input-text
              label
              line
              md-icon-button
              scroller
              v-box
              v-split]]
            [reagent.core :as reagent]
            [client.db :refer [state columns]]))

(def screen-height (.-height (dom/getViewportSize (dom/getWindow))))

(defn data-row
  [row]
  [h-box
   :class "rc-div-table-row"
   :children
   (map
    (fn [{:keys [field width]} v]
      [box :size (str width) :child [label :label (field row)]]) columns)])

(defn data-table
  [rows]
  [v-box
   :class "rc-div-table"
   :size "auto"
   :children
   [[h-box
     :class    "rc-div-table-header"
     :children
     (map
      (fn [{:keys [name width] :or {:width 1}} v]
        [box :size (str width) :child [label :label name]]) columns)]
    (for [row rows] ^{:key (:id row)} [data-row row])]])

(defn panel
  []
  [box :size "auto"
   :child
   [:div {:style (merge (flex-child-style "1")
                        {:background-color "#fff4f4"
                         :border           "1px solid lightgray"
                         :border-radius    "4px"
                         :padding          "0px 20px 0px 20px"})}]])

(defn search-page []
  [v-box
   :padding "10px"
   :height (str screen-height "px")
   :children
   [[v-box
     :gap "10px"
     :size "none"
     :children
     [[label :label "Search over S3 logs for request-response xml ..."]
      [line :size "2px" :color "gray"]
      [h-box
       :children
       [[box
         :size "1"
         :child [input-text
                 :model (:query @state)
                 :width "100%"
                 :on-change #(do
                               (swap! state assoc :query %)
                               (swap! state assoc :status :waiting))
                 ]]
        [gap :size "10px"]
        [box
         :align :center
         :child [md-icon-button
                 :emphasise? true
                 :md-icon-name "zmdi-search"
                 :size :larger
                 :on-click #()]]]]]]
    [gap :size "10px"]
    (if (= (:status @state) :waiting)
      [h-box :size "none" :children
       [[label :label "Waiting for response"]]]
      [v-split
       :margin "0px"
       :panel-1 [scroller :v-scroll :auto :child [data-table (:rows @state)]]
       :panel-2 [panel] :size "1" :initial-split "75%"])]])
