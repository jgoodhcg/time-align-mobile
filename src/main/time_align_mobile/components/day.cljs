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

(defn day-display []
  [scroll-view
   [view
    {:style (merge
             ;; testing styles
             {:border-width    8
              :border-color    "blue"
              :flex-direction  "column"
              :justify-content "space-between"}
             ;; actual styles
             {:height 1440})}

    [view {:style {:height 50}}[text "day stuff top"]]
    [view {:style {:height 50}}[text "day stuff bottom"]]]])

(defn root []
  [view {:style {:flex 1}}
   [status-bar {:hidden true}]
   [top-bar]
   [day-display]])
