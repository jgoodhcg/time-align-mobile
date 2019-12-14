(ns time-align-mobile.screens.report
  (:require [time-align-mobile.js-imports :refer [view
                                                  contribution-graph
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
  (let [chart-data               @(subscribe [:get-stacked-bar-week])
        stacked-bar-chart-config (clj->js {:backgroundColor        (->> theme :colors :background)
                                           :backgroundGradientFrom (->> theme :colors :background color-darken)
                                           :backgroundGradientTo   (->> theme :colors :background color-lighten)
                                           :propsForLabels         {:textAnchor "middle"}
                                           :labelColor
                                           (clj->js
                                            #(color-hex-str->rgba
                                              (->> theme :colors :primary)
                                              (if-some [opacity %] opacity 1)))
                                           :color
                                           (clj->js
                                            #(color-hex-str->rgba
                                              (->> theme :colors :primary)
                                              (if-some [opacity %] opacity 1)))})
        other-chart-props        {;; :vertical-label-rotation 90
                                  :width                  1000
                                  :height                 1000
                                  ;; :with-horizontal-labels false
                                  :with-vertical-labels   false
                                  ;; :acessor                 "population"
                                  :background             "transparent"
                                  ;; :from-zero               true
                                  :chart-config           stacked-bar-chart-config}]

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


        ]]]]))
