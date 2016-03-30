(ns client.search
  (:require
   [re-com.core :refer
    [h-box v-box box gap input-text flex-child-style
     line button label v-split md-icon-button title]]
   [reagent.core :as reagent :refer [atom]]))

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
       :size "auto"
       :gap "10px"
       :padding "10px"
       :children
       [[v-box
         :size "auto"
         :gap "10px"
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
        [v-box
         :size "auto"
         :margin "0px"
         :children
         [[label :label "Results..."]
          [v-split
           :panel-1 [panel]
           :panel-2 [panel]
           :size "400px" :initial-split "25%"
           ]]]
        ]])))
