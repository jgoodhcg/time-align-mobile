(ns time-align-mobile.components.day
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  text-paper
                                                  fa
                                                  mci
                                                  mi
                                                  en
                                                  button-paper
                                                  scroll-view
                                                  touchable-ripple
                                                  divider
                                                  format-date
                                                  touchable-ripple
                                                  modal-paper
                                                  scroll-view
                                                  status-bar
                                                  portal
                                                  surface
                                                  touchable-without-feedback
                                                  card
                                                  format-time
                                                  status-bar
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]
            [time-align-mobile.styles :as styles :refer [styled-icon-factory]]
            [time-align-mobile.screens.period-form :as period-form]
            [time-align-mobile.screens.template-form :as template-form]
            ["react-native-floating-action" :as fab]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers :as helpers :refer [same-day?]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(defn root [{:keys [collision-grouped-elements]}]
  (let [px-ratio-config @(subscribe [:get-pixel-to-minute-ratio])
        pixel-to-minute-ratio      (:current px-ratio-config)
        default-pxl-min-ratio      (:default px-ratio-config)]
   [scroll-view
   [view
    {:style (merge
             ;; testing styles
             {}
             ;; actual styles
             {:flex 1})}

    [touchable-ripple {:style         {}
                       :on-press      #(dispatch
                                        [:set-current-pixel-to-minute-ratio
                                         (* 1.1 pixel-to-minute-ratio)])
                       :on-long-press #(dispatch
                                        [:set-current-pixel-to-minute-ratio
                                         default-pxl-min-ratio])}

     [:<>
      ;; time indicators
      (for [hour (range helpers/day-hour)]
        (let [rel-min  (* 60 hour)
              y-pos    (/ pixel-to-minute-ratio rel-min)
              rel-ms   (helpers/hours->ms hour)
              time-str (helpers/ms->hhmm rel-ms)]

          [view {:key   (str "hour-marker-" hour)
                 :style {:top    y-pos
                         :height (* 60 pixel-to-minute-ratio)}}

           [:<>
            [divider]
            [text-paper {:style {:padding-left 8
                                 :color        (-> styles/theme :colors :disabled)}}
             time-str]]]))

      ;; periods
      [touchable-ripple {:style    {:position "absolute"
                                    :left     60
                                    :right    0
                                    :height   "100%"}
                         :on-press #(println "pressed periods")}
       [:<>]]]]]]))
