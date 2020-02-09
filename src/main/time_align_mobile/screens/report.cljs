(ns time-align-mobile.screens.report
  (:require [time-align-mobile.js-imports :refer [view
                                                  line-chart
                                                  bar-chart
                                                  contribution-graph
                                                  surface
                                                  color-lighten
                                                  color-hex-str->rgba
                                                  button-paper
                                                  scroll-view
                                                  text-paper
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
  (let [chart-data         @(subscribe [:get-scores])
        chart-color-fn-gen (fn [color]
                             #(color-hex-str->rgba
                               color
                               (if-some [opacity %] opacity 1)))]

    [scroll-view {:style {:flex 1}}
     [top-bar {:center-content [subheading "Reports"]
               :right-content  [icon-button]}]
     [view {:style {:flex        1
                    :align-items "center"}}
      [view {:style {:flex           0
                     :flex-direction "column"
                     :align-items    "center"
                     :width          "100%"}}
        [contribution-graph
         {:values   chart-data
          :width    350
          :height   250
          :num-days 90
          :chart-config
          (clj->js {:backgroundColor        (->> theme :colors :background)
                    :backgroundGradientFrom (->> theme :colors :background)
                    :backgroundGradientTo   (->> theme :colors :background)
                    :labelColor             (chart-color-fn-gen (->> theme :colors :accent))
                    :color                  (chart-color-fn-gen (->> theme :colors :accent))})}]

       [button-paper {:on-press #(dispatch [:set-report-data])
                      :mode     "contained"
                      :style    {:margin 16}
                      :icon     "refresh"}
         "calculate scores"]]]]))
