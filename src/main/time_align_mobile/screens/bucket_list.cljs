(ns time-align-mobile.screens.bucket-list
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  flat-list
                                                  touchable-highlight]]
            [time-align-mobile.components.filter-picker :refer [filter-picker
                                                                filter-sort]]
            [time-align-mobile.components.list-buttons :as list-buttons]
            [time-align-mobile.components.list-items :as list-items]
            [reagent.core :as r :refer [atom]]
            ["react" :as react]
            ["react-native-elements" :as rne]
            [re-frame.core :refer [subscribe dispatch]]))

(defn root [params]
  (let [buckets       (subscribe [:get-buckets])
        active-filter (subscribe [:get-active-filter])]

    [view {:style {:flex 1 :justify-content "center" :align-items "center"}}

     [text "Buckets"]
     [filter-picker :bucket]
     [flat-list {:data (filter-sort @buckets @active-filter)
                 :key-extractor list-items/bucket-key-extractor
                 :render-item
                 (fn [i]
                   (let [item (:item (js->clj i :keywordize-keys true))]
                     (r/as-element
                      (list-items/bucket
                       (merge
                        item
                        {:on-press
                         #(dispatch
                           [:navigate-to
                            {:current-screen :bucket
                             :params         {:bucket-id (:id item)}}])})))))}]
     [list-buttons/root #(dispatch [:add-new-bucket {:id (random-uuid)
                                                      :now (new js/Date)}])]]))
