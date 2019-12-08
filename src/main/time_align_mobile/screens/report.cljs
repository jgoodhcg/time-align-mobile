(ns time-align-mobile.screens.report
  (:require [time-align-mobile.js-imports :refer [view
                                                  line-chart
                                                  bar-chart
                                                  pie-chart
                                                  stacked-bar-chart
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
  (let [pie-chart-data           @(subscribe [:get-cumulative-h-by-bucket])
        stacked-bar-chart-config (clj->js {:backgroundColor        (->> theme :colors :background)
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
                                              (if-some [opacity %] opacity 1)))})]
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

        [headline "Actual cumulative time by group"]
        [stacked-bar-chart
         {:data                    (clj->js {:labels    ["Test1" "Test2"]
                                             :legend    ["L1" "L2" "L3"]
                                             :data      [[50 50 60] [12 25 62]]
                                             :barColors ["#dfe4ea" "#ced6e0" "#a4b0be"]})
          :vertical-label-rotation 90
          :width                   400
          :height                  400
          :acessor                 "population"
          :background              "transparent"
          :from-zero               true
          :chart-config            stacked-bar-chart-config}]]]

      [surface
       [view {:style {:flex           0
                      :flex-direction "column"
                      :align-items    "center"
                      :width          "100%"}}

        [headline "Planned cumulative time by group"]
        [stacked-bar-chart
         {:data                    (clj->js {:labels    ["Test1" "Test2"]
                                             :legend    ["L1" "L2" "L3"]
                                             :data      [[50 50 60] [12 25 62]]
                                             :barColors ["#dfe4ea" "#ced6e0" "#a4b0be"]})
          :vertical-label-rotation 90
          :width                   400
          :height                  400
          :acessor                 "population"
          :background              "transparent"
          :from-zero               true
          :chart-config            stacked-bar-chart-config}]]]]]))
