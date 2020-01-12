(ns time-align-mobile.components.list-items
  (:require [time-align-mobile.js-imports :refer [view
                                                  text-paper
                                                  list-item
                                                  list-icon
                                                  touchable-highlight]]
            [time-align-mobile.styles :refer [theme]]
            [reagent.core :as r]
            ["react" :as react]))


(defn bucket [{:keys [id color label periods templates on-press]}]
  [list-item {:title    (r/as-element
                         (if (empty? label)
                           [text-paper {:style
                                        {:color      (-> theme :colors :disabled)
                                         :font-style "italic"}}
                            "Group has no label"]
                           [text-paper label]))
              :key      (str id)
              :on-press on-press
              :left     #(r/as-element
                          [list-icon {:icon  "google-circles-communities"
                                      :color color}])}])

(defn bucket-key-extractor [x]
  (-> x
      (js->clj)
      (get "id")
      (str)))

(defn pattern [{:keys [id label templates on-press]}]
  [list-item {:title    (r/as-element
                         (if (empty? label)
                           [text-paper {:style
                                        {:color      (-> theme :colors :disabled)
                                         :font-style "italic"}}
                            "Template has no label"]
                           [text-paper label]))
              :key      (str id)
              :on-press on-press
              :left     #(r/as-element
                          [list-icon {:icon "floor-plan"}])}])

(defn template [{:keys [id color label pattern-label bucket-label on-press]}]
  [touchable-highlight
   {:key      id
    :on-press on-press}

   [view {:style {:flex-direction "row"}}

    ;; symbol
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

     ;; label
     [text-paper (if (> (count label) 0)
             label
             "No label")]

     ;; bucket label
     [text-paper {:style {:color "grey"}}
      (if (> (count bucket-label) 0)
        (str "bucket-label: " bucket-label)
        "No bucket label")]

     [text-paper {:style {:color "grey"}}
      (if (> (count pattern-label) 0)
        (str "pattern-label: " pattern-label)
        "No pattern label")]
     ;; id
     [text-paper {:style {:color "grey"}}
      (str "id: " id)]]]])

;; TODO move periods and filters
