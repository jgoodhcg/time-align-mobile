(ns time-align-mobile.components.day
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  fa
                                                  mci
                                                  mi
                                                  en
                                                  scroll-view
                                                  format-date
                                                  format-time
                                                  status-bar
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]
            [time-align-mobile.styles :as styles :refer [styled-icon-factory]]
            ["react-native-floating-action" :as fab]
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

        selected       (= id (:id selected-period))
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
                           (max 1))

        button-height-default 40
        button-height-top     (min (- (:height dimensions)
                                      button-height-default)
                                   (max top button-height-default))
        button-height-bottom  (max (- (:height dimensions)
                                      (+ top height))
                                   button-height-default)

        button-width (/ width 3)
        base-style   {:position      "absolute"
                      :border-radius 2}
        period-style (merge base-style {:top              top
                                        :left             left
                                        :width            width
                                        :height           height
                                        :background-color color})
        button-style (merge base-style {:justify-content  "center"
                                        :align-items      "center"
                                        :width            button-width
                                        :opacity          0.5
                                        :background-color "grey"})
        top-style    (merge button-style {:height button-height-top
                                          :top    0})
        bottom-style (merge button-style {:height button-height-bottom
                                          :top    (min (- (:height dimensions)
                                                          button-height-default)
                                                       (max (+ top height)
                                                            button-height-default))
                                          })
        icon-style   {:color styles/text-light}
        mci-styled   (styled-icon-factory mci icon-style)
        mi-styled    (styled-icon-factory mi icon-style)
        icon-params  (fn [name] {:size 32 :name name})]

    [view {:key id}
     ;; Period itself
     [touchable-highlight
      [view {:style period-style}]]

     ;; Top buttons
     [:<>
      [view {:style (merge top-style {:left left})}
       [mi-styled (icon-params "arrow-upward")]]
      [view {:style (merge top-style {:left (+ left button-width)})}
       [mci-styled (icon-params "arrow-collapse-up")]]
      [view {:style (merge top-style {:left (+ left (* 2 button-width))})}
       [mci-styled (icon-params "arrow-collapse-down")]]]

     ;; Buttom buttons
     [:<>
      [view {:style (merge bottom-style {:left left})}
       [mi-styled (icon-params "arrow-upward")]]
      [view {:style (merge bottom-style {:left (+ left button-width)})}
       [mci-styled (icon-params "arrow-expand-down")]]
      [view {:style (merge bottom-style {:left (+ left (* 2 button-width))})}
       [mci-styled (icon-params "arrow-expand-up")]]]]))

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
  [:> rne/Button {:on-press      on-press
                  :on-long-press long-press
                  :icon          (r/as-element icon)
                  :button-style  {:background-color "white"
                                  :width            60
                                  :margin 2
                                  :height           "90%"}}])

(def selection-menu-button-row-style {:flex-direction  "row"
                                      :width           "100%"
                                      :justify-content "center"
                                      :flex            1})

(def selection-menu-button-container-style {:width           "100%"
                                            :padding-top     5
                                            :padding-right   5
                                            :padding-left    5
                                            :flex-direction  "column"
                                            :flex-wrap       "nowrap"
                                            :flex            1})

