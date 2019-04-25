(ns time-align-mobile.screens.pattern-planning
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  status-bar
                                                  touchable-highlight]]
            [time-align-mobile.styles :as styles]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers :as helpers]
            [re-frame.core :refer [subscribe dispatch]]
            [time-align-mobile.components.day :refer [time-indicators
                                                      render-period
                                                      padding]]
            [reagent.core :as r]))

(defn relative-time-to-date-obj [{:keys [hour minute]} date]
  (js/Date. (.getFullYear date)
            (.getMonth date)
            (.getDate date)
            hour
            minute))

(defn templates-comp [{:keys [templates dimensions]}]
  [view
   (doall
    (->> templates
         (map (fn [collision-group]
                (doall
                 (->> collision-group
                      (map-indexed
                       (fn [index {:keys [start stop] :as template}]
                         (let [now (js/Date.)]
                           (render-period
                            {:period (merge template  ;; TODO refactor :period key?
                                            {:start   (relative-time-to-date-obj
                                                       start now)
                                             :stop    (relative-time-to-date-obj
                                                       stop now)
                                             :planned true})

                             :collision-index      index
                             :collision-group-size (count collision-group)
                             :displayed-day        now
                             :dimensions           dimensions
                             :selected-period      nil
                             :period-in-play       nil}))))))))))])

(defn root []
  (let [pattern-form      (subscribe [:get-pattern-form])
        top-bar-height    styles/top-bar-height
        bottom-bar-height styles/bottom-bar-height
        dimensions        (r/atom {:width nil :height nil})]

    (r/create-class
     {:reagent-render
      (fn [params]
        [view {:style {:flex            1
                       :justify-content "flex-start"
                       :align-items     "center"}

               :on-layout
               (fn [event]
                 (let [layout (-> event
                                  (oget "nativeEvent" "layout")
                                  (js->clj :keywordize-keys true))]
                   (if (nil? (:height dimensions))
                     (reset! dimensions {:width  (:width layout)
                                         :height (-
                                                  (:height layout)
                                                  top-bar-height
                                                  bottom-bar-height)}))))}
         ;; top bar stuff
         [status-bar {:hidden true}]
         [view {:style {:height           top-bar-height
                        :width            (:width @dimensions)
                        :background-color styles/background-color
                        :elevation        2
                        :flex-direction   "column"
                        :justify-content  "center"
                        :align-items      "center"}}
          [text "top bar here"]]

         ;; view that stretches to fill what is left of the screen
         [touchable-highlight
          {:on-long-press #(println "should make a template on this pattern")}

          [view {:style {:height           (:height @dimensions)
                         :width            (:width @dimensions)
                         :background-color "grey"}}

           [time-indicators @dimensions]
           [templates-comp {:templates  (->> @pattern-form
                                             :templates
                                             (sort-by #(helpers/relative-to-minutes
                                                        (:start %)))
                                             (helpers/get-collision-groups))
                            :dimensions @dimensions}]]]])})))
