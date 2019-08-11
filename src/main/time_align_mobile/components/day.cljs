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

;; (defn render-period [{:keys [entity
;;                              entity-type
;;                              transform-functions
;;                              select-function-generator
;;                              collision-index
;;                              collision-group-size
;;                              dimensions
;;                              displayed-day
;;                              period-in-play
;;                              selected-entity]}]
;;   (let [{:keys [id start stop planned color label bucket-label]}
;;         entity
;;         {:keys [up down start-earlier stop-earlier start-later stop-later]}
;;         transform-functions

;;         selected       (= id (:id selected-entity))
;;         opacity        1
;;         adjusted-stop  (helpers/bound-stop stop displayed-day)
;;         adjusted-start (helpers/bound-start start displayed-day)
;;         top            (-> adjusted-start
;;                            (helpers/date->y-pos (:height dimensions))
;;                            (max 0)
;;                            (min (:height dimensions)))
;;         base-width     (-> dimensions
;;                            (:width)
;;                            (/ 2)
;;                            )
;;         width          (-> base-width
;;                            (- (* 2 padding))
;;                            (/ collision-group-size))
;;         base-left      (-> dimensions
;;                            (:width)
;;                            (/ 2)
;;                            (#(if planned
;;                                0
;;                                %)))
;;         left           (-> base-left
;;                            (+ padding)
;;                            (+ (* collision-index width)))
;;         height         (-> adjusted-stop
;;                            (.valueOf)
;;                            (- (.valueOf adjusted-start))
;;                            (helpers/duration->height (:height dimensions))
;;                            ;; max 1 to actually see recently played periods
;;                            (max 1))

;;         button-height-default 40
;;         button-height-top     (min (- (:height dimensions)
;;                                       button-height-default)
;;                                    (max top button-height-default))
;;         button-height-bottom  (max (- (:height dimensions)
;;                                       (+ top height))
;;                                    button-height-default)

;;         button-width (/ base-width 3)
;;         base-style   {:position      "absolute"}
;;         period-style (merge base-style {:top              top
;;                                         :left             left
;;                                         :width            width
;;                                         :border-radius    2
;;                                         :overflow         "hidden"
;;                                         :opacity          opacity
;;                                         :height           height
;;                                         :background-color color})
;;         button-style (merge base-style {:justify-content  "flex-start"
;;                                         :align-items      "center"
;;                                         :width            button-width
;;                                         :border-color     styles/text-light
;;                                         :border-width     1
;;                                         :opacity          0.35
;;                                         :background-color "grey"})
;;         top-style    (merge button-style {:height button-height-top
;;                                           :top    0})
;;         bottom-style (merge button-style {:height button-height-bottom
;;                                           :top    (min (- (:height dimensions)
;;                                                           button-height-default)
;;                                                        (max (+ top height)
;;                                                             button-height-default))
;;                                           })
;;         icon-style   {:color styles/text-light}
;;         mci-styled   (styled-icon-factory mci icon-style)
;;         mi-styled    (styled-icon-factory mi icon-style)
;;         icon-params  (fn [name] {:size 32 :name name})]

;;     [view {:key id}
;;      ;; period
;;      [touchable-ripple {:style    period-style
;;                         :on-press (select-function-generator id)}
;;       [:<>
;;        [text-paper label]
;;        [text-paper bucket-label]]]

;;      ;; buttons
;;      (when selected
;;        [:<>
;;         ;; Top buttons

;;         ;; up
;;         [touchable-highlight {:style         (merge top-style {:left base-left})
;;                               :on-press      (up selected-entity)
;;                               :on-long-press (up selected-entity true)}
;;          [mi-styled (icon-params "arrow-upward")]]

;;         ;; start-earlier
;;         [touchable-highlight {:style         (merge top-style
;;                                                     {:left (+ base-left button-width)})
;;                               :on-press      (start-earlier selected-entity)
;;                               :on-long-press (start-earlier selected-entity true)}
;;          [mci-styled (icon-params "arrow-collapse-up")]]

;;         ;; start-later
;;         [touchable-highlight {:style         (merge top-style
;;                                                     {:left (+ base-left (* 2 button-width))})
;;                               :on-press      (start-later selected-entity)
;;                               :on-long-press (start-later selected-entity true)}

;;          [mci-styled (icon-params "arrow-collapse-down")]]
;;         ;; Buttom buttons

;;         ;; down
;;         [touchable-highlight {:style         (merge bottom-style {:left base-left})
;;                               :on-press      (down selected-entity)
;;                               :on-long-press (down selected-entity true)}
;;          [mi-styled (icon-params "arrow-downward")]]

;;         ;; stop-later
;;         [touchable-highlight {:style         (merge bottom-style {:left (+ base-left button-width)})
;;                               :on-press      (stop-later selected-entity)
;;                               :on-long-press (stop-later selected-entity true)}
;;          [mci-styled (icon-params "arrow-expand-down")]]

;;         ;; stop-earlier
;;         [touchable-highlight {:style         (merge bottom-style {:left (+ base-left (* 2 button-width))})
;;                               :on-press      (stop-earlier selected-entity)
;;                               :on-long-press (stop-earlier selected-entity true)}
;;          [mci-styled (icon-params "arrow-expand-up")]]])]))

