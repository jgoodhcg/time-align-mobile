(ns time-align-mobile.screens.report
  (:require [time-align-mobile.js-imports :refer [view
                                                  line-chart
                                                  bar-chart
                                                  pie-chart
                                                  surface
                                                  color-lighten
                                                  color-hex-str->rgba
                                                  color-darken
                                                  subheading
                                                  icon-button
                                                  text]]
            [time-align-mobile.components.top-bar :refer [top-bar]]
            [time-align-mobile.styles :refer [theme]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            ["react" :as react]))

(defn root [params]
  (let [pie-chart-data @(subscribe [:get-cumulative-h-by-bucket])]
    [view {:style {:flex 1}}
     [top-bar {:center-content [subheading "Reports"]
               :right-content  [icon-button]}]
     [view {:style {:flex        1
                    :align-items "center"}}

      [surface
       [pie-chart
        {:data                    (clj->js pie-chart-data)
         :vertical-label-rotation 90
         :width                   400
         :height                  400
         :acessor                 "population"
         :background              "transparent"
         :from-zero               true
         :chart-config
         (clj->js {:backgroundColor        (->> theme :colors :background)
                   :backgroundGradientFrom (->> theme :colors :background color-darken)
                   :backgroundGradientTo   (->> theme :colors :background color-lighten)
                   :labelColor
                   (clj->js
                    #(color-hex-str->rgba
                      (->> theme :colors :primary)
                      (if-some [opacity %] opacity 1)))
                   :color
                   (clj->js
                    #(color-hex-str->rgba
                      (->> theme :colors :primary)
                      (if-some [opacity %] opacity 1)))})}]]
      ]]))
