(ns time-align-mobile.components.day
  (:require [time-align-mobile.js-imports
             :refer [view
                     text
                     text-paper
                     fa
                     dismiss-keyboard
                     mci
                     modal
                     mi
                     alert
                     en
                     button-paper
                     icon-button
                     scroll-view-gesture-handler
                     pan-gesture-handler
                     pinch-gesture-handler
                     bottom-sheet
                     touchable-ripple
                     tap-gesture-handler
                     long-press-gesture-handler
                     divider
                     format-date
                     touchable-ripple
                     get-device-width
                     flat-list
                     scroll-view
                     portal
                     modal-paper
                     status-bar
                     portal
                     rect-button
                     surface
                     fab-group
                     snackbar
                     touchable-without-feedback
                     color-light?
                     card
                     format-time
                     status-bar
                     list-subheader
                     list-section
                     touchable-highlight]]
            ["react-native-elements" :as rne]
            ["react" :as react]
            ["react-native-floating-action" :as fab]
            [time-align-mobile.components.list-items :as list-items]
            [time-align-mobile.styles :as styles :refer [styled-icon-factory
                                                         modal-style]]
            [time-align-mobile.screens.period-form :as period-form]
            [time-align-mobile.components.form-fields :refer [bucket-modal]]
            [time-align-mobile.screens.template-form :as template-form]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers
             :as helpers
             :refer [same-day?
                     get-gesture-handler-state
                     dispatch-debounced
                     dispatch-throttled
                     element-time-stamp-info
                     get-gesture-handler-ys
                     get-gesture-handler-xs]
             :rename {get-gesture-handler-state get-state
                      get-gesture-handler-ys get-ys
                      get-gesture-handler-xs get-xs}]
            [time-align-mobile.components.form-fields
             :refer [bucket-selection-content]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(def now (r/atom (js/Date.)))

;; start ticking
(js/setInterval #(do (reset! now (js/Date.))
                     (dispatch-throttled [:tick (js/Date.)])) 1000)

(def pan-offset (r/atom 0))

(def play-modal-visible (r/atom {:visible false}))

(def pattern-modal-visible (r/atom false)) ;; TODO change to {:visible bool}

(def spacer-height (r/atom 0))

(def bottom-sheet-ref (r/atom nil))

(def long-press-bucket-picker (r/atom {:visible false}))

(def long-press-indicator (r/atom {:visible false
                                   :y-pos   nil
                                   :planned nil
                                   :start   nil
                                   :stop    nil}))

(def scroll-ref (atom nil))

(defn scroll-to ;; TODO use this from a reg-fx by requiring it as a dep in handlers
  ([y-pos]
   (scroll-to @scroll-ref y-pos))
  ([scroll-ref y-pos]
   (ocall scroll-ref "scrollTo"
          (clj->js {:y (max 0 (- y-pos 50))}))))

(defn snap-bottom-sheet
  ([snap]
   (snap-bottom-sheet bottom-sheet-ref snap))
  ([bottom-sheet-ref snap]
   (let [bsr @bottom-sheet-ref]
     ;; bsr might not be set yet
     (try
       (ocall bsr "snapTo" snap)
       (catch js/Error e
         (do
           ;; no op for any other screen
           (println e)))))))

(defn close-bottom-sheet-side-effects [element-type & _]
  (reset! spacer-height 0)
  (dismiss-keyboard)
  (dispatch [:select-element-edit {:element-type element-type
                                   :bucket-id    nil
                                   :element-id   nil}]))

(defn close-bottom-sheet [bottom-sheet-ref element-type]
  (snap-bottom-sheet bottom-sheet-ref 0)
  (close-bottom-sheet-side-effects element-type))

(defn render-collision-group [{:keys [pixel-to-minute-ratio
                                      displayed-day
                                      element-type
                                      selected-element
                                      selected-element-edit
                                      collision-group]}]

  (->> collision-group
       (map-indexed
        (fn [index element]
          (let [start-info              (element-time-stamp-info
                                         (:start element)
                                         pixel-to-minute-ratio
                                         displayed-day)
                stop-info               (element-time-stamp-info
                                         (:stop element)
                                         pixel-to-minute-ratio
                                         displayed-day)
                start-min               (:min start-info)
                start-y-pos             (:y-pos start-info)
                stop-min                (:min stop-info)
                height                  (max
                                         (* pixel-to-minute-ratio (- stop-min start-min))
                                         2)
                index-offset            (min 90
                                             (-> index
                                                 (* 16)
                                                 (+ 2)))
                light                   (color-light? (:color element))
                selected                (= (:id element) (:id selected-element))
                selected-edit           (= (:id element) (:id selected-element-edit))
                something-else-selected (and (some? selected-element-edit)
                                             (not selected-edit))
                start-before-today      (not (same-day? (:start element)
                                                        displayed-day))
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
                                     :border-radius    4
                                     :background-color (-> styles/theme :colors :background)
                                     :overflow         "hidden"}
                                    (when selected
                                      {:elevation    32
                                       :border-width 4.5
                                       :border-color (-> styles/theme :colors :text)
                                       :border-style "dotted"})
                                    (when selected-edit
                                      {:elevation 32}))}
             [rect-button
              {:enabled                 (and (not selected)
                                             (not something-else-selected))
               :wait-for                double-tap-ref
               :on-handler-state-change #(if (= :active (get-state %))
                                           (if (not selected-edit)
                                             ;; select this element
                                             (do
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
                 :flex             1
                 :justify-content  "flex-start"
                 :overflow         "hidden"
                 :background-color (:color element)
                 :padding          4}
                (when something-else-selected
                  {:opacity 0.3})
                (when start-before-today
                  {:justify-content "flex-end"}))}
              [text {:style {:color (if light
                                      (-> styles/theme :colors :element-text-dark)
                                      (-> styles/theme :colors :element-text-light))}}
               (:label element)]]])))))

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

