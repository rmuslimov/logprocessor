(ns client.ui
  (:require [client.db :refer [columns state update-query]]
            [goog.dom :as dom]
            [re-com.core
             :refer
             [box gap h-box input-text label line md-icon-button scroller v-box]]))

(def screen-height (.-height (dom/getViewportSize (dom/getWindow))))

(defn data-row
  "Render a row in table "
  [row]
  [h-box
   :class "rc-div-table-row"
   :children
   (map
    (fn [{:keys [field width link href]} v]
      (let [value (if (string? field) field (field row))]
        [box
         :size (str width)
         :child
         (cond
           href [:div [:a {:href (str "/raw/" (:id row)) :target :_blank} field]]
           link [:div [:a {:href (str "?q=" (name field) ":" value)} value]]
           :else [label :label value])]))
    columns)])

(defn data-table
  [rows]
  [scroller :v-scroll :auto :child
   [v-box
   :class "rc-div-table"
   :size "auto"
   :children
   [[h-box
      :class "rc-div-table-header"
      :children
      (map
       (fn [{:keys [name width] :or {:width 1}} v]
         [box :size (str width) :child [label :label name]]) columns)]
     (for [row rows] ^{:key (:id row)} [data-row row])]]])

(defn search-page []
  [v-box
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
                 :model (:query @state)
                 :width "100%"
                 :on-change update-query]]
        [gap :size "10px"]
        [box
         :align :center
         :child [md-icon-button
                 :emphasise? true
                 :md-icon-name "zmdi-search"
                 :size :larger
                 :on-click update-query]]]]]]
    [gap :size "10px"]
    (if (= (:status @state) :waiting)
      [h-box :size "none" :children
       [[label :label "Waiting for response..."]]]
      [v-box :size "none" :gap "5px"
       :children
       [[label :label (str (:total @state) " Records found...")]
        [data-table (:rows @state)]]])]])
