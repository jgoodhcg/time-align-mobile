(ns time-align-mobile.screens.pattern-list
(:require [time-align-mobile.js-imports :refer [view
                                                text
                                                flat-list
                                                list-section
                                                scroll-view
                                                icon-button
                                                subheading
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
  (let [patterns      (subscribe [:get-patterns])
        active-filter (subscribe [:get-active-filter])]

    [view {:style {:flex 1}}
     [top-bar {:center-content [subheading "Plans"]
               :right-content  [icon-button]}]
     [scroll-view
      [list-section
       (->> (filter-sort @patterns @active-filter)
            (map (fn [item]
                   (r/as-element
                    (list-items/pattern
                     (merge
                      item
                      {:on-press
                       #(dispatch
                         [:navigate-to
                          {:current-screen :pattern
                           :params         {:pattern-id (:id item)}}])}))))))]]

     [list-buttons/root
      #(dispatch [:add-new-pattern {:id (random-uuid)
                                    :now (new js/Date)}])]]))


