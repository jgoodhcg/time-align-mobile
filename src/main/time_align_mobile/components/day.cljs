(ns time-align-mobile.components.day
  (:require [time-align-mobile.js-imports
             :refer [view
                     text
                     text-paper
                     fa
                     mci
                     mi
                     alert
                     en
                     button-paper
                     scroll-view-gesture-handler
                     pan-gesture-handler
                     pinch-gesture-handler
                     bottom-sheet
                     touchable-ripple
                     tap-gesture-handler
                     divider
                     format-date
                     touchable-ripple
                     get-device-width
                     portal
                     modal-paper
                     status-bar
                     portal
                     rect-button
                     surface
                     touchable-without-feedback
                     color-light?
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
                     get-gesture-handler-ys
                     get-gesture-handler-xs]
             :rename {get-gesture-handler-state get-state
                      get-gesture-handler-ys get-ys
                      get-gesture-handler-xs get-xs}]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(def pan-offset (r/atom 0))

(def now (r/atom (js/Date.)))

;; start ticking
(js/setInterval #(reset! now (js/Date.)) 1000)

(def bottom-sheet-ref (r/atom nil))

(defn snap-bottom-sheet [bottom-sheet-ref snap]
  ;; TODO refactor this to let the callers of this and close-bottom-sheet deref
  (let [bsr @bottom-sheet-ref]
    ;; bsr might not be set yet
    (try
      (ocall bsr "snapTo" snap)
      (catch js/Error e
        (do
          ;; no op for any other screen
          (println e))))))

(defn close-bottom-sheet [bottom-sheet-ref element-type]
  (snap-bottom-sheet bottom-sheet-ref 0)
  (dispatch [:select-element-edit {:element-type element-type
                                   :bucket-id    nil
                                   :element-id   nil}]))

(defn render-collision-group [{:keys [pixel-to-minute-ratio
                                      displayed-day
                                      element-type
                                      selected-element
                                      selected-element-edit
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
                index-offset            (min 90
                                             (-> index
                                                 (* 16)
                                                 (+ 2)))
                light                   (color-light? (:color element))
                selected                (= (:id element) (:id selected-element))
                selected-edit           (= (:id element) (:id selected-element-edit))
                something-else-selected (and (some? selected-element)
                                             (not selected))
                double-tap-ref          (.createRef react)
                left                    (str index-offset "%")
                width                   (str (max 4 (- 96 index-offset)) "%")]

            [surface {:key   (:id element)
                      :style (merge {:position         "absolute"
                                     :left             left
                                     :width            width
                                     :top              start-y-pos
                                     :height           height
                                     :elevation        (* 2 index)
                                     :border-radius    8
                                     :background-color (:color element)
                                     :overflow         "hidden"}
                                    (when selected
                                      {:elevation    32
                                       :border-width 4.5
                                       :border-color (-> styles/theme :colors :text)
                                       :border-style "dotted"})
                                    (when selected-edit
                                      {:border-color (-> styles/theme :colors :text)
                                       :border-width 4.5}))}
             [rect-button
              {:enabled                 (and (not selected)
                                             (not something-else-selected))
               :wait-for                double-tap-ref
               :on-handler-state-change #(if (= :active (get-state %))
                                           (if (not selected-edit)
                                             ;; select this element
                                             (do (snap-bottom-sheet bottom-sheet-ref 1)
                                                 (dispatch
                                                  [:select-element-edit
                                                   {:element-type element-type
                                                    :bucket-id    (:bucket-id element)
                                                    :element-id   (:id element)}]))
                                             ;; deselect this element
                                             (do (close-bottom-sheet bottom-sheet-ref element-type))))
               :style
               (merge
                {:height           "100%"
                 :width            "100%"
                 :overflow         "hidden"
                 :background-color (:color element)
                 :padding          4}
                (when something-else-selected
                  {:opacity 0.5}))}

              [tap-gesture-handler
               {:ref            double-tap-ref
                :number-of-taps 2

                :on-handler-state-change
                #(let [state (get-state %)]
                   (if (= :active state)
                     (do
                       (close-bottom-sheet bottom-sheet-ref element-type)
                       (dispatch [:select-element-movement
                                  {:element-type element-type
                                   :bucket-id    (:bucket-id element)
                                   :element-id   (:id element)}]))))}
               [view {:style {:width  "100%"
                              :height "100%"}}
                [text-paper {:style {:color (if light
                                              (-> styles/theme :colors :element-text-dark)
                                              (-> styles/theme :colors :element-text-light))}}
                 (:label element)]]]]])))))

