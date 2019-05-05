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
                                            {:start   (helpers/reset-relative-ms
                                                       start now)
                                             :stop    (helpers/reset-relative-ms
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
       (fn [_]
         ;; TODO stop from moving past stop ? or does spec do that?
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :start]
                                  #(+ (helpers/minutes->ms 5) %)
                                  pattern-form)
                                 [:templates])]))
       (fn [_]
         ;; TODO stop from moving past stop
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :start]
                                  #(+ (helpers/hours->ms 3) %)
                                  pattern-form)
                                 [:templates])]))]]

     ;; start-earlier
     [view row-style
      [selection-menu-button
       "start earlier"
       [mci {:name "arrow-expand-up"}]
       (fn [_]
         ;; TODO stop from moving below 0
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :start]
                                  #(- % (helpers/minutes->ms 5))
                                  pattern-form)
                                 [:templates])]))
       (fn [_]
         ;; TODO stop from moving below 0
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))
                                   :start]
                                  #(- % (helpers/hours->ms 3))
                                  pattern-form)
                                 [:templates])]))]]

     ;; up
     [view row-style
      [selection-menu-button
       "up"
       [mi {:name "arrow-upward"}]
       (fn [_]
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))]
                                  (fn [template]
                                    (->> template
                                         (transform
                                          [:start]
                                          #(max 0 (- % (helpers/minutes->ms 5))))
                                         (transform
                                          [:stop]
                                          #(max 1 (- % (helpers/minutes->ms 5))))))
                                  pattern-form)
                                 [:templates])]))
       (fn [_]
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))]
                                  (fn [template]
                                    (->> template
                                         (transform
                                          [:start]
                                          #(max 0 (- % (helpers/hours->ms 3))))
                                         (transform
                                          [:stop]
                                          #(max 1 (- % (helpers/hours->ms 3))))))
                                  pattern-form)
                                 [:templates])]))]]

     ;; down
     [view row-style
      [selection-menu-button
       "down"
       [mi {:name "arrow-downward"}]
       (fn [_]
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))]
                                  (fn [template]
                                    (->> template
                                         (transform
                                          [:start]
                                          #(min (helpers/hours->ms 23.8)
                                                (+ % (helpers/minutes->ms 5))))
                                         (transform
                                          [:stop]
                                          #(min (helpers/hours->ms 23.9)
                                                (+ % (helpers/minutes->ms 5))))))
                                  pattern-form)
                                 [:templates])]))
       (fn [_]
         (dispatch [:update-pattern-form
                    (select-keys (transform
                                  [:templates sp/ALL
                                   #(= (:id %) (:id selected-template))]
                                  (fn [template]
                                    (->> template
                                         (transform
                                          [:start]
                                          #(min (helpers/hours->ms 23.8)
                                                (+ % (helpers/hours->ms 3))))
                                         (transform
                                          [:stop]
                                          #(min (helpers/hours->ms 23.9)
                                                (+ % (helpers/hours->ms 3))))))
                                  pattern-form)
                                 [:templates])]))]]

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
                                             (sort-by :start)
                                             (helpers/get-collision-groups))
                            :dimensions @dimensions}]

           ;; selection menu
           (when (some? @selected-template)
             [selection-menu
              {:selected-period-or-template (-> @selected-template
                                                :id
                                                ;; get the template from the form
                                                ;; not from the pattern
                                                (#(some (fn [template]
                                                          (if (= (:id template)
                                                                 %)
                                                            template
                                                            false))
                                                        (:templates @pattern-form)))
                                                (#(merge
                                                   %
                                                   {:planned true})))
               :dimensions                  @dimensions}
              [selection-menu-buttons @selected-template @pattern-form]])]]

         [bottom-bar {:bottom-bar-height bottom-bar-height}
          [:<>
           ;; back button
           [:> rne/Button
            ;; TODO prompt user that this will lose any unsaved changes
            {:icon            (r/as-element [:> rne/Icon {:name  "arrow-back"
                                                          :type  "material-icons"
                                                          :color "#fff"}])
             :on-press        #(dispatch [:navigate-to {:current-screen :pattern
                                                        :params         {:pattern-id (:id @pattern-form)}}])
             :container-style {:margin-right 4}}]

           ;; save button
           [:> rne/Button
            (merge {:container-style {:margin-left 4}
                    :icon            (r/as-element [:> rne/Icon {:name  "save"
                                                                 :type  "font-awesome"
                                                                 :color "#fff"}])
                    :on-press        #(dispatch [:save-pattern-form (js/Date.)])}
                   (when-not (> (count @changes) 0)
                     {:disabled true}))]
           ]]])})))
