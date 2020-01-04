(ns time-align-mobile.screens.pattern-planning
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  mi
                                                  button-paper
                                                  menu-item
                                                  menu
                                                  icon-button
                                                  surface
                                                  subheading
                                                  text-paper
                                                  mci
                                                  status-bar
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]
            [time-align-mobile.styles :as styles]
            [time-align-mobile.components.top-bar :refer [top-bar]]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers :as helpers :refer [xor dispatch-debounced short-time long-time]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]
            [time-align-mobile.components.day :as day-comp]
            [time-align-mobile.screens.template-form :refer [compact]]
            [reagent.core :as r]))

(def top-bar-menu (r/atom {:visible false}))

(defn start-earlier
  ([selected-template]
   (start-earlier selected-template false))
  ([selected-template long]
   (let [time (if long
                long-time
                (* 1 60 1000))]
     (dispatch
       [:update-template-on-pattern-planning-form
        {:id (:id selected-template)
         :start (-> selected-template
                    (:start)
                    (- time))}]))))

(defn start-later
  ([selected-template]
   (start-later selected-template false))
  ([selected-template long]
   (let [time (if long
                long-time
                short-time)]
     (dispatch
      [:update-template-on-pattern-planning-form
       {:id (:id selected-template)
        :start (-> selected-template
                   (:start)
                   (+ time))}]))))

(defn down
  ([selected-template]
   (down selected-template false))
  ([selected-template long]
   (let [time (if long
                long-time
                short-time)]
     (dispatch
      [:update-template-on-pattern-planning-form
       {:id (:id selected-template)
        :start (-> selected-template
                   (:start)
                   (+ time))
        :stop  (-> selected-template
                   (:stop)
                   (+ time))}]))))

(defn up
  ([selected-template]
   (up selected-template false))
  ([selected-template long]
   (let [time (if long
                long-time
                short-time)]
     (dispatch
      [:update-template-on-pattern-planning-form
       {:id (:id selected-template)
        :start (-> selected-template
                   (:start)
                   (- time))
        :stop  (-> selected-template
                   (:stop)
                   (- time))}]))))

(defn stop-later
  ([selected-template]
   (stop-later selected-template false))
  ([selected-template long]
   (let [time (if long
                long-time
                short-time)]
     (dispatch
      [:update-template-on-pattern-planning-form
       {:id (:id selected-template)
        :stop (-> selected-template
                  (:stop)
                  (+ time))}]))))

(defn stop-earlier
  ([selected-template]
   (stop-earlier selected-template false))
  ([selected-template long]
   (let [time (if long
                long-time
                short-time)]
     (dispatch
      [:update-template-on-pattern-planning-form
       {:id (:id selected-template)
        :stop (-> selected-template
                  (:stop)
                  (- time))}]))))

(def template-transform-functions {:up          up
                                   :down          down
                                   :start-earlier start-earlier
                                   :start-later   start-later
                                   :stop-earlier  stop-earlier
                                   :stop-later    stop-later})

(defn move-template [{:keys [selected-element start-relative-min]}]
  (let [new-start (-> start-relative-min
                      (helpers/minutes->ms)
                      (int))
        duration  (- (-> selected-element
                         :stop)
                     (-> selected-element
                         :start))
        new-stop  (-> new-start
                      (+ duration)
                      (int))]
    (dispatch-debounced [:update-template-on-pattern-planning-form
                         {:id    (:id selected-element)
                          :start new-start
                          :stop  new-stop}])))

(defn top-bar-right-content [{:keys [no-changes]}]
  ;; TODO DRY this up (day_planning also has this code)
  [view {:style {:flex-direction  "row"
                 :justify-content "space-between"
                 :align-items     "center"}}
   [button-paper {:icon     "content-save"
                  :mode     "outlined"
                  :on-press #(dispatch [:save-pattern-form (new js/Date)])
                  :disabled no-changes}
    "Save"]
   [menu {:anchor
          (r/as-element
           [icon-button {:on-press #(swap!
                                     top-bar-menu
                                     (fn [m] (assoc-in m [:visible] true)))
                         :icon     "dots-vertical"}])
          :visible    (:visible @top-bar-menu)
          :on-dismiss #(swap! top-bar-menu
                              (fn [m] (assoc-in m [:visible] false)))}
    [menu-item {:title    "zoom in"
                :icon     "magnify-plus-outline"
                :on-press #(do
                             (dispatch [:zoom-in]))}]
    [menu-item {:title    "zoom out"
                :icon     "magnify-minus-outline"
                :on-press #(do
                             (dispatch [:zoom-out]))}]
    [menu-item {:title    "zoom reset"
                :icon     "magnify"
                :on-press #(do
                             (dispatch [:zoom-default])
                             (swap! top-bar-menu
                                    (fn [m] (assoc-in m [:visible] false))))}]]])

(defn root [params]
  (let [pattern-form           (subscribe [:get-pattern-form])
        pattern-form-changes   (subscribe [:get-pattern-form-changes])
        buckets                (subscribe [:get-buckets]) ;; this is just for selecting a random bucket for new template long press
        changes                (subscribe [:get-pattern-form-changes])
        selected-template      (subscribe [:get-selection-template-movement])
        selected-template-edit (subscribe [:get-selection-template-edit])
        top-bar-height         styles/top-bar-height
        dimensions             (r/atom {:width nil :height nil})]

    [view {:style {:flex 1}}
     [status-bar {:hidden true}]
     [top-bar
      {:center-content [subheading {:number-of-lines 1
                                    :ellipsize-mode  "tail"
                                    :style           {:max-width 150}}
                        (:label @pattern-form)]
       :right-content  [top-bar-right-content
                        {:no-changes (empty? @pattern-form-changes)}]}]

     [day-comp/root
      {:selected-element            @selected-template
       :selected-element-edit       @selected-template-edit
       :in-play-element             nil
       :displayed-day               (js/Date.)
       :element-type                :template
       :element-transform-functions template-transform-functions
       :buckets                     buckets
       :edit-form                   compact
       :move-element                move-template
       :elements
       {:actual  (->> @pattern-form
                      :templates
                      (remove :planned)
                      (sort-by :start)
                      (helpers/get-collision-groups))
        :planned (->> @pattern-form
                      :templates
                      (filter :planned)
                      (sort-by :start)
                      (helpers/get-collision-groups))}}]]))
