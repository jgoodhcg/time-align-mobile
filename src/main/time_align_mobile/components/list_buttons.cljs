(ns time-align-mobile.components.list-buttons
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  portal
                                                  fab
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]))

(defn root [add-fn]
  [fab {:icon     "plus"
        :style    {:position "absolute"
                   :margin   16
                   :bottom   0
                   :right    0}
        :on-press add-fn}])
