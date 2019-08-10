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

(defn top-bar []
  [view
   {:style (merge
            ;; testing styles
            {:border-width 8
             :border-color "red"}
            ;; actual styles
            {})}

   [text {:style {:height 60}} "top bar stuff"]])

(def pixel-minute-ratio 5)

(def total-height-pixel (* helpers/day-ms pixel-minute-ratio))

(defn day-display []
  [scroll-view
   [view
    {:style (merge
             ;; testing styles
             {}
             ;; actual styles
             {:flex 1})}

    [view {:style {}}

     ;; time indicators
     (for [hour (range helpers/day-hour)]
       (let [rel-min  (* 60 hour)
             y-pos    (/ pixel-minute-ratio rel-min)
             rel-ms   (helpers/hours->ms hour)
             time-str (helpers/ms->hhmm rel-ms)]

         [view {:key   (str "hour-marker-" hour)
                :style {:top    y-pos
                        :height (* 60 pixel-minute-ratio)}}

          [:<>
           [divider]
           [text-paper {:style {:padding-left 8
                                :color        (-> styles/theme :colors :disabled)}}
            time-str]]]))

     ;; periods
     [view {:style {:left         60
                    :right        0
                    :border-color "blue"
                    :border-width 8
                    :position     "absolute"
                    :height       "100%"}}
      [text "periods"]]]]])

(defn root []
  [view {:style {:flex 1}}
   [status-bar {:hidden true}]
   [top-bar]
   [day-display]])
