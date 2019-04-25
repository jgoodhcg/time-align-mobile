(ns time-align-mobile.components.day
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  status-bar
                                                  touchable-highlight]]
            [time-align-mobile.styles :as styles]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers :as helpers]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(def padding 20) ;; TODO refactor to styling

(defn time-indicators
  ([dimensions] (time-indicators dimensions (js/Date.)))
  ([dimensions displayed-day]
   (let [six-in-the-morning      (->> displayed-day
                                     (helpers/reset-relative-ms (* 1000 60 60 6)))
        twelve-in-the-afternoon (->> displayed-day
                                     (helpers/reset-relative-ms (* 1000 60 60 12)))
        six-in-the-evening      (->> displayed-day
                                     (helpers/reset-relative-ms (* 1000 60 60 18)))
        container-style         {:position    "absolute"
                                 :left        0
                                 :align-items "center"}
        line-style              (merge
                                 styles/time-indicator-line-style
                                 {:width (:width dimensions)})
        text-style              (merge
                                 styles/time-indicator-text-style
                                 ;; {:padding-left 16}
                                 )]

    [view
     [view {:style (merge container-style
                          {:top (-> six-in-the-morning
                                    (helpers/date->y-pos (:height dimensions))
                                    (max 0)
                                    (min (:height dimensions)))})}

      [view {:style line-style}]
      [text {:style text-style} "06:00"]]

     [view {:style (merge container-style
                          {:top (-> twelve-in-the-afternoon
                                    (helpers/date->y-pos (:height dimensions))
                                    (max 0)
                                    (min (:height dimensions)))})}

      [view line-style]
      [text {:style text-style} "12:00"]]

     [view {:style (merge container-style
                          {:top (-> six-in-the-evening
                                    (helpers/date->y-pos (:height dimensions))
                                    (max 0)
                                    (min (:height dimensions)))})}
      [view line-style]
      [text {:style text-style} "18:00"]]])))

(defn render-period [{:keys [period
                             collision-index
                             collision-group-size
                             dimensions
                             displayed-day
                             period-in-play
                             selected-period]}]
  (let [{:keys [id start stop planned color label bucket-label]} period

        adjusted-stop  (helpers/bound-stop stop displayed-day)
        adjusted-start (helpers/bound-start start displayed-day)
        top            (-> adjusted-start
                           (helpers/date->y-pos (:height dimensions))
                           (max 0)
                           (min (:height dimensions)))
        width          (-> dimensions
                           (:width)
                           (/ 2)
                           (- (* 2 padding))
                           (/ collision-group-size))
        left           (-> dimensions
                           (:width)
                           (/ 2)
                           (#(if planned
                               padding
                               (+ % padding)))
                           (+ (* collision-index width)))
        height         (-> adjusted-stop
                           (.valueOf)
                           (- (.valueOf adjusted-start))
                           (helpers/duration->height (:height dimensions))
                           ;; max 1 to actually see recently played periods
                           (max 1))]

    [view {:key id}
     (when (= id (:id selected-period))
       [view {:style {:position         "absolute"
                      :top              top
                      :left             left
                      :width            width
                      :height           height
                      :elevation        4
                      :border-radius    2
                      :background-color "white"}}])

     [view {:style (merge (when (= id (:id selected-period))
                            {:elevation 5})
                          {:position         "absolute"
                           :top              top
                           :left             left
                           :width            width
                           :height           height
                           :border-radius    2
                           :background-color color})}

      [touchable-highlight {:style    {:width          "100%"
                                       :height         "100%"
                                       :padding-left   10
                                       :padding-right  10
                                       :padding-top    0
                                       :padding-bottom 0}
                            :on-press #(dispatch [:select-period id])}
       [view
        ;; [text label]
        ;; [text bucket-label]
        ]]]]))

