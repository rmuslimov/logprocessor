(ns client.ui
  (:require [client.db :refer [columns state update-query]]
            [clojure.string :as str]
            [goog.dom :as dom]
            [re-com.core
             :refer
             [box
              gap
              h-box
              h-split
              input-text
              label
              line
              md-icon-button
              scroller
              v-box]]))

(declare on-row-view)

(def screen-height (.-height (dom/getViewportSize (dom/getWindow))))

(defn data-row
  "Render a row in table "
  [row]
  [h-box
   :class "rc-div-table-row"
   :children
   (map
    (fn [{:keys [field width link on-click]} v]
      (let [value (if (string? field) field (field row))]
        [box
         :size (str width)
         :child
         (cond
           link [:div [:a {:href (str "?q=" (name field) ":" value)} value]]
           on-click [:div [:a {:on-click (partial on-row-view (:id row)) :href "#"} field]]
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

(defn panel
  []
  (let [{r :rows active :active} @state
        {value :raw} (first (filter #(= (:id %) active) r))]
    [box :size "1" :child [:span {:style {:font-size 11}} value]]))

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
       [[label :label "Waiting for response"]]]
      [h-split
       :margin "0px"
       :panel-1 [data-table (:rows @state)]
       :panel-2 [panel] :size "1" :initial-split "70%"])]])

(defn on-row-view
  "Return ."
  [row-id evt]
  (swap! state assoc :active row-id))