(defn elements-comp [{:keys [elements
                             element-type
                             selected-element
                             selected-element-edit
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
                 :selected-element-edit selected-element-edit
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
                 :selected-element-edit selected-element-edit
                 :element-type          element-type})))]])

(defn transform-button [selected-element-edit t-function icon]
  [touchable-ripple {:on-press      #(t-function selected-element-edit)
                     :on-long-press #(t-function selected-element-edit true)
                     :style         {:padding 8}}
   icon])

(defn transform-buttons [{:keys [transform-functions selected-element-edit]}]
  (let [{:keys [up
                down
                start-earlier
                stop-earlier
                stop-later
                start-later]} transform-functions
        icon-style            {:color (-> styles/theme :colors :text)}
        mci-styled            (styled-icon-factory mci icon-style)
        mi-styled             (styled-icon-factory mi icon-style)
        icon-params           (fn [name] {:size 28 :name name})
        t-btn                 (partial transform-button selected-element-edit)]

    [surface
     [view {:style {:flex            0
                    :width           "100%"
                    :flex-direction  "row"
                    :justify-content "space-around"
                    :padding         2
                    :height          50}}

      [t-btn up [mi-styled (icon-params "arrow-upward")]]
      [t-btn down [mi-styled (icon-params "arrow-downward")]]
      [t-btn start-earlier [mci-styled (icon-params "arrow-collapse-up")]]
      [t-btn start-later [mci-styled (icon-params "arrow-expand-down")]]
      [t-btn stop-earlier [mci-styled (icon-params "arrow-expand-up")]]
      [t-btn stop-later [mci-styled (icon-params "arrow-collapse-down")]]]]))

(defn now-indicator [{:keys [displayed-day
                             element-type
                             pixel-to-minute-ratio]}]
  (let [same-day    (helpers/same-day? displayed-day @now)
        y-pos       (-> @now
                        (helpers/get-ms)
                        (helpers/ms->minutes)
                        (* pixel-to-minute-ratio))
        height      25
        width       (-> height (* 2))
        line-height 2]

    (when (and same-day
               (= :period element-type))
      [view {:style {:flex     1
                     :position "absolute"
                     :top      (-> y-pos (- (-> height (/ 2))))
                     :left     (-> 60 ;; left on periods
                                   (- (-> width (/ 2))))
                     :width    "100%"
                     :height   height}}
       [view {:style {:position     "absolute"
                      :top          (-> height (/ 2)
                                        (- (-> line-height
                                               ;; idk why *2 ... it just looks right
                                               (* 2))))
                      :border-color (-> styles/theme :colors :primary)
                      :border-width (-> line-height (/ 2))
                      :width        "100%"
                      :height       0}}]
       [view {:style {:background-color (-> styles/theme :colors :primary)
                      :border-radius    height
                      :justify-content  "center"
                      :align-items      "center"
                      :width            width}}
        [text-paper {:style {:color (-> styles/theme :colors :accent-light)}}
         (format-time @now)]]])))

(defn fab []
  (let [action-style   {:background-color "white"
                        :width            40
                        :height           40
                        :justify-content  "center"
                        :align-items      "center"
                        :border-radius    20}
        action-element (fn [button-icon]
                         (fn [x]
                           (r/as-element
                            [view {:key   (:key (js->clj x :keywordize-keys true))
                                   :style action-style}
                             button-icon])))
        actions        (filter some? [{:render   (action-element [view {:style {:flex-direction "row"}}
                                                                  [en {:name "plus"}]
                                                                  [en {:name "air"}]])
                                       :name     "generate-pattern"
                                       :position 1}
                                      {:render   (action-element [en {:name "air"}])
                                       :name     "apply-pattern"
                                       :position 2}
                                      (when (some? @period-in-play)
                                        {:render   (action-element [mi {:name "stop"}])
                                         :name     "stop-playing"
                                         :position 3})
                                      {:render   (action-element [mi {:name "play-arrow"}])
                                       :name     "play"
                                       :position 4}
                                      (when (some? @selected-period)
                                        {:render   (action-element [mi {:name "play-circle-outline"}])
                                         :name     "play-from"
                                         :position 5})])]

    [:> fab/FloatingAction
     {:actions       (clj->js actions)
      :color         (if (some? @period-in-play)
                       (:color @period-in-play)
                       (-> styles/theme :colors :primary))
      :on-press-item (fn [action-name]
                       (println action-name)
                       (case action-name
                         "generate-pattern" (dispatch [:make-pattern-from-day
                                                       {:date    @displayed-day
                                                        :now     (js/Date.)
                                                        :planned false}])
                         "apply-pattern"    (reset! pattern-modal-visible true)
                         "stop-playing"     (dispatch [:stop-playing-period])
                         "play"             (reset! play-modal-visible true)
                         "play-from"        (dispatch [:play-from-period  {:id           (:id @selected-period)
                                                                           :time-started (js/Date.)
                                                                           :new-id       (random-uuid)}])
                         :else              (println "nothing matched")))}]))

