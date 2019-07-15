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
            [time-align-mobile.helpers :as helpers :refer [xor]]
            [re-frame.core :refer [subscribe dispatch]]
            [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]
            [time-align-mobile.components.day :refer [time-indicators
                                                      top-bar-outer-style
                                                      bottom-bar
                                                      render-period
                                                      selection-menu
                                                      get-touch-info-from-event
                                                      selection-menu-button-row-style
                                                      selection-menu-button
                                                      selection-menu-button-container-style
                                                      selection-menu-button-container-style
                                                      padding]]
            [reagent.core :as r]))

(defn start-later
  ([pattern-form selected-template]
   (start-later pattern-form selected-template false))
  ([pattern-form selected-template long]
   (let [time (if long
                (helpers/hours->ms 3)
                (helpers/minutes->ms 5))]
     (fn [_]
       (dispatch [:update-pattern-form
                  (select-keys (transform
                                [:templates sp/ALL
                                 #(= (:id %) (:id selected-template))
                                 :start]
                                #(+ % time)
                                pattern-form)
                               [:templates])])))))

(defn start-earlier
  ([pattern-form selected-template]
   (start-earlier pattern-form selected-template false))
  ([pattern-form selected-template long]
   (let [time (if long
                (helpers/hours->ms 3)
                (helpers/minutes->ms 5))]
     (fn [_]
       ;; TODO stop from moving below 0
       (dispatch [:update-pattern-form
                  (select-keys (transform
                                [:templates sp/ALL
                                 #(= (:id %) (:id selected-template))
                                 :start]
                                #(- % time)
                                pattern-form)
                               [:templates])])))))

(defn up
  ([pattern-form selected-template]
   (up pattern-form selected-template false))
  ([pattern-form selected-template long]
   (let [time (if long
                (helpers/hours->ms 3)
                (helpers/minutes->ms 5))]
     (fn [_]
       (dispatch [:update-pattern-form
                  (select-keys (transform
                                [:templates sp/ALL
                                 #(= (:id %) (:id selected-template))]
                                (fn [template]
                                  (->> template
                                       (transform
                                        [:start]
                                        #(max 0 (- % time)))
                                       (transform
                                        [:stop]
                                        #(max 1 (- % time)))))
                                pattern-form)
                               [:templates])])))))

(defn down
  ([pattern-form selected-template]
   (down pattern-form selected-template false))
  ([pattern-form selected-template long]
   (let [time (if long
                (helpers/hours->ms 3)
                (helpers/minutes->ms 5))]
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
                                              (+ % time)))
                                       (transform
                                        [:stop]
                                        #(min (helpers/hours->ms 23.9)
                                              (+ % time)))))
                                pattern-form)
                               [:templates])])))))

(defn stop-later
  ([pattern-form selected-template]
   (stop-later pattern-form selected-template false))
  ([pattern-form selected-template long]
   (let [time (if long
                (helpers/hours->ms 3)
                (helpers/minutes->ms 5))]
     (fn [_]
       ;; TODO keep from going beyond end of day
       (dispatch [:update-pattern-form
                  (select-keys (transform
                                [:templates sp/ALL
                                 #(= (:id %) (:id selected-template))
                                 :stop]
                                #(+ % time)
                                pattern-form)
                               [:templates])])))))

(defn stop-earlier
  ([pattern-form selected-template]
   (stop-earlier pattern-form selected-template false))
  ([pattern-form selected-template long]
   (let [time (if long
                (helpers/hours->ms 3)
                (helpers/minutes->ms 5))]
     (fn [_]
       ;; TODO keep from going before start
       (dispatch [:update-pattern-form
                  (select-keys (transform
                                [:templates sp/ALL
                                 #(= (:id %) (:id selected-template))
                                 :stop]
                                #(- % time)
                                pattern-form)
                               [:templates])])))))

(defn generate-transform-functions [pattern-form]
  {:up            (partial up pattern-form)
   :down          (partial down pattern-form)
   :start-earlier (partial start-earlier pattern-form)
   :stop-earlier  (partial stop-earlier pattern-form)
   :stop-later    (partial stop-later pattern-form)
   :start-later   (partial start-later pattern-form)})

