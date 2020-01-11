(ns time-align-mobile.screens.report
  (:require [time-align-mobile.js-imports :refer [view
                                                  line-chart
                                                  bar-chart
                                                  surface
                                                  color-lighten
                                                  color-hex-str->rgba
                                                  button-paper
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

      [surface {:style {:margin-top 32}}
       [view {:style {:flex           0
                      :flex-direction "column"
                      :align-items    "center"
                      :width          "100%"}}
        [subheading "Scores for the past 7 days"]
        [bar-chart
         {:data     chart-data
          :width    400
          :height   400
          :fromZero true

          :chart-config
          (clj->js {:backgroundColor        (->> theme :colors :surface)
                    :backgroundGradientFrom (->> theme :colors :surface)
                    :backgroundGradientTo   (->> theme :colors :surface)
                    :labelColor             (chart-color-fn-gen (->> theme :colors :primary))
                    :color                  (chart-color-fn-gen (->> theme :colors :primary))})}]

        [view {:style {:flex-direction "column"
                       :justify-content "flex-start"}}
         [text "For each minute in a day:"]
         [text "  - 4 pts if you did exactly what you planned to do"]
         [text "  - 3 pts if you did some of what you planned to do"]
         [text "  - 2 pts if you planned but did something else"]
         [text "  - 1 pts if you planned _or_ did something"]
         [text "  - 0 pts if you did not plan or do anything"]]

        [button-paper {:on-press #(dispatch [:set-report-data])
                       :mode     "outlined"
                       :style    {:margin 16}
                       :icon     "refresh"}
         "calculate scores"]]]]]))