(defn selection-button [selected-element-edit s-function icon]
  [touchable-ripple {:on-press      #(s-function selected-element-edit)
                     :on-long-press #(s-function selected-element-edit true)
                     :style         {:padding 2}}
   icon])

(defn selection-buttons [{:keys [transform-functions
                                 height
                                 in-play-element
                                 element-type
                                 selected-element-edit]}]
  (let [{:keys [up
                down
                start-earlier
                stop-earlier
                stop-later
                start-later]} transform-functions

        icon-style          {:color (-> styles/theme :colors :text)}
        mci-styled          (styled-icon-factory mci icon-style)
        mi-styled           (styled-icon-factory mi icon-style)
        icon-params         (fn [n] {:size 28 :name n})
        t-btn               (partial selection-button selected-element-edit)
        vertical-pair-style {:flex-direction "column"}

        select-prev  (fn [& _]
                       (println "dafuq")
                       (case element-type
                         :period
                         (dispatch [:select-next-or-prev-period :prev])
                         :template
                         (dispatch [:select-next-or-prev-template-in-form :prev])
                         :default))
        select-next  (fn [& _]
                       (println "dafuqqqqq")
                       (case element-type
                         :period
                         (dispatch [:select-next-or-prev-period :next])
                         :template
                         (dispatch [:select-next-or-prev-template-in-form :next])
                         :default))
        play-from    (fn []
                       (dispatch
                        [:play-from-period
                         {:id           (:id selected-element-edit)
                          :time-started (js/Date.)
                          :new-id       (random-uuid)}]))
        stop-playing (fn []
                       (dispatch
                        [:stop-playing-period]))]

    [view {:style {:flex            0
                   :width           "100%"
                   :height          height
                   :flex-direction  "row"
                   :align-items     "center"
                   :justify-content "space-around"}}

     [view {:style vertical-pair-style}
      [t-btn select-prev [mi-styled (icon-params "keyboard-arrow-up")]]
      [t-btn select-next [mi-styled (icon-params "keyboard-arrow-down")]]]

     [view {:style vertical-pair-style}
      [t-btn up [mi-styled (icon-params "arrow-upward")]]
      [t-btn down [mi-styled (icon-params "arrow-downward")]]]

     [view {:style vertical-pair-style}
      [t-btn start-earlier [mci-styled (icon-params "arrow-collapse-up")]]
      [t-btn start-later [mci-styled (icon-params "arrow-expand-down")]]]

     [view {:style vertical-pair-style}
      [t-btn stop-earlier [mci-styled (icon-params "arrow-expand-up")]]
      [t-btn stop-later [mci-styled (icon-params "arrow-collapse-down")]]]

     (when (= :period element-type)
       (if (not (some? in-play-element))
         [t-btn play-from [mi-styled (icon-params "play-circle-outline")]]
         [t-btn stop-playing [mi-styled (icon-params "stop")]]))]))

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
      [view {:style {:flex            1
                     :flex-direction  "row"
                     :position        "absolute"
                     :top             (-> y-pos (- (-> height (/ 2))))
                     :left            (-> 60 ;; left on periods
                                          (- (-> width (/ 2))))
                     :width           "100%"
                     :height          height
                     :justify-content "flex-start"
                     :align-items "center"}}

       ;; time label
       [view {:style {:background-color (-> styles/theme :colors :primary)
                      :border-radius    height
                      :justify-content  "center"
                      :align-items      "center"
                      :width            width}}
        [text-paper {:style {:color (-> styles/theme :colors :element-text-dark)}}
         (format-time @now)]]

       ;; line
       [view {:style {:border-color (-> styles/theme :colors :primary)
                      :border-width (-> line-height (/ 2))
                      :width        "100%"
                      :height       0}}]])))

