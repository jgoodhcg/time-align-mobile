(ns time-align-mobile.components.day
  (:require [time-align-mobile.js-imports
             :refer [view
                     text
                     text-paper
                     fa
                     mci
                     mi
                     en
                     button-paper
                     scroll-view-gesture-handler
                     pan-gesture-handler
                     pinch-gesture-handler
                     touchable-ripple
                     tap-gesture-handler
                     divider
                     format-date
                     touchable-ripple
                     modal-paper
                     status-bar
                     portal
                     rect-button
                     surface
                     touchable-without-feedback
                     card
                     format-time
                     status-bar
                     touchable-highlight]]
            ["react-native-elements" :as rne]
            ["react" :as react]
            [time-align-mobile.styles :as styles :refer [styled-icon-factory]]
            [time-align-mobile.screens.period-form :as period-form]
            [time-align-mobile.screens.template-form :as template-form]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers
             :as helpers
             :refer [same-day?
                     get-gesture-handler-state
                     get-gesture-handler-ys]
             :rename {get-gesture-handler-state get-state
                      get-gesture-handler-ys get-ys}]
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

(def pinch-ref (.createRef react))

(def tap-ref (.createRef react))

(def double-tap-ref (.createRef react))

(def pan-offset (r/atom 0))

(defn render-collision-group [{:keys [pixel-to-minute-ratio
                                      displayed-day
                                      element-type
                                      selected-element
                                      collision-group]}]

  (->> collision-group
       (map-indexed
        (fn [index element]
          (let [start-ms     (helpers/abstract-element-timestamp
                              (:start element)
                              displayed-day)
                start-min    (helpers/ms->minutes start-ms)
                start-y-pos  (* pixel-to-minute-ratio start-min)
                stop-ms      (helpers/abstract-element-timestamp
                              (:stop element)
                              displayed-day)
                stop-min     (helpers/ms->minutes stop-ms)
                height       (* pixel-to-minute-ratio (- stop-min start-min))
                index-offset (-> index
                                 (* 16)
                                 (+ 2))
                selected     (= (:id element) (:id selected-element))]

            [surface {:key   (:id element)
                      :style (merge {:position      "absolute"
                                     :left          (str index-offset "%")
                                     :width         (str (- 96 index-offset) "%")
                                     :top           start-y-pos
                                     :height        height
                                     :elevation     (* 2 index)
                                     :border-radius 4
                                     :overflow      "hidden"}
                                    (when selected
                                      {:elevation    10
                                       :border-color "white"
                                       :border-width 4}))}
             [rect-button
              {:ref                     tap-ref
               :wait-for                double-tap-ref
               :on-handler-state-change #(let [state (get-state %)]
                                           (println (str "tap " state))
                                           (if (= :active state)
                                             (do
                                               (println "edit selection made"))))
               :style
               {:height           "100%"
                :width            "100%"
                :overflow         "hidden"
                :padding          4
                :background-color (:color element)}}

              ;; TODO add double tap
              [tap-gesture-handler
               {:ref                     double-tap-ref
                :number-of-taps          2
                :on-handler-state-change #(let [state (get-state %)]
                                            (println (str "double tap " state))
                                            (if (= :active state)
                                              (dispatch [:select-element-movement
                                                         {:element-type element-type
                                                          :id           (:id element)}])))}
               [view {:style {:width  "100%"
                              :height "100%"}}
                [text-paper (:label element)]]]]])))))

(defn elements-comp [{:keys [elements
                             element-type
                             selected-element
                             in-play-element
                             pixel-to-minute-ratio
                             displayed-day]}]
  [view {:style {:flex 1}}
   ;; planned
   [view {:style {:position           "absolute"
                  :top                0
                  :left               0
                  :width              "50%"
                  :height             "100%"
                  :border-color       (-> styles/theme :colors :disabled)
                  :border-left-width  0.5
                  :border-right-width 0.25}}

    (->> elements
         :planned
         (map #(render-collision-group
                {:pixel-to-minute-ratio pixel-to-minute-ratio
                 :displayed-day         displayed-day
                 :collision-group       %
                 :selected-element      selected-element
                 :element-type          element-type})))]

   ;; actual
   [view {:style {:position           "absolute"
                  :top                0
                  :left               "50%"
                  :width              "50%"
                  :height             "100%"
                  :border-color       (-> styles/theme :colors :disabled)
                  :border-left-width  0.25
                  :border-right-width 0.5}}
    (->> elements
         :actual
         (map #(render-collision-group
                {:pixel-to-minute-ratio pixel-to-minute-ratio
                 :displayed-day         displayed-day
                 :collision-group       %
                 :selected-element      selected-element
                 :element-type          element-type})))]])

