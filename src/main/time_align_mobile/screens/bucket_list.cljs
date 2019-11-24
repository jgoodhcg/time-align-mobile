(ns time-align-mobile.screens.bucket-list
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  subheading
                                                  icon-button
                                                  scroll-view
                                                  list-section
                                                  flat-list
                                                  touchable-highlight]]
            [time-align-mobile.components.filter-picker :refer [filter-picker
                                                                filter-sort]]
            [time-align-mobile.components.list-buttons :as list-buttons]
            [time-align-mobile.components.list-items :as list-items]
            [time-align-mobile.components.top-bar :refer [top-bar]]
            [reagent.core :as r :refer [atom]]
            ["react" :as react]
            ["react-native-elements" :as rne]
            [re-frame.core :refer [subscribe dispatch]]))

(defn root [params]
  (let [buckets       (subscribe [:get-buckets])
        active-filter (subscribe [:get-active-filter])]

    [view {:style {:flex 1}}
     [top-bar {:center-content [subheading "Groups"]
               :right-content  [icon-button]}]
     [filter-picker :bucket]
     [scroll-view
      [list-section
       (->> (filter-sort @buckets @active-filter)
            (map (fn [item]
                   (list-items/bucket
                    (merge
                     item
                     {:on-press
                      #(dispatch
                        [:navigate-to
                         {:current-screen :bucket
                          :params         {:bucket-id (:id item)}}])})))))]]

     [list-buttons/root #(dispatch [:add-new-bucket {:id   (random-uuid)
                                                     :now (new js/Date)}])]]))