;; (defn render-periods-col
;;   "Renders all non-selected only when `render-selected-only` is false. Only renders selected when it is true."
;;   [{:keys [periods
;;            displayed-day
;;            dimensions
;;            selected-period
;;            period-in-play
;;            render-selected-only]}]
;;   (->> periods
;;        (map (fn [collision-group]
;;               (doall
;;                (->> collision-group
;;                     (map-indexed
;;                      (fn [index period]
;;                        (when (xor render-selected-only
;;                                   (not= (:id period) (:id selected-period)))
;;                          (render-period
;;                           {:entity                    period
;;                            :entity-type               :period
;;                            :transform-functions       period-transform-functions
;;                            :collision-index           index
;;                            :collision-group-size      (count collision-group)
;;                            :displayed-day             displayed-day
;;                            :dimensions                dimensions
;;                            :select-function-generator (fn [id]
;;                                                         #(dispatch [:select-period id]))
;;                            :selected-entity           selected-period
;;                            :period-in-play            period-in-play}))))))))))

;; (defn periods-comp [{:keys [displayed-day
;;                             selected-period ;; TODO handle deref better in the args name and/or function body
;;                             period-in-play
;;                             periods
;;                             dimensions]}]
;;   (let [periods-combined (->> @periods  ;; TODO maybe refactor or find another way to do this?
;;                               (#(concat (:actual %)
;;                                         (:planned %))))
;;         sel-period @selected-period]
;;     [view
;;      ;; render everything but selected
;;      (render-periods-col {:periods periods-combined
;;                           :displayed-day @displayed-day
;;                           :dimensions @dimensions
;;                           :selected-period sel-period
;;                           :period-in-play @period-in-play
;;                           :render-selected-only false})
;;      ;; render only the selected
;;      (render-periods-col {:periods periods-combined
;;                           :displayed-day @displayed-day
;;                           :dimensions @dimensions
;;                           :selected-period sel-period
;;                           :period-in-play @period-in-play
;;                           :render-selected-only true})]))

(defn root [{:keys [collision-grouped-elements]}]
  (let [px-ratio-config       @(subscribe [:get-pixel-to-minute-ratio])
        pixel-to-minute-ratio (:current px-ratio-config)
        default-pxl-min-ratio (:default px-ratio-config)]
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

           [view {:style {:flex-direction "row"
                          :align-items    "center"}}
            ;; time stamp
            [text-paper {:style {:padding-left 8
                                 :color        (-> styles/theme :colors :accent)}}
             time-str]
            ;; bar
            [view {:style {:border-color (-> styles/theme :colors :disabled)
                           :border-width 0.50
                           :margin-left  4
                           :width        "100%"
                           :height       0}}]]]))

      ;; periods
      [touchable-ripple {:style    {:position "absolute"
                                    :left     60
                                    :right    0
                                    :height   "100%"}
                         :on-press #(println "pressed periods")}
       [:<>]]]]]]))
