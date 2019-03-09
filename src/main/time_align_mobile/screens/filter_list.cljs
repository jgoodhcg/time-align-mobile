(ns time-align-mobile.screens.filter-list
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  flat-list
                                                  touchable-highlight]]
            [time-align-mobile.components.filter-picker :refer [filter-picker
                                                                filter-sort]]
            [time-align-mobile.components.list-buttons :as list-buttons]
            [reagent.core :as r :refer [atom]]
            ["react" :as react]
            [re-frame.core :refer [subscribe dispatch]]))


(defn root [params]
  (let [filters       (subscribe [:get-filters])
        active-filter (subscribe [:get-active-filter])]
    [view {:style {:flex 1 :justify-content "center" :align-items "center"}}
     [text "Filters"]
     [filter-picker :filter]
     [flat-list {:data (filter-sort @filters @active-filter)
                 :key-extractor (fn [x]
                                  (-> x
                                      (js->clj)
                                      (get "id")
                                      (str)))
                 :render-item
                 (fn [i]
                   (let [item               (:item (js->clj i :keywordize-keys true))
                         {:keys [id label]} item]
                     (r/as-element [touchable-highlight
                                    {:key      id
                                     :on-press #(dispatch
                                                 [:navigate-to
                                                  {:current-screen :filter
                                                   :params         {:filter-id id}}])}

                                    [view {:style {:flex-direction "row"}}
                                     [view {:style {:width               0
                                                    :height              0
                                                    :margin-right        20
                                                    :background-color    "transparent"
                                                    :border-style        "solid"
                                                    :border-left-width   25
                                                    :border-right-width  25
                                                    :border-bottom-width 50
                                                    :border-left-color   "transparent"
                                                    :border-right-color  "transparent"
                                                    :border-bottom-color "grey"
                                                    :transform           [{:rotate "180deg"}]}}]
                                     [view {:style {:flex-direction "column"}}
                                      [text (if (> (count label) 0)
                                              label
                                              "No label")]
                                      [text {:style {:color "grey"}}
                                       (str "id: " id)]]]])))}]
     [list-buttons/root #(dispatch [:add-new-filter {:id (random-uuid)
                                                     :now (js/Date.)}])]]))
