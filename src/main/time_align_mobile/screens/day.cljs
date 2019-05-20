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
            [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]
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
            [time-align-mobile.components.day :refer [time-indicators
                                                      render-period
                                                      top-bar
                                                      bottom-bar
                                                      bottom-bar-buttons
                                                      padding
                                                      get-touch-info-from-event
                                                      selection-menu
                                                      selection-menu-button
                                                      selection-menu-buttons]]
            [reagent.core :as r]))

;; atoms
(def play-modal-visible (r/atom false))

(def pattern-modal-visible (r/atom false))

;; components

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
    (let [now (js/Date.)

          ;; touch stuff, separated for reuse
          {:keys [native-event
                  location-y
                  location-x
                  relative-ms
                  start]}
          (get-touch-info-from-event {:evt           evt
                                      :dimensions    @dimensions
                                      :displayed-day @displayed-day})

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
                          {:period                    period
                           :collision-index           index
                           :collision-group-size      (count collision-group)
                           :displayed-day             @displayed-day
                           :dimensions                @dimensions
                           :select-function-generator (fn [id]
                                                        #(dispatch [:select-period id]))
                           :selected-period           @selected-period
                           :period-in-play            @period-in-play})))))))))])

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

(defn pattern-modal-content [{:keys [patterns]}]
  [view {:style {:flex    1
                 :padding 10}}
   [touchable-highlight {:on-press #(reset! pattern-modal-visible false)}
            [text "Cancel"]]
   [scroll-view {:style {:height "50%"}}
    [text "Select a pattern to apply to today"]
    [flat-list {:data          @patterns
                :key-extractor (fn [x]
                                 (-> x
                                     (js->clj)
                                     (get "id")
                                     (str)))
                :render-item
                (fn [i]
                  (let [pattern (:item (js->clj i :keywordize-keys true))]
                    (r/as-element
                     (list-items/pattern
                      (merge
                       pattern
                       {:on-press
                        (fn [_]
                          (reset! pattern-modal-visible false)
                          (let [new-periods
                                (->> pattern
                                     (transform
                                      [:templates sp/ALL]
                                      (fn [template]
                                        (merge template
                                               {:id          (random-uuid)
                                                :planned     true
                                                :last-edited (js/Date.)}))))]

                            (dispatch [:apply-pattern-to-displayed-day
                                       {:pattern-id  (:id pattern)
                                        :new-periods new-periods}])))})))))}]]])

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
        patterns          (subscribe [:get-patterns])
        templates         (subscribe [:get-templates])]

    (r/create-class
     {:reagent-render
      (fn [params]
        [view {:style     {:flex            1
                           :justify-content "center" ;; child view pushes this up TODO change to flex-start
                           :align-items     "center"}
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
           [time-indicators
            @dimensions
            (cond (nil? @selected-period)           :center
                  (:planned @selected-period)       :left
                  (not (:planned @selected-period)) :right)
            @displayed-day]

           ;; now indicator
           (when (same-day? @now @displayed-day)
             [now-indicator {:dimensions dimensions
                             :now        now}])

           ;; periods
           [periods-comp {:displayed-day   displayed-day
                          :selected-period selected-period
                          :period-in-play  period-in-play
                          :periods         periods
                          :dimensions      dimensions}]

           ;; selection menu
           (when (some? @selected-period)
             [selection-menu {:dimensions                  @dimensions
                              :selected-period-or-template @selected-period}
              [selection-menu-buttons
               {:dimensions      @dimensions
                :selected-period @selected-period
                :displayed-day   @displayed-day
                :period-in-play  @period-in-play}]])]]

         [bottom-bar {:bottom-bar-height bottom-bar-height}
          [bottom-bar-buttons {:period-in-play        period-in-play
                               :selected-period       selected-period
                               :pattern-modal-visible pattern-modal-visible
                               :play-modal-visible    play-modal-visible}]]

         ;; play modal
         [modal {:animation-type   "slide"
                 :transparent      false
                 :on-request-close #(reset! play-modal-visible false)
                 :visible          @play-modal-visible}
          [play-modal-content {:templates templates
                               :buckets   buckets}]]

         ;; pattern modal
         [modal {:animation-type   "slide"
                 :transparent      false
                 :on-request-close #(reset! pattern-modal-visible false)
                 :visible          @pattern-modal-visible}
          [pattern-modal-content {:patterns patterns}]]])})))