(defn root
  "elements - {:actual [[collision-group-1] [collision-group-2]] :planned ... }"
  [{:keys [elements
           selected-element
           selected-element-edit
           in-play-element
           element-type
           element-transform-functions
           displayed-day
           edit-form
           move-element]}]
  (let [px-ratio-config       @(subscribe [:get-pixel-to-minute-ratio])
        pixel-to-minute-ratio (:current px-ratio-config)
        default-pxl-min-ratio (:default px-ratio-config)
        movement-selected     (some? selected-element)
        edit-selected         (some? selected-element-edit)
        pinch-ref             (.createRef react)
        double-tap-add-ref    (.createRef react)]

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
        :wait-for         double-tap-add-ref
        :on-gesture-event #(let [scale (helpers/get-gesture-handler-scale %)]
                             (dispatch-debounced
                              [:set-current-pixel-to-minute-ratio
                               (* pixel-to-minute-ratio scale)]))}
       [tap-gesture-handler
        {:ref            double-tap-add-ref
         :enabled        (not (or movement-selected
                                  edit-selected))
         :number-of-taps 2
         :on-handler-state-change
         #(let [state (get-state %)]
            (if (= :active state)
              (let [id      (random-uuid)
                    start   (-> %
                                (get-ys)
                                :y
                                (/ pixel-to-minute-ratio)
                                (helpers/minutes->ms)
                                (helpers/reset-relative-ms displayed-day)
                                (.valueOf))
                    stop    (+ start (helpers/minutes->ms 45))
                    now     (js/Date.)
                    x       (-> %
                                (get-xs)
                                :x)
                    planned (-> x
                                (< (-> (get-device-width)
                                       (/ 2))))]

                (if (= element-type :period)
                  (do
                    (close-bottom-sheet bottom-sheet-ref element-type)
                    (dispatch [:add-period {:period    {:id          id
                                                        :start       (js/Date. start)
                                                        :stop        (js/Date. stop)
                                                        :planned     planned
                                                        :data        {}
                                                        :last-edited now
                                                        :created     now
                                                        :label       ""}
                                            :bucket-id nil}]))))))}

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
                           :selected-element-edit selected-element-edit
                           :element-type          element-type
                           :in-play-element       in-play-element
                           :pixel-to-minute-ratio pixel-to-minute-ratio
                           :displayed-day         displayed-day}]]

          ;; now indicator
          [now-indicator {:pixel-to-minute-ratio pixel-to-minute-ratio
                          :displayed-day         displayed-day
                          :element-type          element-type}]]]]]

      ;; spacer for bottom sheet
      [view {:style {:height           500
                     :background-color (-> styles/theme :colors :background)}}]

      [portal
       [bottom-sheet {:ref           (fn [com]
                                       (reset! bottom-sheet-ref com))
                      :snap-points   [0 100 450]
                      :initial-snap  (if (some? selected-element-edit)
                                       1
                                       0)
                      ;; TODO figure out why this isn't being called
                      :on-close-end  #(do
                                        (println "closing")
                                        (dispatch [:select-element-edit {:element-type element-type
                                                                         :bucket-id    nil
                                                                         :element-id   nil}]))
                      :render-header #(r/as-element
                                       [surface
                                        [view {:style {:flex           1
                                                       :height         450
                                                       :width          "100%"
                                                       :flex-direction "column"
                                                       :align-items    "center"}}

                                         [transform-buttons
                                          {:transform-functions   element-transform-functions
                                           :selected-element-edit selected-element-edit}]

                                         [edit-form {:save-callback
                                                     (fn [_] (close-bottom-sheet bottom-sheet-ref element-type))
                                                     :delete-callback
                                                     (fn [_] (close-bottom-sheet bottom-sheet-ref element-type))}]]])}]]]]))
