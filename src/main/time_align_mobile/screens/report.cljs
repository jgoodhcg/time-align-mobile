(ns time-align-mobile.screens.report
  (:require [time-align-mobile.js-imports :refer [view
                                                  line-chart
                                                  surface
                                                  color-lighten
                                                  color-hex-str->rgba
                                                  scroll-view
                                                  color-darken
                                                  subheading
                                                  headline
                                                  icon-button
                                                  text]]
            [time-align-mobile.components.top-bar :refer [top-bar]]
            [time-align-mobile.styles :refer [theme]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            ["react" :as react]))

(defn root [params]
  (let [chart-data        @(subscribe [:get-tracked-time-by-day])
        chart-config      {:backgroundColor        (->> theme :colors :background)
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
                              (if-some [opacity %] opacity 1)))}
        other-chart-props {:width                350
                           :height               350
                           :with-vertical-labels false
                           :background           "transparent"
                           :chart-config         (clj->js chart-config)}]

    (println chart-data)
    [scroll-view {:style {:flex 1}}
     [top-bar {:center-content [subheading "Reports"]
               :right-content  [icon-button]}]
     [view {:style {:flex        1
                    :align-items "center"}}

      [surface
       [view {:style {:flex           0
                      :flex-direction "column"
                      :align-items    "center"
                      :width          "100%"}}
        [line-chart (clj->js
                     (merge other-chart-props {:data chart-data}))]

        ]]]]))