(defn selection-menu-buttons [{:keys [dimensions
                                      selected-period
                                      period-in-play
                                      displayed-day]}]

  (let [row-style           {:style selection-menu-button-row-style}
        icon-style          {:color styles/background-color-dark}
        mci-styled          (styled-icon-factory mci icon-style)
        fa-styled           (styled-icon-factory fa icon-style)
        mi-styled           (styled-icon-factory mi icon-style)
        en-styled           (styled-icon-factory en icon-style)]
    [view {:style selection-menu-button-container-style}

     ;; cancel edit
     [view row-style
      [selection-menu-button
     "cancel"
       [mci-styled {:name "backburger"}]
     #(dispatch [:select-period nil])]
      [selection-menu-button
     "edit"
       [mi-styled {:name "edit"}]
       #(dispatch [:navigate-to {:current-screen :period
                                 :params         {:period-id (:id selected-period)}}])]]

     ;; start-later
     [view row-style
      [selection-menu-button
       "start later"
       [mci-styled {:name "arrow-collapse-down"}]
       ;; TODO stop from moving beyond stop
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
       [mci-styled {:name "arrow-expand-up"}]
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
       [mi-styled {:name "arrow-upward"}]
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:start (-> selected-period
                                                           (:start)
                                                           (.valueOf)
                                                           (- (* 5 60 1000)) ;; five mi-stylednutes
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
                                                           (- (* 3 60 60 1000)) ;; sixty mi-stylednutes
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
        [mi-styled {:name "content-copy"}]
        [mi-styled {:name "arrow-back"}]]
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
       [mi-styled {:name "content-copy"}]
       #(dispatch [:add-period {:period    (merge selected-period
                                                  {:planned (not (:planned selected-period))
                                                   :id      (random-uuid)})
                                :bucket-id (:bucket-id selected-period)}])]
      [selection-menu-button
       "copy next day"
       [view {:flex-direction "row"}
        [mi-styled {:name "arrow-forward"}]
        [mi-styled {:name "content-copy"}]]
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
       [mi-styled {:name "play-circle-outline"}]
       #(dispatch [:play-from-period  {:id           (:id selected-period)
                                       :time-started (js/Date.)
                                       :new-id       (random-uuid)}])]]

     ;; back-a-day forward-a-day
     [view row-style
      [selection-menu-button
       "back a day"
       [mi-styled {:name "fast-rewind"}]
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
       [mi-styled {:name "fast-forward"}]
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
       [mi-styled {:name "arrow-downward"}]
       #(dispatch [:update-period {:id         (:id selected-period)
                                   :update-map {:start (-> selected-period
                                                           (:start)
                                                           (.valueOf)
                                                           (+ (* 5 60 1000)) ;; five mi-stylednutes
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
                                                           (+ (* 3 60 60 1000)) ;; sixty mi-stylednutes
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
       [mci-styled {:name "arrow-expand-down"}]
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
       ;; TODO stop from moving before start
       "stop earlier"
       [mci-styled {:name "arrow-collapse-up"}]
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
       [mci-styled {:name  "arrow-down-drop-circle"
                    :style {:transform [{:rotate "180deg"}]}}]
       #(dispatch [:select-next-or-prev-period :prev])]]

     ;; select-playing
     (when (some? period-in-play)
       [view row-style
        [selection-menu-button
         "select playing"
         [mi-styled {:name "play-circle-filled"}]
         #(dispatch [:select-period (:id period-in-play)])]])

     ;; select-next
     [view row-style
      [selection-menu-button
       "select next"
       [mci-styled {:name "arrow-down-drop-circle"}]
       #(dispatch [:select-next-or-prev-period :next])]]

     ;; jump-to-selected
     (when (not (or (same-day? (:start selected-period) displayed-day)
                    (same-day? (:stop selected-period) displayed-day)))
       [view row-style
        [selection-menu-button
         "jump to selected"
         [fa-styled {:name "dot-circle-o"}]
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
  (let [width                   (-> dimensions
                                    (:width)
                                    (* 0.51))
        ;; TODO move this to helpers
        format-relative-or-date #(if (inst? %)
                                   (format-time %)
                                   (format-time
                                    (helpers/reset-relative-ms % (js/Date.))))
        start-formatted         (->> selected-period-or-template
                                     :start
                                     format-relative-or-date)
        stop-formatted          (->> selected-period-or-template
                                     :stop
                                     format-relative-or-date)
        info-text               (fn [txt]
                                  [text {:style {:color styles/text-light}}
                                   txt])]
    [view {:style {:position         "absolute"
                   :elevation        5
                   :background-color styles/background-color-dark
                   :top              0
                   :height           (:height dimensions)
                   :width            width
                   :left             (-> dimensions
                                         (:width)
                                         (* 0.49)
                                         (#(if (:planned selected-period-or-template)
                                             % 0)))}}

     [view {:style {:width            width
                    :height           "100%"
                    :background-color styles/background-color-dark}}

      ;; [selection-menu-info dimensions selected-period]

      buttons-comp]

     ;; [selection-menu-arrow dimensions selected-period displayed-day]

     ;; period info
     ;; [view {:style {:padding 10}}
     ;;  [info-text (:label selected-period-or-template)]
     ;;  [info-text (:bucket-label selected-period-or-template)]
     ;;  [info-text start-formatted]
     ;;  [info-text stop-formatted]]
     ]))

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
                                  pattern-modal-visible
                                  play-modal-visible]}]
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
        actions        (filter some? [{:render   (action-element [en {:name "air"}])
                                       :name     "apply-pattern"
                                       :position 1}
                                      (when (some? @period-in-play)
                                        {:render   (action-element [mi {:name "stop"}])
                                         :name     "stop-playing"
                                         :position 2})
                                      {:render   (action-element [mi {:name "play-arrow"}])
                                       :name     "play"
                                       :position 3}
                                      (when (some? @selected-period)
                                        {:render   (action-element [mi {:name "play-circle-outline"}])
                                         :name     "play-from"
                                         :position 4})])]

    [:> fab/FloatingAction
     {:actions       (clj->js actions)
      :color         styles/background-color-dark
      :on-press-item (fn [action-name]
                       (println action-name)
                       (case action-name
                         "apply-pattern" (reset! pattern-modal-visible true)
                         "stop-playing"  (dispatch [:stop-playing-period])
                         "play"          (reset! play-modal-visible true)
                         "play-from"     (dispatch [:play-from-period  {:id           (:id @selected-period)
                                                                         :time-started (js/Date.)
                                                                         :new-id       (random-uuid)}])
                         :else (println "nothing matched")))}]))

(defn bottom-bar  [_ buttons]
  buttons)

(defn get-touch-info-from-event [{:keys [evt
                                         dimensions
                                         displayed-day]}]
  (let [native-event (oget evt "nativeEvent")
        location-y   (oget native-event "locationY")
        location-x   (oget native-event "locationX")
        relative-ms  (helpers/y-pos->ms location-y (:height dimensions))
        start        (helpers/reset-relative-ms relative-ms displayed-day)]

    {:native-event native-event
     :location-y location-y
     :location-x location-x
     :relative-ms relative-ms
     :start start}))