(defn root
  "elements - {:actual [[collision-group-1] [collision-group-2]] :planned ... }"
  [{:keys [elements
           selected-element
           in-play-element
           element-type
           displayed-day
           move-element]}]
  (let [px-ratio-config       @(subscribe [:get-pixel-to-minute-ratio])
        pixel-to-minute-ratio (:current px-ratio-config)
        default-pxl-min-ratio (:default px-ratio-config)
        movement-selected     (some? selected-element)]

    ;; TODO add pan
    [pan-gesture-handler
     {:enabled                 movement-selected
      :on-gesture-event        #(do
                                  ;; TODO remove println
                                  (println "pan gesture")
                                  (if movement-selected
                                    (let [start-time-in-pixels  (+ @pan-offset (:y (get-ys %)))
                                          start-time-in-minutes (/ start-time-in-pixels
                                                                   pixel-to-minute-ratio)]
                                      (move-element {:selected-element   selected-element
                                                     :start-relative-min start-time-in-minutes}))))
      :on-handler-state-change #(let [y     (:y (get-ys %))
                                      state (get-state %)]
                                  (println (str "pan " state))
                                  (case state
                                    :active (let [top (->> selected-element
                                                           :start
                                                           (helpers/get-ms)
                                                           (helpers/ms->minutes)
                                                           (* pixel-to-minute-ratio))]
                                              (reset! pan-offset (- top y)))
                                    :end    (dispatch [:select-element-movement
                                                       {:element-type element-type
                                                        :id           nil}])
                                    nil))}

     [scroll-view-gesture-handler
      {;; :enabled                 (not movement-selected)
       ;; :disable-scroll-view-pan-responder movement-selected
       :scroll-enabled          (not movement-selected)
       ;; this stops all touch events from going to children kind of ... I guess.
       ;; My observation is that is somehow only stops child gesture events but not their  state changes.
       :wait-for                pinch-ref
       ;; TODO remove println
       :on-gesture-event        #(println "scroll gesture")
       :on-handler-state-change #(println (str "scroll " (get-state %)))}


      [pinch-gesture-handler
       {:ref                     pinch-ref
        :on-gesture-event        #(do (println "Pinch gesture")
                                      (println (helpers/get-gesture-handler-scale %))
                                      (let [scale (helpers/get-gesture-handler-scale %)]
                                        (dispatch [:set-current-pixel-to-minute-ratio
                                                   (* pixel-to-minute-ratio scale)])))
        ;; TODO remove println
        :on-handler-state-change #(println (str "pinch " (get-state %)))}
       [view
        {:style {:flex 1}}

        [view {:style {:height (* helpers/day-min pixel-to-minute-ratio)
                       :flex   1}}

         ;; time indicators
         (for [hour (range 1 helpers/day-hour)]
           (let [rel-min     (* 60 hour)
                 y-pos       (* pixel-to-minute-ratio rel-min)
                 rel-ms      (helpers/hours->ms hour)
                 time-str    (helpers/ms->hhmm rel-ms)
                 text-height 30]

             [view {:key   (str "hour-marker-" hour)
                    :style {:flex     1
                            :position "absolute"
                            :top      (- y-pos (/ text-height 2)) ;; so that the center of text is the y-pos
                            :height   (+ (* 60 pixel-to-minute-ratio)
                                         (/ text-height 2))}}

              [view {:style {:flex-direction "row"
                             :align-items    "flex-start"}}
               ;; time stamp
               [text-paper {:style {:padding-left        8
                                    :color               (-> styles/theme :colors :accent)
                                    :height              text-height
                                    :text-align-vertical "center"}}
                time-str]
               ;; bar
               [view {:style {:height         text-height
                              :flex-direction "row"
                              :align-items    "center"}}
                [view {:style {:border-color (-> styles/theme :colors :disabled)
                               :border-width 0.25
                               :margin-left  4
                               :width        "100%"
                               :height       0}}]]]]))

         ;; periods
         [view {:style {:position "absolute"
                        :left     60
                        :right    0
                        :height   "100%"}}
          [elements-comp {:elements              elements
                          :selected-element      selected-element
                          :element-type          element-type
                          :in-play-element       in-play-element
                          :pixel-to-minute-ratio pixel-to-minute-ratio
                          :displayed-day         displayed-day}]]]]]]]))
