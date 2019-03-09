(ns time-align-mobile.components.list-buttons
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]))

(defn root [add-fn]
  [view {:style {:flex            1
                 :padding         25
                 :flex-direction  "row"
                 :align-items     "center"
                 :justify-content "center"}}

   [:> rne/Button {:title        "add"
                   :on-press     add-fn}]])
