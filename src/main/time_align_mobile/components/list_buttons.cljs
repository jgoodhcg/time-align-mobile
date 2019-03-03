(ns time-align-mobile.components.list-buttons
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  touchable-highlight]]))

(defn root [add-fn]
  [view {:style {:flex            1
                 :padding         25
                 :flex-direction  "row"
                 :align-items     "center"
                 :justify-content "center"}}
   [touchable-highlight {:on-press add-fn
                         :style    {:background-color "#00ffff"
                                    :border-radius    2
                                    :flex-direction   "row"
                                    :justify-content  "center"
                                    :align-items      "center"}}
    [text "add"]]])
