(ns time-align-mobile.screens.day
  (:require [time-align-mobile.js-imports :refer [view
                                                  scroll-view
                                                  text
                                                  flat-list
                                                  format-date
                                                  format-time
                                                  touchable-highlight
                                                  status-bar
                                                  animated-view
                                                  mi
                                                  mci
                                                  fa
                                                  modal
                                                  animated-xy
                                                  pan-responder]]
            ["react-native-elements" :as rne]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers :refer [same-day?]]
            [time-align-mobile.components.list-items :as list-items]
            [time-align-mobile.styles :as styles]
            [goog.string :as gstring]
            ;; [zprint.core :refer [zprint]]
            ["react" :as react]
            [goog.string.format]
            [re-frame.core :refer [subscribe dispatch]]
            [time-align-mobile.helpers :as helpers]
            [time-align-mobile.components.day :refer [time-indicators render-period padding]]
            [reagent.core :as r]))

;; constants

(def play-modal-visible (r/atom false))

;; components
(defn top-bar [{:keys [top-bar-height dimensions displayed-day now]}]
  (let [outer-style              {:height           top-bar-height
                                  :width            (:width @dimensions)
                                  :background-color styles/background-color
                                  :elevation        2
                                  :flex-direction   "column"
                                  :justify-content  "center"
                                  :align-items      "center"}
        inner-style              {:flex-direction  "row"
                                  :align-items     "center"
                                  :justify-content "center"}
        displayed-day-style      {:justify-content "center"
                                  :align-items     "center"
                                  :width           "75%"}
        displayed-day-text-style (merge
                                  {:padding 6}
                                  (when (same-day? displayed-day now)
                                    {:border-color        "black"
                                     :border-radius       4
                                     :border-bottom-width 2
                                     ;; adjust padding for border
                                     :padding-bottom      4}))]

    [view {:style outer-style}
     [view {:style inner-style}
      ;; back
      [touchable-highlight
       {:on-press      #(dispatch [:update-day-time-navigator (helpers/back-n-days displayed-day 1)])
        :on-long-press #(dispatch [:update-day-time-navigator (helpers/back-n-days displayed-day 7)])}
       [mi {:name "fast-rewind"
            :size 32 }]]

      ;; displayed day
      [view {:style displayed-day-style}
       [touchable-highlight {:on-press #(dispatch [:update-day-time-navigator now])}
        [text {:style displayed-day-text-style} (.toDateString displayed-day)]]]

      ;; forward
      [touchable-highlight
       {:on-press      #(dispatch [:update-day-time-navigator (helpers/forward-n-days displayed-day 1)])
        :on-long-press #(dispatch [:update-day-time-navigator (helpers/forward-n-days displayed-day 7)])}
       [mi {:name "fast-forward"
            :size 32}]]]]))

(defn selection-menu-info [dimensions selected-period]
  (let [heading-style    {:background-color "#bfbfbf"}
        info-style       {}
        heading-sub-comp (fn [heading] [text {:style heading-style} heading])
        info-sub-comp    (fn [info] [text {:style info-style} info])]

    [scroll-view {:style {:background-color "white"
                          :width            "100%"
                          :padding-top      10
                          :padding-left     4
                          :max-height       "30%"}}

     [heading-sub-comp "uuid"]
     [info-sub-comp (:id selected-period)]

     [heading-sub-comp "label"]
     [info-sub-comp (:label selected-period)]

     [heading-sub-comp "bucket"]
     [info-sub-comp (:bucket-label selected-period)]

     [heading-sub-comp "start"]
     [info-sub-comp
      (format-date (:start selected-period))]

     [heading-sub-comp "stop"]
     [info-sub-comp
      (format-date (:stop selected-period))]

     [heading-sub-comp "data"]
     [info-sub-comp
      (str (:data selected-period))
      ;; (with-out-str
      ;;   (zprint (:data selected-period)
      ;;           {:map {:force-nl? true}}))
      ]]))

(defn selection-menu-button [label icon on-press long-press]
  [touchable-highlight {:on-press      on-press
                        :on-long-press long-press
                        :style         {:background-color "white"
                                        :border-radius    2
                                        :padding          8
                                        :margin           4
                                        :width            60
                                        :align-self       "flex-start"}}
   [view {:style {:flex-direction  "row"
                  :justify-content "center"
                  :align-items     "center"}}
    icon
    ;; [text label]
    ]])

(defn selection-menu-buttons [{:keys [dimensions selected-period period-in-play displayed-day]}]
  (let [row-style {:flex-direction  "row"
                   :justify-content "center"
                   :flex 1}]
    [view {:style {:background-color "#b9b9b9"
                   :width            "100%"
                   :padding-top      10
                   :padding-right    padding
                   :padding-left     padding
                   :height           "100%"
                   :flex-direction   "column"
                   :flex-wrap        "wrap"
                   :flex 1}}

     ;; cancel edit
     [view row-style
      [selection-menu-button
     "cancel"
       [mci {:name "backburger"}]
     #(dispatch [:select-period nil])]
      [selection-menu-button
     "edit"
       [mi {:name "edit"}]
       #(dispatch [:navigate-to {:current-screen :period
                                 :params         {:period-id (:id selected-period)}}])]]

     ;; start-later
     [view row-style
      [selection-menu-button
       "start later"
       [mci {:name "arrow-collapse-down"}]
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:start (-> selected-period
                                                           (:start)
                                                           (.valueOf)
                                                           (+ (* 5 60 1000))
                                                           (js/Date.))}}])
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:start (-> selected-period
                                                           (:start)
                                                           (.valueOf)
                                                           (+ (* 60 60 1000))
                                                           (js/Date.))}}])]]

     ;; start-earlier
     [view row-style
      [selection-menu-button
       "start earlier"
       [mci {:name "arrow-expand-up"}]
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:start (-> selected-period
                                                           (:start)
                                                           (.valueOf)
                                                           (- (* 5 60 1000))
                                                           (js/Date.))}}])
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:start (-> selected-period
                                                           (:start)
                                                           (.valueOf)
                                                           (- (* 60 60 1000))
                                                           (js/Date.))}}])]]

     ;; up
     [view row-style
      [selection-menu-button
       "up"
       [mi {:name "arrow-upward"}]
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:start (-> selected-period
                                                           (:start)
                                                           (.valueOf)
                                                           (- (* 5 60 1000)) ;; five minutes
                                                           (js/Date.))
                                                :stop  (-> selected-period
                                                           (:stop)
                                                           (.valueOf)
                                                           (- (* 5 60 1000))
                                                           (js/Date.))}}])
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:start (-> selected-period
                                                           (:start)
                                                           (.valueOf)
                                                           (- (* 60 60 1000)) ;; sixty minutes
                                                           (js/Date.))
                                                :stop  (-> selected-period
                                                           (:stop)
                                                           (.valueOf)
                                                           (- (* 60 60 1000))
                                                           (js/Date.))}}])]]

     ;; copy-previous-day copy-over copy-next-day
     [view row-style
      [selection-menu-button
       "copy previous day"
       [view {:flex-direction "row"}
        [mi {:name "content-copy"}]
        [mi {:name "arrow-back"}]]
       #(dispatch [:add-period {:period    (merge selected-period
                                                  {:start (-> selected-period
                                                              (:start)
                                                              (.valueOf)
                                                              (- (* 24 60 60 1000))
                                                              (js/Date.))
                                                   :stop  (-> selected-period
                                                              (:stop)
                                                              (.valueOf)
                                                              (- (* 24 60 60 1000))
                                                              (js/Date.))
                                                   :id    (random-uuid)})
                                :bucket-id (:bucket-id selected-period)}])]
      [selection-menu-button
       "copy over"
       [mi {:name "content-copy"}]
       #(dispatch [:add-period {:period    (merge selected-period
                                                  {:planned (not (:planned selected-period))
                                                   :id      (random-uuid)})
                                :bucket-id (:bucket-id selected-period)}])]
      [selection-menu-button
       "copy next day"
       [view {:flex-direction "row"}
        [mi {:name "arrow-forward"}]
        [mi {:name "content-copy"}]]
       #(dispatch [:add-period {:period    (merge selected-period
                                                  {:start (-> selected-period
                                                              (:start)
                                                              (.valueOf)
                                                              (+ (* 24 60 60 1000))
                                                              (js/Date.))
                                                   :stop  (-> selected-period
                                                              (:stop)
                                                              (.valueOf)
                                                              (+ (* 24 60 60 1000))
                                                              (js/Date.))
                                                   :id    (random-uuid)})
                                :bucket-id (:bucket-id selected-period)}])]]

     ;; play-from
     [view row-style
      [selection-menu-button
       "play from"
       [mi {:name "play-circle-outline"}]
       #(dispatch [:play-from-period  {:id           (:id selected-period)
                                       :time-started (js/Date.)
                                       :new-id       (random-uuid)}])]]

     ;; back-a-day forward-a-day
     [view row-style
      [selection-menu-button
       "back a day"
       [mi {:name "fast-rewind"}]
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:start (-> selected-period
                                                           (:start)
                                                           (.valueOf)
                                                           (- (* 24 60 60 1000))
                                                           (js/Date.))
                                                :stop  (-> selected-period
                                                           (:stop)
                                                           (.valueOf)
                                                           (- (* 24 60 60 1000))
                                                           (js/Date.))}}])]
      [selection-menu-button
       "forward a day"
       [mi {:name "fast-forward"}]
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:start (-> selected-period
                                                           (:start)
                                                           (.valueOf)
                                                           (+ (* 24 60 60 1000))
                                                           (js/Date.))
                                                :stop  (-> selected-period
                                                           (:stop)
                                                           (.valueOf)
                                                           (+ (* 24 60 60 1000))
                                                           (js/Date.))}}])]]

     ;; down
     [view row-style
      [selection-menu-button
       "down"
       [mi {:name "arrow-downward"}]
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:start (-> selected-period
                                                           (:start)
                                                           (.valueOf)
                                                           (+ (* 5 60 1000)) ;; five minutes
                                                           (js/Date.))
                                                :stop  (-> selected-period
                                                           (:stop)
                                                           (.valueOf)
                                                           (+ (* 5 60 1000))
                                                           (js/Date.))}}])
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:start (-> selected-period
                                                           (:start)
                                                           (.valueOf)
                                                           (+ (* 60 60 1000)) ;; sixty minutes
                                                           (js/Date.))
                                                :stop  (-> selected-period
                                                           (:stop)
                                                           (.valueOf)
                                                           (+ (* 60 60 1000))
                                                           (js/Date.))}}])]
      ]

     ;; stop-later
     [view row-style
      [selection-menu-button
       "stop later"
       [mci {:name "arrow-expand-down"}]
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:stop (-> selected-period
                                                          (:stop)
                                                          (.valueOf)
                                                          (+ (* 5 60 1000))
                                                          (js/Date.))}}])
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:stop (-> selected-period
                                                          (:stop)
                                                          (.valueOf)
                                                          (+ (* 60 60 1000))
                                                          (js/Date.))}}])]]

     ;; stop-earlier
     [view row-style
      [selection-menu-button
       "stop earlier"
       [mci {:name "arrow-collapse-up"}]
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:stop (-> selected-period
                                                          (:stop)
                                                          (.valueOf)
                                                          (- (* 5 60 1000))
                                                          (js/Date.))}}])
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:stop (-> selected-period
                                                          (:stop)
                                                          (.valueOf)
                                                          (- (* 60 60 1000))
                                                          (js/Date.))}}])]]

     ;; select-prev
     [view row-style
      [selection-menu-button
       "select prev"
       [mci {:name  "arrow-down-drop-circle"
             :style {:transform [{:rotate "180deg"}]}}]
       #(dispatch [:select-next-or-prev-period :prev])]]

     ;; select-playing
     (when (some? period-in-play)
       [view row-style
        [selection-menu-button
         "select playing"
         [mi {:name "play-circle-filled"}]
         #(dispatch [:select-period (:id period-in-play)])]])

     ;; select-next
     [view row-style
      [selection-menu-button
       "select next"
       [mci {:name "arrow-down-drop-circle"}]
       #(dispatch [:select-next-or-prev-period :next])]]

     ;; jump-to-selected
     (when (not (or (same-day? (:start selected-period) displayed-day)
                    (same-day? (:stop selected-period) displayed-day)))
       [view row-style
        [selection-menu-button
         "jump to selected"
         [fa {:name "dot-circle-o"}]
         #(dispatch [:update-day-time-navigator (:start selected-period)])]])]))

(defn selection-menu-arrow [dimensions selected-period displayed-day]
  (let [adjusted-start             (helpers/bound-start (:start selected-period) displayed-day)
        some-part-on-displayed-day (or (same-day? (:start selected-period) displayed-day)
                                       (same-day? (:stop selected-period) displayed-day))]
    (when some-part-on-displayed-day
      [view {:style {:position            "absolute"
                     :top                 (-> adjusted-start
                                              (helpers/date->y-pos (:height dimensions))
                                              (max 0)
                                              (min (:height dimensions))
                                              (- 5))
                     :left                (if (:planned selected-period)
                                            0
                                            (-> dimensions
                                                (:width)
                                                (/ 2)
                                                (- 7.5)))
                     :background-color    "transparent"
                     :border-style        "solid"
                     :border-left-width   10
                     :border-right-width  10
                     :border-bottom-width 15
                     :border-left-color   "transparent"
                     :border-right-color  "transparent"
                     :border-bottom-color "red"
                     :transform           (if (:planned selected-period)
                                            [{:rotate "270deg"}]
                                            [{:rotate "90deg"}])}}])))

(defn selection-menu [{:keys [dimensions selected-period displayed-day period-in-play]}]
  (let [width (-> dimensions
                  (:width)
                  (/ 2))]
    [view {:style {:position         "absolute"
                   :background-color "white"
                   :top              0
                   :height           (:height dimensions)
                   :width            width
                   :left             (-> dimensions
                                         (:width)
                                         (/ 2)
                                         (#(if (:planned selected-period) % 0)))}}

     [view {:style {:height           "85%"
                    :width            width
                    :background-color "grey"}}

      ;; [selection-menu-info dimensions selected-period]

      ;; buttons
      [selection-menu-buttons {:dimensions      dimensions
                               :selected-period selected-period
                               :displayed-day   displayed-day
                               :period-in-play  period-in-play}]]

     ;; [selection-menu-arrow dimensions selected-period displayed-day]

     ;; period info
     [view {:style {:padding 10}}
      [text (:label selected-period)]
      [text (:bucket-label selected-period)]
      [text (format-time (:start selected-period))]
      [text (format-time (:stop selected-period))]]]))


(defn now-indicator [{:keys [dimensions now]}]
  [view {:style (merge
                 styles/time-indicator-line-style
                 {:width            (:width @dimensions)
                  :background-color "black"
                  :align-items      "center"
                  :top              (-> @now
                                        (helpers/date->y-pos (:height @dimensions))
                                        (max 0)
                                        (min (:height @dimensions)))})}
   [text {:style (merge
                  styles/time-indicator-text-style
                  {:color "black"})}
    (format-time @now)]])

(defn make-period-from-touch [{:keys [displayed-day dimensions]}]
  (fn [evt]
    (let [now          (js/Date.)
          native-event (oget evt "nativeEvent")
          location-y   (oget native-event "locationY")
          location-x   (oget native-event "locationX")
          planned      (< location-x (-> @dimensions
                                         (:width)
                                         (/ 2)))
          relative-ms  (helpers/y-pos->ms location-y (:height @dimensions))
          start        (helpers/reset-relative-ms relative-ms @displayed-day)
          id           (random-uuid)]
      (dispatch [:add-period
                 {:bucket-id nil
                  :period    {:id          id
                              :start       start
                              :stop        (-> start
                                               (.valueOf)
                                               (+ (* 1000 60 60 1))
                                               (js/Date.))
                              :planned     planned
                              :created     now
                              :last-edited now
                              :label       ""
                              :data        {}}}])
      (dispatch [:navigate-to {:current-screen :period
                               :params         {:period-id id}}]))))

(defn floating-action-button [{:keys [bottom-bar-height
                                      period-in-play
                                      selected-period
                                      play-modal-visible]}]

  [view {:height           bottom-bar-height
         :width            "100%"
         :flex-direction   "row"
         :justify-content  "center"
         :align-items      "center"
         :background-color "grey"}

   (if (some? @period-in-play)
     [selection-menu-button
      "stop playing"
      [mi {:name "stop"}]
      #(dispatch [:stop-playing-period])]

     [selection-menu-button
      "play"
      [mi {:name "play-arrow"}]
      #(reset! play-modal-visible true)])

   (when (some? @selected-period)
     [selection-menu-button
      "play from"
      [mi {:name "play-circle-outline"}]
      #(dispatch [:play-from-period  {:id           (:id @selected-period)
                                      :time-started (js/Date.)
                                      :new-id       (random-uuid)}])])])

(defn periods-comp [{:keys [displayed-day
                            selected-period
                            period-in-play
                            periods
                            dimensions]}]
  [view
   (doall
    (->> @periods
         (#(concat (:actual %) (:planned %))) ;; TODO maybe refactor or find another way to do this?
         (map (fn [collision-group]
                (doall
                 (->> collision-group
                      (map-indexed
                       (fn [index period]
                         (render-period
                          {:period period
                           :collision-index index
                           :collision-group-size (count collision-group)
                           :displayed-day   @displayed-day
                           :dimensions      @dimensions
                           :selected-period @selected-period
                           :period-in-play  @period-in-play})))))))))])

(defn play-modal-content [{:keys [templates
                                  buckets]}]
  [view {:style {:flex    1
                         :padding 10}}
           [touchable-highlight {:on-press #(reset! play-modal-visible false)}
            [text "Cancel"]]
           [scroll-view {:style {:height "50%"}}
            [text "Select a bucket to make the period with"]
            [flat-list {:data @buckets
                        :render-item
                        (fn [i]
                          (let [item (:item (js->clj i :keywordize-keys true))]
                            (r/as-element
                             (list-items/bucket
                              (merge
                               item
                               {:on-press
                                (fn [_]
                                  (reset! play-modal-visible false)
                                  ;; passing dispatch the parent bucket id
                                  ;; for the period about to be created
                                  (dispatch [:play-from-bucket {:bucket-id (:id item)
                                                                :id        (random-uuid)
                                                                :now       (new js/Date)}]))})))))}]]

           [scroll-view {:style {:height "50%"}}
            [text "Or select a template"]
            [flat-list {:data @templates
                        :render-item
                        (fn [i]
                          (let [item (:item (js->clj i :keywordize-keys true))]
                            (r/as-element
                             (list-items/template
                              (merge
                               item
                               {:on-press
                                (fn [_]
                                  (reset! play-modal-visible false)
                                  ;; passing dispatch the parent bucket id
                                  ;; for the period about to be created
                                  (dispatch [:play-from-template {:template item
                                                                  :id       (random-uuid)
                                                                  :now      (js/Date.)}]))})))))}]]])

(defn root [params]
  (let [dimensions        (r/atom {:width nil :height nil})
        top-bar-height    styles/top-bar-height
        bottom-bar-height styles/bottom-bar-height
        periods           (subscribe [:get-collision-grouped-periods])
        displayed-day     (subscribe [:get-day-time-navigator])
        selected-period   (subscribe [:get-selected-period])
        period-in-play    (subscribe [:get-period-in-play])
        now               (subscribe [:get-now])
        buckets           (subscribe [:get-buckets])
        templates         (subscribe [:get-templates])]

    (r/create-class
     {:reagent-render
      (fn [params]
        [view {:style     {:flex 1
                           :justify-content "center" ;; child view pushes this up TODO change to flex-start
                           :align-items "center"}
               :on-layout (fn [event]
                            (let [layout (-> event
                                             (oget "nativeEvent" "layout")
                                             (js->clj :keywordize-keys true))]
                              (if (nil? (:height dimensions))
                                (reset! dimensions {:width  (:width layout)
                                                    :height (-
                                                             (:height layout)
                                                             top-bar-height
                                                             bottom-bar-height)}))))}

         ;; make our own status bar
         [status-bar {:hidden true}]
         [top-bar {:top-bar-height top-bar-height
                   :dimensions     dimensions
                   :displayed-day  @displayed-day
                   :now            @now}]

         ;; view that stretches to fill what is left of the screen
         [touchable-highlight
          {:on-long-press (make-period-from-touch {:displayed-day displayed-day
                                                   :dimensions    dimensions})}

          [view {:style {:height           (:height @dimensions)
                         ;; ^ height is already adjusted to account for top-bar
                         :width            (:width @dimensions)
                         :background-color "white"}}

           ;; time indicators
           [time-indicators @dimensions @displayed-day]

           ;; now indicator
           (when (same-day? @now @displayed-day)
             [now-indicator {:dimensions dimensions
                             :now now}])

           ;; periods
           [periods-comp {:displayed-day   displayed-day
                          :selected-period selected-period
                          :period-in-play  period-in-play
                          :periods         periods
                          :dimensions      dimensions}]

           ;; selection menu
           (when (some? @selected-period)
             [selection-menu {:dimensions      @dimensions
                              :selected-period @selected-period
                              :displayed-day   @displayed-day
                              :period-in-play  @period-in-play}])]]

         ;; floating action buttons
         [floating-action-button {:bottom-bar-height  bottom-bar-height
                                  :period-in-play     period-in-play
                                  :selected-period    selected-period
                                  :play-modal-visible play-modal-visible}]

         ;; play modal
         [modal {:animation-type   "slide"
                 :transparent      false
                 :on-request-close #(reset! play-modal-visible false)
                 :visible          @play-modal-visible}
          [play-modal-content {:templates templates
                               :buckets buckets}]]])})))

