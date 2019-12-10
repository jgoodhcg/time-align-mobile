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
  (let [chart-data               @(subscribe [:get-stacked-bar-week])
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
                                              (if-some [opacity %] opacity 1)))})
        other-chart-props        {:vertical-label-rotation 90
                                  :width                   400
                                  :height                  400
                                  :acessor                 "population"
                                  :background              "transparent"
                                  :from-zero               true
                                  :chart-config            stacked-bar-chart-config}]

    (println (clj->js (:actual chart-data)))

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
         (merge other-chart-props
                {:data
                 #js {:labels #js [1 0 6 5 4 3 2],
                      :legend #js [🤹‍♂️ misc 🥘 food ➰ maintenance 🛌 sleep 🍎 health 👨‍💼 career 👥 social 🌱 growth ✔️ planning 📱 leisure],
                      :data #js [#js [1 3 0 0 0 0 0 0 0 0]
                                 #js [0 0 3 0 0 0 0 0 0 0]
                                 #js [0 0 0 3 0 0 0 0 0 0]
                                 #js [0 0 0 0 3 0 0 0 0 0]
                                 #js [0 0 0 0 0 3 0 0 0 0]
                                 #js [0 0 0 0 0 0 2 0 0 0]
                                 #js [0 0 0 0 0 0 0 9 0 0]],
                      :barColors #js ["#8b8b8b"
                                      #98ff11
                                      #46e5ff
                                      #9711ff
                                      #60e563
                                      #dd0f1d
                                      #ffd611
                                      #11a5ff
                                      #89f1ed #ffac11]}
                 ;; (clj->js (:actual chart-data))
                 })]]]

      ;; [surface
      ;;  [view {:style {:flex           0
      ;;                 :flex-direction "column"
      ;;                 :align-items    "center"
      ;;                 :width          "100%"}}

      ;;   [headline "Planned cumulative time by group"]
      ;;   [stacked-bar-chart
      ;;    (merge other-chart-props
      ;;           {:data (clj->js (:planned chart-data))})]]]
      ]]))
