(ns time-align-mobile.screens.report
  (:require [time-align-mobile.js-imports :refer [view
                                                  line-chart
                                                  bar-chart
                                                  contribution-graph
                                                  progress-ring-chart
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
        chart-data-week    @(subscribe [:get-week-scores])
        selected-day-data  @(subscribe [:get-day-score])
        chart-color-fn-gen (fn [color]
                             #(color-hex-str->rgba
                               color
                               (if-some [opacity %] opacity 1)))
        chart-config
        (clj->js
         {:backgroundColor        (->> theme :colors :background)
          :backgroundGradientFrom (->> theme :colors :background)
          :backgroundGradientTo   (->> theme :colors :background)
          :labelColor             (chart-color-fn-gen (->> theme :colors :accent))
          :color                  (chart-color-fn-gen (->> theme :colors :accent))})]

    [scroll-view {:style {:flex 1}}
     [top-bar {:center-content [subheading "Reports"]
               :right-content  [icon-button]}]
     [view {:style {:flex        1
                    :align-items "center"}}
      [view {:style {:flex           0
                     :flex-direction "column"
                     :align-items    "center"
                     :width          "100%"}}

       [button-paper {:on-press #(dispatch [:set-report-data])
                      :mode     "contained"
                      :style    {:margin 16}
                      :icon     "refresh"}

        "calculate scores"]
       [contribution-graph
        {:values       chart-data
         :width        350
         :height       250
         :num-days     90
         :chart-config chart-config}]

       [text-paper "How much did you plan and track?"]
       [line-chart
        {:data
         (clj->js
          {:labels   (:labels chart-data-week)
           :datasets [{:data (:data-amount chart-data-week)}]})
         :width        350
         :height       250
         :chart-config chart-config}]

       [text-paper "Did you do what you wanted?"]
       [line-chart
        {:data
         (clj->js
          {:labels   (:labels chart-data-week)
           :datasets [{:data (:data-where chart-data-week)}]})
         :width        350
         :height       250
         :chart-config chart-config}]

       [text-paper "Did you do it when you wanted?"]
       [line-chart
        {:data
         (clj->js
          {:labels   (:labels chart-data-week)
           :datasets [{:data (:data-when chart-data-week)}]})
         :width        350
         :height       250
         :chart-config chart-config}]


       ]]]))
