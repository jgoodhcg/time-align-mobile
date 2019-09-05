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
                     dispatch-debounced
                     dispatch-throttled
                     get-gesture-handler-ys]
             :rename {get-gesture-handler-state get-state
                      get-gesture-handler-ys get-ys}]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(def pan-offset (r/atom 0))

(defn render-collision-group [{:keys [pixel-to-minute-ratio
                                      displayed-day
                                      element-type
                                      selected-element
                                      collision-group]}]

  (->> collision-group
       (map-indexed
        (fn [index element]
          (let [start-ms                (helpers/abstract-element-timestamp
                                         (:start element)
                                         displayed-day)
                start-min               (helpers/ms->minutes start-ms)
                start-y-pos             (* pixel-to-minute-ratio start-min)
                stop-ms                 (helpers/abstract-element-timestamp
                                         (:stop element)
                                         displayed-day)
                stop-min                (helpers/ms->minutes stop-ms)
                height                  (* pixel-to-minute-ratio (- stop-min start-min))
                index-offset            (-> index
                                            (* 16)
                                            (+ 2))
                selected                (= (:id element) (:id selected-element))
                something-else-selected (and (some? selected-element)
                                             (not selected))
                double-tap-ref          (.createRef react)]

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
                                      {:elevation 10}))}
             [rect-button
              {:enabled                 (and (not selected)
                                             (not something-else-selected))
               :wait-for                double-tap-ref
               :on-handler-state-change #(if (= :active (get-state %))
                                           (dispatch
                                            [:select-element-edit
                                             {:element-type element-type
                                              :id           (:id element)}]))
               :style
               (merge
                {:height           "100%"
                 :width            "100%"
                 :overflow         "hidden"
                 :padding          4
                 :background-color (:color element)}
                (when something-else-selected
                  {:opacity 0.5}))}

              [tap-gesture-handler
               {:ref            double-tap-ref
                :number-of-taps 2

                :on-handler-state-change
                #(let [state (get-state %)]
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

    (->> @elements
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
    (->> @elements
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
        movement-selected     (some? selected-element)
        pinch-ref             (.createRef react)]

    [pan-gesture-handler
     {:enabled movement-selected
      :on-gesture-event
      (if movement-selected
        #(let [start-time-in-pixels  (+ @pan-offset (:y (get-ys %)))
               start-time-in-minutes (/ start-time-in-pixels
                                        pixel-to-minute-ratio)]
           (move-element {:selected-element   selected-element
                          :start-relative-min start-time-in-minutes})))

      :on-handler-state-change
      #(let [y     (:y (get-ys %))
             state (get-state %)]
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
      {:scroll-enabled (not movement-selected)
       ;; this stops all touch events from going to children kind of ... I guess.
       ;; My observation is that is somehow only stops child gesture events but not their  state changes.
       :wait-for       pinch-ref}


      [pinch-gesture-handler
       {:ref              pinch-ref
        :on-gesture-event #(let [scale (helpers/get-gesture-handler-scale %)]
                             (dispatch-debounced
                              [:set-current-pixel-to-minute-ratio
                               (* pixel-to-minute-ratio scale)]))}
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
