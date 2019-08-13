(ns time-align-mobile.screens.calendar
  (:require [time-align-mobile.js-imports :refer [view text pan-gesture-handler]]
            ["react" :as react]
            [goog.object :as obj]))

(defn root [params]
  [pan-gesture-handler {:enabled                    true
                        :should-cancel-when-outside false
                        :on-gesture-event           #(println (obj/getValueByKeys % #js["nativeEvent" "y"]))}
   [view {:style {:flex 1 :justify-content "center" :align-items "center"}}
    [text "calendar"]]])
