(ns time-align-mobile.components.list-items
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  touchable-highlight]]
            ["react" :as react]))


(defn bucket [{:keys [id color label periods templates on-press]}]
  [touchable-highlight
   {:key      id
    :on-press on-press}

   [view {:style {:flex-direction "row"}}
    [view
     {:style {:width            50
              :height           50
              :margin-right     20
              :background-color color}}]
    [view {:style {:flex-direction "column"}}
     [text (if (> (count label) 0) label "No label")]
     [text {:style {:color "grey"}} "periods: " (count periods)]
     [text {:style {:color "grey"}} "templates: " (count templates)]
     [text {:style {:color "grey"}} (str "id: " id)]]]])

(defn template [{:keys [id color label bucket-label on-press]}]
  [touchable-highlight
   {:key      id
    :on-press on-press}

   [view {:style {:flex-direction "row"}}
    [view
     {:style {:width            0
              :height           0
              :margin-right     20
              :background-color "transparent"
              :border-style "solid"
              :border-left-width 25
              :border-right-width 25
              :border-bottom-width 50
              :border-left-color "transparent"
              :border-right-color "transparent"
              :border-bottom-color color}}]
    [view {:style {:flex-direction "column"}}
     [text (if (> (count label) 0)
             label
             "No label")]
     [text {:style {:color "grey"}}
      (if (> (count bucket-label) 0)
        (str "bucket-label: " bucket-label)
        "No bucket label")]
     [text {:style {:color "grey"}}
      (str "id: " id)]]]])

;; TODO move periods and filters
