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

(def pixel-minute-ratio 0.50)
(def total-height-pixel (* helpers/day-ms pixel-minute-ratio))

(defn day-display []
  [scroll-view
   [view
    {:style (merge
             ;; testing styles
             {:border-color "blue"
              :border-width 8}
             ;; actual styles
             {:flex 1})}

    (println "+++++++++++++++++++")
    ;; time indicators
    [view {:style {}}
     (for [hour (range helpers/day-hour)]
       (let [rel-min (* 60 hour)
             y-pos   (/ pixel-minute-ratio rel-min)]
         (println {:rel-min rel-min
                   :hour    hour
                   :y-pos   y-pos})
         [view {:key   (str "hour-marker-" hour)
                :style {:top            y-pos
                        :height         (* 60 pixel-minute-ratio)
                        :padding-top    0
                        :padding-bottom 4}}
          [view {:style {:background-color "grey"
                         :height           "100%"
                         :width            "100%"}}
           [text (str "hour-marker-" hour)]]]))]]])

(defn root []
  [view {:style {:flex 1}}
   [status-bar {:hidden true}]
   [top-bar]
   [day-display]])
