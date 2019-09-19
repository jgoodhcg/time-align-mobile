(ns time-align-mobile.screens.day-planning
  (:require [time-align-mobile.js-imports :refer [view
                                                  scroll-view
                                                  text
                                                  flat-list
                                                  format-date
                                                  format-time
                                                  touchable-highlight
                                                  status-bar
                                                  format-date-day
                                                  animated-view
                                                  text-paper
                                                  subheading
                                                  surface
                                                  mi
                                                  mci
                                                  icon-button
                                                  fa
                                                  modal
                                                  animated-xy
                                                  pan-responder]]
            ["react-native-elements" :as rne]
            [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers :refer [same-day? xor]]
            [time-align-mobile.screens.period-form :refer [compact]]
            [time-align-mobile.components.list-items :as list-items]
            [time-align-mobile.styles :as styles :refer [theme]]
            [goog.string :as gstring]
            ;; [zprint.core :refer [zprint]]
            ["react" :as react]
            [goog.string.format]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [time-align-mobile.helpers :as helpers :refer [dispatch-debounced short-time long-time]]
            [time-align-mobile.components.day :as day-comp]
            [reagent.core :as r]))

(defn start-earlier
  ([selected-period]
   (start-earlier selected-period false))
  ([selected-period long]
   (let [time (if long
                long-time
                (* 1 60 1000))]
     (dispatch
       [:update-period
        {:period-id  (:id selected-period)
         :bucket-id  (:bucket-id selected-period)
         :update-map {:start (-> selected-period
                                 (:start)
                                 (.valueOf)
                                 (- time)
                                 (js/Date.))}}]))))

(defn start-later
  ([selected-period]
   (start-later selected-period false))
  ([selected-period long]
   (let [time (if long
                long-time
                short-time)]
     (dispatch
      [:update-period
       {:period-id  (:id selected-period)
        :bucket-id  (:bucket-id selected-period)
        :update-map {:start (-> selected-period
                                (:start)
                                (.valueOf)
                                (+ time)
                                (js/Date.))}}]))))

(defn down
  ([selected-period]
   (down selected-period false))
  ([selected-period long]
   (let [time (if long
                long-time
                short-time)]
     (dispatch
      [:update-period
       {:period-id  (:id selected-period)
        :bucket-id  (:bucket-id selected-period)
        :update-map {:start (-> selected-period
                                (:start)
                                (.valueOf)
                                (+ time)
                                (js/Date.))
                     :stop  (-> selected-period
                                (:stop)
                                (.valueOf)
                                (+ time)
                                (js/Date.))}}]))))

(defn up
  ([selected-period]
   (up selected-period false))
  ([selected-period long]
   (let [time (if long
                long-time
                short-time)]
     (dispatch
      [:update-period
        {:period-id  (:id selected-period)
         :bucket-id  (:bucket-id selected-period)
         :update-map {:start (-> selected-period
                                 (:start)
                                 (.valueOf)
                                 (- time)
                                 (js/Date.))
                      :stop  (-> selected-period
                                 (:stop)
                                 (.valueOf)
                                 (- time)
                                 (js/Date.))}}]))))

(defn stop-later
  ([selected-period]
   (stop-later selected-period false))
  ([selected-period long]
   (let [time (if long
                long-time
                short-time)]
     (dispatch
      [:update-period
       {:period-id  (:id selected-period)
        :bucket-id  (:bucket-id selected-period)
        :update-map {:stop (-> selected-period
                               (:stop)
                               (.valueOf)
                               (+ time)
                               (js/Date.))}}]))))

(defn stop-earlier
  ([selected-period]
   (stop-earlier selected-period false))
  ([selected-period long]
   (let [time (if long
                long-time
                short-time)]
     (dispatch
      [:update-period
       {:period-id  (:id selected-period)
        :bucket-id  (:bucket-id selected-period)
        :update-map {:stop (-> selected-period
                               (:stop)
                               (.valueOf)
                               (- time)
                               (js/Date.))}}]))))

(def period-transform-functions {:up            up
                                 :down          down
                                 :start-earlier start-earlier
                                 :start-later   start-later
                                 :stop-earlier  stop-earlier
                                 :stop-later    stop-later})

(defn top-bar [{:keys [displayed-day]}]
  [surface {:elevation 1
            :style     {:flex-direction  "row"
                        :justify-content "space-between"
                        :align-items     "center"
                        :padding         8}}
   [icon-button {:icon     "keyboard-arrow-left"
                 :size     20
                 :on-press #(dispatch
                             [:update-day-time-navigator
                              (helpers/back-n-days displayed-day 1)])}]

   [subheading (format-date-day displayed-day)]

   [icon-button {:icon     "keyboard-arrow-right"
                 :size     20
                 :on-press #(dispatch
                             [:update-day-time-navigator
                              (helpers/forward-n-days displayed-day 1)])}]])

(defn move-period [{:keys [selected-element start-relative-min]}]
  (let [new-start-ms (-> start-relative-min
                         (helpers/minutes->ms))
        new-start    (helpers/reset-relative-ms
                      new-start-ms
                      (:start selected-element))
        duration     (- (-> selected-element
                            :stop
                            (.valueOf))
                        (-> selected-element
                            :start
                            (.valueOf)))
        new-stop     (helpers/reset-relative-ms
                      (-> new-start-ms
                          (+ duration))
                      (:stop selected-element))]
    (dispatch-debounced [:update-period {:period-id  (:id selected-element)
                                         :bucket-id  (:bucket-id selected-element)
                                         :update-map {:start new-start
                                                      :stop  new-stop}}])))

(defn root [params]
  (let [dimensions           (r/atom {:width nil :height nil})
        top-bar-height       styles/top-bar-height
        bottom-bar-height    0 ;; styles/bottom-bar-height
        periods              (subscribe [:get-collision-grouped-periods])
        displayed-day        (subscribe [:get-day-time-navigator])
        selected-period      (subscribe [:get-selection-period-movement])
        selected-period-edit (subscribe [:get-selection-period-edit])
        period-in-play       (subscribe [:get-period-in-play])
        now                  (subscribe [:get-now])
        buckets              (subscribe [:get-buckets])
        patterns             (subscribe [:get-patterns])
        templates            (subscribe [:get-templates])
        time-alignment-fn    #(cond (nil? %)           :center
                                    (:planned %)       :left
                                    (not (:planned %)) :right)]
    [view {:style {:flex 1}}
     [status-bar {:hidden true}]
     [top-bar {:displayed-day @displayed-day}]
     [day-comp/root {:selected-element            @selected-period
                     :in-play-element             @period-in-play
                     :displayed-day               @displayed-day
                     :selected-element-edit       @selected-period-edit
                     :element-type                :period
                     :elements                    @periods
                     :patterns                    patterns
                     :buckets                     buckets
                     :templates                   templates
                     :element-transform-functions period-transform-functions
                     :move-element                move-period
                     :edit-form                   compact}]]))

