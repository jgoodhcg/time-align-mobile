(ns time-align-mobile.screens.report
  (:require [time-align-mobile.js-imports :refer [view
                                                  line-chart
                                                  bar-chart
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
  (let [bar-chart-data @(subscribe [:get-cumulative-h-by-bucket])]
    [view {:style {:flex 1}}
     [top-bar {:center-content [subheading "Reports"]
               :right-content  [icon-button]}]
     [view {:style {:flex        1
                    :align-items "center"}}

      [surface
       [bar-chart
        {:data
         (clj->js {:labels   (clj->js (->> bar-chart-data (map :label)))
                   :datasets (clj->js
                              [(clj->js {:data
                                         (clj->js
                                          (->> bar-chart-data
                                               (map :cumulative-hours)))})])})
         :vertical-label-rotation 45
         :width                   400
         :height                  400
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
