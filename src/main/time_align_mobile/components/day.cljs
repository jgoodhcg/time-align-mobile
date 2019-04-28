(ns time-align-mobile.components.day
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  fa
                                                  mci
                                                  mi
                                                  scroll-view
                                                  format-date
                                                  format-time
                                                  status-bar
                                                  touchable-highlight]]
            [time-align-mobile.styles :as styles]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers :as helpers :refer [same-day?]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(def padding 20) ;; TODO refactor to styling

(defn time-indicators
  "alignment - :left :center :right"
  ([dimensions] (time-indicators dimensions :center (js/Date.)))
  ([dimensions alignment] (time-indicators dimensions alignment (js/Date.)))
  ([dimensions alignment displayed-day]
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
                                  (cond
                                    (= alignment :left)   {:padding-left 1
                                                           :align-self   "flex-start"}
                                    (= alignment :center) {}
                                    (= alignment :right)  {:padding-right 1
                                                           :align-self    "flex-end"}))]

    [view
     [view {:style (merge container-style
                          {:top (-> six-in-the-morning
                                    (helpers/date->y-pos (:height dimensions))
                                    (max 0)
                                    (min (:height dimensions)))})}

      [view {:style line-style}]
      [text {:style text-style} "06"]]

     [view {:style (merge container-style
                          {:top (-> twelve-in-the-afternoon
                                    (helpers/date->y-pos (:height dimensions))
                                    (max 0)
                                    (min (:height dimensions)))})}

      [view line-style]
      [text {:style text-style} "12"]]

     [view {:style (merge container-style
                          {:top (-> six-in-the-evening
                                    (helpers/date->y-pos (:height dimensions))
                                    (max 0)
                                    (min (:height dimensions)))})}
      [view line-style]
      [text {:style text-style} "18"]]])))

(defn render-period [{:keys [period
                             select-function-generator
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
                            :on-press (select-function-generator id)}
       [view
        ;; [text label]
        ;; [text bucket-label]
        ]]]]))

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

(def selection-menu-button-row-style {:flex-direction   "row"
                                      :justify-content "center"
                                      :flex            1})

(def selection-menu-button-container-style {:background-color "#b9b9b9"
                                             :width            "100%"
                                             :padding-top      10
                                             :padding-right    padding
                                             :padding-left     padding
                                             :height           "100%"
                                             :flex-direction   "column"
                                             :flex-wrap        "wrap"
                                             :flex             1})

(defn selection-menu-buttons [{:keys [dimensions
                                              selected-period
                                              period-in-play
                                              displayed-day]}]

  (let [row-style {:style selection-menu-button-row-style}]
    [view {:style selection-menu-button-container-style}

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
                                                           (+ (* 3 60 60 1000))
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
                                                           (- (* 3 60 60 1000))
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
                                                           (- (* 3 60 60 1000)) ;; sixty minutes
                                                           (js/Date.))
                                                :stop  (-> selected-period
                                                           (:stop)
                                                           (.valueOf)
                                                           (- (* 3 60 60 1000))
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
                                                           (+ (* 3 60 60 1000)) ;; sixty minutes
                                                           (js/Date.))
                                                :stop  (-> selected-period
                                                           (:stop)
                                                           (.valueOf)
                                                           (+ (* 3 60 60 1000))
                                                           (js/Date.))}}])]]

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
                                                          (+ (* 3 60 60 1000))
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
                                                          (- (* 3 60 60 1000))
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

(defn selection-menu [{:keys [dimensions
                              selected-period-or-template]}
                      buttons-comp]
  (let [width (-> dimensions
                  (:width)
                  (/ 2))
        ;; TODO move this to helpers
        format-relative-or-date #(if (inst? %)
                                  (format-time %)
                                  (str (if (< (:hour %) 10)
                                         ;; prepend the zero
                                         (str "0" (:hour %))
                                         (:hour %))
                                       "-"
                                       (if (< (:minute %) 10)
                                         ;; prepend the zero
                                         (str "0" (:minute %))
                                         (:minute %))))
        start-formatted (->> selected-period-or-template
                             :start
                             format-relative-or-date)
        stop-formatted (->> selected-period-or-template
                             :stop
                             format-relative-or-date)]
    [view {:style {:position         "absolute"
                   :background-color "white"
                   :top              0
                   :height           (:height dimensions)
                   :width            width
                   :left             (-> dimensions
                                         (:width)
                                         (/ 2)
                                         (#(if (:planned selected-period-or-template)
                                             % 0)))}}

     [view {:style {:height           "85%"
                    :width            width
                    :background-color "grey"}}

      ;; [selection-menu-info dimensions selected-period]

      buttons-comp]

     ;; [selection-menu-arrow dimensions selected-period displayed-day]

     ;; period info
     [view {:style {:padding 10}}
      [text (:label selected-period-or-template)]
      [text (:bucket-label selected-period-or-template)]
      [text start-formatted]
      [text stop-formatted]]]))

(defn top-bar-outer-style [top-bar-height dimensions]
  {:height           top-bar-height
   :width            (:width @dimensions)
   :background-color styles/background-color
   :elevation        2
   :flex-direction   "column"
   :justify-content  "center"
   :align-items      "center"})

(def top-bar-inner-style {:flex-direction  "row"
                          :align-items     "center"
                          :justify-content "center"})

(defn top-bar [{:keys [top-bar-height dimensions displayed-day now]}]
  (let [outer-style              (top-bar-outer-style top-bar-height dimensions)
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

(defn bottom-bar-buttons [{:keys [period-in-play
                                  selected-period
                                  play-modal-visible]}]
  [:<>
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

(defn bottom-bar  [{:keys [bottom-bar-height]} buttons]

  [view {:height           bottom-bar-height
         :width            "100%"
         :flex-direction   "row"
         :justify-content  "center"
         :align-items      "center"
         :background-color "grey"}

   buttons])