(defn render-templates-col
  "Renders all non-selected only when `render-selected-only` is false. Only renders selected when it is true."
  [{:keys [templates
           dimensions
           selected-template
           pattern-form
           render-selected-only]}]
  (->> templates
       (map (fn [collision-group]
              (doall
               (->> collision-group
                    (map-indexed
                     (fn [index {:keys [start stop] :as template}]
                       (when (xor render-selected-only
                                  (not= (:id template)
                                        (:id selected-template)))
                         (let [now (js/Date.)]
                           (render-period
                            {:entity (merge template  ;; TODO refactor :period key?
                                            {:start   (helpers/reset-relative-ms
                                                       start now)
                                             :stop    (helpers/reset-relative-ms
                                                       stop now)
                                             :planned true})

                             :transform-functions       (generate-transform-functions pattern-form)
                             :entity-type               :template
                             :collision-index           index
                             :collision-group-size      (count collision-group)
                             :displayed-day             now
                             :dimensions                dimensions
                             :selected-entity           selected-template
                             :select-function-generator (fn [id]
                                                          #(dispatch [:select-template id]))
                             :period-in-play            nil})))))))))))

(defn templates-comp [{:keys [templates dimensions selected-template pattern-form]}]
  [view
   ;; render everything but selected
   (render-templates-col {:templates            templates
                          :dimensions           dimensions
                          :selected-template    selected-template
                          :pattern-form         pattern-form
                          :render-selected-only false})

   ;; render only the selected
   (render-templates-col {:templates            templates
                          :dimensions           dimensions
                          :selected-template    selected-template
                          :pattern-form         pattern-form
                          :render-selected-only true})])

(defn selection-menu-buttons [selected-template pattern-form]
  (let [row-style {:style selection-menu-button-row-style}]
    [view {:style selection-menu-button-container-style}
     [view row-style
      ;; select-prev
      [selection-menu-button
       "select prev"
       [mci {:name  "arrow-down-drop-circle"
             :style {:transform [{:rotate "180deg"}]}}]
       #(dispatch [:select-next-or-prev-template-in-form :prev])]

      ;; edit
      [selection-menu-button
       "edit"
       [mi {:name "edit"}]
       (fn [_]
         (dispatch [:navigate-to
                    {:current-screen :template
                     :params         {:template-id             (:id selected-template)
                                      :pattern-form-pattern-id (:id pattern-form)}}]))]
      ;; select-next
      [selection-menu-button
       "select next"
       [mci {:name "arrow-down-drop-circle"}]
       #(dispatch [:select-next-or-prev-template-in-form :next])]]]))

(defn root []
  (let [pattern-form      (subscribe [:get-pattern-form])
        buckets           (subscribe [:get-buckets]) ;; this is just for selecting a random bucket for new template long press
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
          ;; add new template on long press
          {:on-long-press (fn [evt]
                            (let [{:keys [native-event
                                          location-y
                                          location-x
                                          relative-ms
                                          start]}
                                  (get-touch-info-from-event {:evt           evt
                                                              :dimensions    @dimensions
                                                              :displayed-day (js/Date.)})]
                              (dispatch [:add-new-template-to-planning-form
                                         {:pattern-id (:id @pattern-form)
                                          :start      start
                                          :bucket-id  (->> @buckets
                                                           first
                                                           :id)
                                          :id         (random-uuid)
                                          :now        (js/Date.)}])))}

          [view {:style {:height           (:height @dimensions)
                         :width            (:width @dimensions)
                         :background-color styles/background-color}}

           [time-indicators @dimensions :left]

           ;; templates
           [templates-comp {:templates         (->> @pattern-form
                                                    :templates
                                                    (sort-by :start)
                                                    (helpers/get-collision-groups))
                            :pattern-form      @pattern-form
                            :selected-template @selected-template
                            :dimensions        @dimensions}]

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
               :type                        :template
               :dimensions                  @dimensions}
              [selection-menu-buttons @selected-template @pattern-form]])]]

         [bottom-bar {:bottom-bar-height bottom-bar-height}
          [view {:flex-direction "row"}
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
                     {:disabled true}))]]]])})))
