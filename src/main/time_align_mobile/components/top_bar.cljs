(ns time-align-mobile.components.top-bar
  (:require [time-align-mobile.js-imports :refer [surface
                                                  icon-button]]
            [re-frame.core :refer [subscribe dispatch]]))

(defn top-bar [{:keys [center-content right-content]}]
  (let [menu-open (subscribe [:get-menu-open])]
    [surface {:elevation 1
              :style     {:flex-direction  "row"
                          :justify-content "space-between"
                          :align-items     "center"
                          :padding         8}}

     [icon-button {:icon     (if @menu-open "backburger" "menu")
                   :size     20
                   :on-press #(dispatch [:set-menu-open (not @menu-open)])}]

     center-content
     right-content]))
