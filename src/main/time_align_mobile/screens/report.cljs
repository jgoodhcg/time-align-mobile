(ns time-align-mobile.screens.report
  (:require [time-align-mobile.js-imports :refer [view
                                                  line-chart
                                                  text]]
            ["react" :as react]))

(defn root [params]
  [view {:style {:flex 1 :justify-content "center" :align-items "center"}}
   [line-chart
    {:data
     (clj->js {:labels   (clj->js ["A" "B" "C"])
               :datasets (clj->js
                          [(clj->js {:data (clj->js [2 5 7])})])})
     :bezier        true
     :width         400
     :height        400
     :y-axis-label  "$"
     :y-axis-suffix "k"
     :chart-config
     (clj->js {:backgroundColor        "#e26a00"
               :backgroundGradientFrom "#fb8c00"
               :backgroundGradientTo   "#ffa726"
               :labelColor
               (clj->js
                #(str "rgba(255, 255, 255, "
                      (if-some [opacity %] opacity 1)
                      ")"))
               :color
               (clj->js
                #(str "rgba(255, 255, 255, "
                      (if-some [opacity %] opacity 1)
                      ")"))})}]])
