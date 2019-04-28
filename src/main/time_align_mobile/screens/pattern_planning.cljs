(ns time-align-mobile.screens.pattern-planning
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  mi
                                                  mci
                                                  status-bar
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]
            [time-align-mobile.styles :as styles]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers :as helpers]
            [re-frame.core :refer [subscribe dispatch]]
            [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]
            [time-align-mobile.components.day :refer [time-indicators
                                                      top-bar-outer-style
                                                      bottom-bar
                                                      render-period
                                                      selection-menu
                                                      selection-menu-button-row-style
                                                      selection-menu-button
                                                      selection-menu-button-container-style
                                                      selection-menu-button-container-style
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
                             :select-function-generator (fn [id]
                                                          #(dispatch [:select-template id]))
                             :period-in-play       nil}))))))))))])

(defn selection-menu-buttons [selected-template pattern-form]
  (let [row-style {:style selection-menu-button-row-style}]
    [view {:style selection-menu-button-container-style}
     ;; cancel edit
     [view row-style
      [selection-menu-button
       "cancel"
       [mci {:name "backburger"}]
       #(dispatch [:select-template nil])]
      [selection-menu-button
       "edit"
       [mi {:name "edit"}]
       #(dispatch [:navigate-to
                   {:current-screen :template
                    :params         {:template-id (:id selected-template)}}])]]

     ;; start-later
     [view row-style
      [selection-menu-button
       "start later"
       [mci {:name "arrow-collapse-down"}]
       (fn []
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :start :minute]
                                  #(+ 5 %)
                                  pattern-form)
                                 [:templates])]))
       (fn []
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :start :hour]
                                  #(+ 3 %)
                                  pattern-form)
                                 [:templates])]))]]

     ;; ;; start-earlier
     ;; [view row-style
     ;;  [selection-menu-button
     ;;   "start earlier"
     ;;   [mci {:name "arrow-expand-up"}]
     ;;   #(dispatch [:update-template {:id         (:id selected-template)
     ;;                                 :update-map {:start (-> selected-template
     ;;                                                         (:start)
     ;;                                                         (.valueOf)
     ;;                                                         (- (* 5 60 1000))
     ;;                                                         (js/Date.))}}])
     ;;   #(dispatch [:update-template {:id         (:id selected-template)
     ;;                                 :update-map {:start (-> selected-template
     ;;                                                         (:start)
     ;;                                                         (.valueOf)
     ;;                                                         (- (* 60 60 1000))
     ;;                                                         (js/Date.))}}])]]

     ;; ;; up
     ;; [view row-style
     ;;  [selection-menu-button
     ;;   "up"
     ;;   [mi {:name "arrow-upward"}]
     ;;   #(dispatch [:update-template {:id         (:id selected-template)
     ;;                                 :update-map {:start (-> selected-template
     ;;                                                         (:start)
     ;;                                                         (.valueOf)
     ;;                                                         (- (* 5 60 1000)) ;; five minutes
     ;;                                                         (js/Date.))
     ;;                                              :stop  (-> selected-template
     ;;                                                         (:stop)
     ;;                                                         (.valueOf)
     ;;                                                         (- (* 5 60 1000))
     ;;                                                         (js/Date.))}}])
     ;;   #(dispatch [:update-template {:id         (:id selected-template)
     ;;                                 :update-map {:start (-> selected-template
     ;;                                                         (:start)
     ;;                                                         (.valueOf)
     ;;                                                         (- (* 60 60 1000)) ;; sixty minutes
     ;;                                                         (js/Date.))
     ;;                                              :stop  (-> selected-template
     ;;                                                         (:stop)
     ;;                                                         (.valueOf)
     ;;                                                         (- (* 60 60 1000))
     ;;                                                         (js/Date.))}}])]]

     ;; ;; down
     ;; [view row-style
     ;;  [selection-menu-button
     ;;   "down"
     ;;   [mi {:name "arrow-downward"}]
     ;;   #(dispatch [:update-template {:id         (:id selected-template)
     ;;                                 :update-map {:start (-> selected-template
     ;;                                                         (:start)
     ;;                                                         (.valueOf)
     ;;                                                         (+ (* 5 60 1000)) ;; five minutes
     ;;                                                         (js/Date.))
     ;;                                              :stop  (-> selected-template
     ;;                                                         (:stop)
     ;;                                                         (.valueOf)
     ;;                                                         (+ (* 5 60 1000))
     ;;                                                         (js/Date.))}}])
     ;;   #(dispatch [:update-template {:id         (:id selected-template)
     ;;                                 :update-map {:start (-> selected-template
     ;;                                                         (:start)
     ;;                                                         (.valueOf)
     ;;                                                         (+ (* 60 60 1000)) ;; sixty minutes
     ;;                                                         (js/Date.))
     ;;                                              :stop  (-> selected-template
     ;;                                                         (:stop)
     ;;                                                         (.valueOf)
     ;;                                                         (+ (* 60 60 1000))
     ;;                                                         (js/Date.))}}])]]

     ;; ;; stop-later
     ;; [view row-style
     ;;  [selection-menu-button
     ;;   "stop later"
     ;;   [mci {:name "arrow-expand-down"}]
     ;;   #(dispatch [:update-template {:id         (:id selected-template)
     ;;                                 :update-map {:stop (-> selected-template
     ;;                                                        (:stop)
     ;;                                                        (.valueOf)
     ;;                                                        (+ (* 5 60 1000))
     ;;                                                        (js/Date.))}}])
     ;;   #(dispatch [:update-template {:id         (:id selected-template)
     ;;                                 :update-map {:stop (-> selected-template
     ;;                                                        (:stop)
     ;;                                                        (.valueOf)
     ;;                                                        (+ (* 60 60 1000))
     ;;                                                        (js/Date.))}}])]]

     ;; ;; stop-earlier
     ;; [view row-style
     ;;  [selection-menu-button
     ;;   "stop earlier"
     ;;   [mci {:name "arrow-collapse-up"}]
     ;;   #(dispatch [:update-template {:id         (:id selected-template)
     ;;                                 :update-map {:stop (-> selected-template
     ;;                                                        (:stop)
     ;;                                                        (.valueOf)
     ;;                                                        (- (* 5 60 1000))
     ;;                                                        (js/Date.))}}])
     ;;   #(dispatch [:update-template {:id         (:id selected-template)
     ;;                                 :update-map {:stop (-> selected-template
     ;;                                                        (:stop)
     ;;                                                        (.valueOf)
     ;;                                                        (- (* 60 60 1000))
     ;;                                                        (js/Date.))}}])]]

     ;; ;; select-prev
     ;; [view row-style
     ;;  [selection-menu-button
     ;;   "select prev"
     ;;   [mci {:name  "arrow-down-drop-circle"
     ;;         :style {:transform [{:rotate "180deg"}]}}]
     ;;   #(dispatch [:select-next-or-prev-template :prev])]]

     ;; ;; select-next
     ;; [view row-style
     ;;  [selection-menu-button
     ;;   "select next"
     ;;   [mci {:name "arrow-down-drop-circle"}]
     ;;   #(dispatch [:select-next-or-prev-template :next])]]
     ]))

(defn root []
  (let [pattern-form      (subscribe [:get-pattern-form])
        changes           (subscribe [:get-pattern-form-changes])
        selected-template (subscribe [:get-selected-template])
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
         [view {:style (top-bar-outer-style top-bar-height dimensions)}
          [text (:label @pattern-form)]]

         ;; view that stretches to fill what is left of the screen
         [touchable-highlight
          {:on-long-press #(println "should make a template on this pattern")}

          [view {:style {:height           (:height @dimensions)
                         :width            (:width @dimensions)
                         :background-color styles/background-color}}

           [time-indicators @dimensions :left]
           [templates-comp {:templates  (->> @pattern-form
                                             :templates
                                             (sort-by #(helpers/relative-to-minutes
                                                        (:start %)))
                                             (helpers/get-collision-groups))
                            :dimensions @dimensions}]
           (when (some? @selected-template)
             [selection-menu {:selected-period-or-template (merge
                                                            @selected-template
                                                            {:planned true})
                              :dimensions                  @dimensions}
              [selection-menu-buttons @selected-template @pattern-form]])]]

         [bottom-bar {:bottom-bar-height bottom-bar-height}
          [:<>
           [:> rne/Button
            ;; TODO prompt user that this will lose any unsaved changes
            {:icon            (r/as-element [:> rne/Icon {:name  "arrow-back"
                                                          :type  "material-icons"
                                                          :color "#fff"}])
             :on-press        #(dispatch [:navigate-to {:current-screen :pattern
                                                        :params         {:pattern-id (:id @pattern-form)}}])
             :container-style {:margin-right 4}}]

           [:> rne/Button
            (merge {:container-style {:margin-left 4}
                    :icon            (r/as-element [:> rne/Icon {:name  "save"
                                                                 :type  "font-awesome"
                                                                 :color "#fff"}])
                    :on-press        #(dispatch [:save-pattern-form (js/Date.)])}
                   (when-not (> (count @changes) 0)
                     {:disabled true}))]
           ]]])})))