(defn pattern-modal-content [{:keys [patterns]}]
  [surface {:style modal-style}

   [icon-button
    {:icon     "close"
     :size     20
     :on-press #(reset! pattern-modal-visible false)}]

   [scroll-view
    [list-subheader "What is your day going to look like?"]
     [list-section
      (->> @patterns
           (map (fn [pattern]
                  (list-items/pattern
                   (merge
                    pattern
                    {:on-press
                     (fn [_]
                       (reset! pattern-modal-visible false)
                       (let [new-periods
                             (->> pattern
                                  :templates
                                  (map (fn [template]
                                         (merge template
                                                {:id          (random-uuid)
                                                 :created     (js/Date.)
                                                 :last-edited (js/Date.)}))))]

                         (dispatch [:apply-pattern-to-displayed-day
                                    {:pattern-id  (:id pattern)
                                     :new-periods new-periods}])))})))))]]])

(defn fab-comp [{:keys [displayed-day in-play-element selected-element]}]
  (let [fab-state (subscribe [:get-day-fab-open])
        actions   (filter some? [{:icon    "expand-all-outline"
                                  :label   "create template"
                                  :onPress #(dispatch [:make-pattern-from-day
                                                       {:date displayed-day
                                                        :now  (js/Date.)}])}
                                 {:icon    "expand-all"
                                  :label   "apply template"
                                  :onPress #(reset! pattern-modal-visible true)}
                                 (if (some? in-play-element)
                                   {:icon    "stop"
                                    :label   "stop"
                                    :onPress #(dispatch [:stop-playing-period])}
                                   {:icon    "play"
                                    :label   "start"
                                    :onPress #(reset! play-modal-visible {:visible true})})])]

    [fab-group (merge {:open            @fab-state
                       :icon            "plus"
                       :actions         (clj->js actions) ;; TODO kebab case conversion
                       :on-state-change #(dispatch [:set-day-fab-open (oget % "open")])}
                      (when (some? in-play-element)
                        {:color (:color in-play-element)}))]))

(defn long-press-add-generator [{:keys [displayed-day
                                        pixel-to-minute-ratio]}]
  (fn [event]
    (let [state   (get-state event)
          ys      (get-ys event)
          start   (-> ys
                      :y
                      (/ pixel-to-minute-ratio)
                      (helpers/minutes->ms)
                      (helpers/reset-relative-ms displayed-day)
                      (.valueOf))
          stop    (+ start (helpers/minutes->ms 45))
          x       (-> event
                      (get-xs)
                      :x)
          planned (-> x
                      (< (-> (get-device-width)
                             (/ 2))))]
      (case state
        :began         (do
                         (reset! long-press-indicator {:visible true
                                                       :planned planned
                                                       :y-pos   (:y ys)
                                                       :start   start
                                                       :stop    stop}))
        :active        (let [id  (random-uuid)
                             now (js/Date.)]
                         (reset! long-press-indicator {:visible false
                                                       :planned nil
                                                       :y-pos   nil
                                                       :start   nil
                                                       :stop    nil})
                         (reset! long-press-bucket-picker {:start     start
                                                           :stop      stop
                                                           :id        id
                                                           :timestamp now
                                                           :planned   planned
                                                           :visible   true}))
        (:failed :end) (do
                         (reset! long-press-indicator {:visible false
                                                       :planned nil
                                                       :y-pos   nil
                                                       :start   nil
                                                       :stop    nil}))
        :default))))

(defn root
  "elements - {:actual [[collision-group-1] [collision-group-2]] :planned ... }"
  [{:keys [elements
           selected-element
           selected-element-edit
           in-play-element
           element-type
           patterns
           buckets
           element-transform-functions
           displayed-day
           templates
           edit-form
           move-element]}]
  (let [px-ratio-config       @(subscribe [:get-pixel-to-minute-ratio])
        fab-visible           @(subscribe [:get-day-fab-visible])
        lpi                   @long-press-indicator
        pixel-to-minute-ratio (:current px-ratio-config)
        default-pxl-min-ratio (:default px-ratio-config)
        movement-selected     (some? selected-element)
        edit-selected         (some? selected-element-edit)
        nothing-selected      (and (not movement-selected)
                                   (not edit-selected))
        pinch-ref             (.createRef react)
        long-press-ref        (.createRef react)]

    [pan-gesture-handler
     {:enabled movement-selected ;; this is effectively disabled because there is no way to select for movement
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
           :default))}

     [view {:style {:flex 1}}

      [scroll-view-gesture-handler
       {:scroll-enabled (not movement-selected)
        :ref            (fn [ref] (reset! scroll-ref ref))

        ;; This is a little jumpy and annoying when developing, if it is missed try enabling it
        ;; :on-layout      (fn [] (let [r        @scroll-ref
        ;;                              now      (js/Date.)
        ;;                              now-info (element-time-stamp-info
        ;;                                        now
        ;;                                        pixel-to-minute-ratio
        ;;                                        displayed-day)]
        ;;                          (if (same-day? displayed-day now)
        ;;                            (scroll-to r (:y-pos now-info)))))

        ;; this stops all touch events from going to children kind of ... I guess.
        ;; My observation is that is somehow only stops child gesture events but not their  state changes.
        :wait-for pinch-ref}

       [pinch-gesture-handler
        {:enabled          false ;; re-enable when it can decoupled from re-frame lifecycle
         :ref              pinch-ref
         :wait-for         long-press-ref
         :on-gesture-event #(let [scale (helpers/get-gesture-handler-scale %)]
                              (dispatch-debounced
                               [:set-current-pixel-to-minute-ratio
                                (* pixel-to-minute-ratio scale)]))}

        [long-press-gesture-handler
         {:ref             long-press-ref
          :enabled         (not (or movement-selected
                                    edit-selected))
          :min-duration-ms 1500
          :max-dist        20
          :on-handler-state-change
          (long-press-add-generator
           {:displayed-day         displayed-day
            :pixel-to-minute-ratio pixel-to-minute-ratio})}

         [view
          {:style {:flex 1}}

          [view {:style {:height (* helpers/day-min pixel-to-minute-ratio)
                         :flex   1}}

           ;; time indicators
           (for [hour (range 1 helpers/day-hour)]
             (let [rel-min     (* 60 hour)
                   y-pos       (* pixel-to-minute-ratio rel-min)
                   rel-ms      (helpers/hours->ms hour)
                   time-str    (helpers/ms->h rel-ms)
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
                                      :color               (-> styles/theme :colors :primary)
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

           ;; long press indicator
           (when false ;;(:visible lpi) ;; re-enable some day
             (let [y-pos   (:y-pos lpi)
                   planned (:planned lpi)]

               [surface {:style {:position         "absolute"
                                 :left             (if planned 70 "60%")
                                 :width            100
                                 :opacity          0.2
                                 :top              y-pos
                                 :height           20
                                 :border-radius    8
                                 :background-color (-> styles/theme :colors :accent)}}]))

           ;; now indicator
           [now-indicator {:pixel-to-minute-ratio pixel-to-minute-ratio
                           :displayed-day         displayed-day
                           :element-type          element-type}]]]]]

       ;; fab
       (when (and nothing-selected
                  (= :period element-type)
                  fab-visible)
         [portal
          [fab-comp {:displayed-day    displayed-day
                     :in-play-element  in-play-element
                     :selected-element selected-element}]])

       ;; long press bucket selection modal
       [bucket-modal
        buckets
        long-press-bucket-picker
        (fn [item] ;; item selection generator function
          (fn [_]
            (let [{:keys [start stop planned id timestamp]} @long-press-bucket-picker]
              (case element-type
                :period
                (do
                  (dispatch [:add-period-with-selection
                             {:period    {:id          id
                                          :start       (js/Date. start)
                                          :stop        (js/Date. stop)
                                          :planned     planned
                                          :data        {}
                                          :last-edited timestamp
                                          :created     timestamp
                                          :label       ""}
                              :bucket-id (:id item)}]))

                :template
                (do
                  (dispatch [:add-new-template-to-planning-form
                             {:id        id
                              :bucket-id (:id item)
                              :start     (js/Date. start)
                              :planned   planned
                              :now       timestamp}]))
                :default)
              (reset! long-press-bucket-picker
                      {:start   nil
                       :stop    nil
                       :id      nil
                       :planned nil
                       :visible false})
              (snap-bottom-sheet bottom-sheet-ref 1))))]

       ;; play modal
       [bucket-modal
        buckets
        play-modal-visible
        (fn [item]
          (fn [_]
            (let [now   (js/Date.)
                  y-pos (-> now
                            (element-time-stamp-info
                             pixel-to-minute-ratio
                             displayed-day)
                            :y-pos)]
              (reset! play-modal-visible {:visible false})
              (dispatch
               [:play-from-bucket
                {:bucket-id (:id item)
                 :id        (random-uuid)
                 :now       now}])
              (snap-bottom-sheet bottom-sheet-ref 1))))]

       ;; pattern modal
       [modal {:animation-type   "slide"
               :transparent      true
               :on-request-close #(reset! pattern-modal-visible false)
               :visible          @pattern-modal-visible}
        [pattern-modal-content {:patterns patterns}]]

       ;; spacer for bottom sheet
       [view {:style {:height           @spacer-height
                      :background-color (-> styles/theme :colors :background)}}]

       ;; bottom sheet
       [portal
        (let [drag-indicator-height       12
              drag-indicator-total-height 40
              time-buttons-height         150
              bottom-sheet-height         500]
          [bottom-sheet {:ref           (fn [com]
                                          (reset! bottom-sheet-ref com))
                         :snap-points   [0
                                         bottom-sheet-height]
                         :initial-snap  (if (some? selected-element-edit)
                                          1
                                          0)
                         :on-open-start #(reset! spacer-height bottom-sheet-height)
                         :on-open-end   #(do
                                           (scroll-to (-> selected-element-edit
                                                          :start
                                                          (element-time-stamp-info
                                                           pixel-to-minute-ratio
                                                           displayed-day)
                                                          :y-pos)))
                         :on-close-end  (partial close-bottom-sheet-side-effects element-type)
                         :render-header #(r/as-element
                                          [surface
                                           [view {:style {:height         bottom-sheet-height
                                                          :width          "100%"
                                                          :flex-direction "column"
                                                          :align-items    "center"}}

                                            [icon-button {:icon     "drag-horizontal"
                                                          :size     (-> drag-indicator-height
                                                                        (* 2))
                                                          :style    {:height drag-indicator-height}
                                                          :on-press (fn []
                                                                      (snap-bottom-sheet bottom-sheet-ref 2))}]

                                            [edit-form {:scroll-to scroll-to
                                                        :save-callback
                                                        (fn [_]
                                                          (dismiss-keyboard)
                                                          (dispatch [:tick (js/Date.)]))
                                                        :in-play-element
                                                        in-play-element
                                                        :play-callback
                                                        (fn [_] (close-bottom-sheet bottom-sheet-ref element-type))
                                                        :copy-over-callback
                                                        (fn [_] (close-bottom-sheet bottom-sheet-ref element-type))
                                                        :delete-callback
                                                        (fn [_] (close-bottom-sheet bottom-sheet-ref element-type))
                                                        :close-callback
                                                        (fn [_] (close-bottom-sheet bottom-sheet-ref element-type))}]]])}])]]]]))
