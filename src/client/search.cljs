(ns client.search
  (:require [re-com.core
             :refer
             [box
              flex-child-style
              gap
              h-box
              input-text
              label
              line
              md-icon-button
              title
              v-box
              v-split
              h-split]]
            [goog.dom :as dom]
            [reagent.core :as reagent]))

(def screen-height (.-height (dom/getViewportSize (dom/getWindow))))

(def rounded-panel
  (merge (flex-child-style "1")
         {:background-color "#fff4f4"
          :border           "1px solid lightgray"
          :border-radius    "4px"
          :padding          "0px 20px 0px 20px"}))

(defn splitter-panel-title
  [text]
  [title
   :label text
   :level :level3
   :style {:margin-top "20px"}])

(defn panel
  []
  [box
   :size "auto"
   :child [:div {:style rounded-panel}
           [splitter-panel-title [:code ":panel"]]]])

(defn search-page []
  (let [text-val (reagent/atom "")]
    (fn []
      [v-box
       :gap "10px"
       :padding "10px"
       :height (str screen-height "px")
       :children
       [[v-box
         :gap "10px"
         :size "none"
         :children
         [[label :label "Search over S3 logs for request-response xml..."]
          [line :size "2px" :color "gray"]
          [h-box
           :children
           [[box
             :size "1"
             :child [input-text
                     :model @text-val
                     :width "100%"
                     :on-change #(reset! text-val %)]]
            [gap :size "10px"]
            [box
             :align :end
             :child [md-icon-button
                     :emphasise? true
                     :md-icon-name "zmdi-search"
                     :size :larger
                     :on-click #()]]]]]]
        [gap :size "10px"]
        [h-split
         :panel-1 [panel]
         :panel-2 [panel]
         :size "1" :initial-split "25%"]]
        ])))
