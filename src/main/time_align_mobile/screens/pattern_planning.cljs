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

(defn selection-menu-buttons [selected-template]
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
              [selection-menu-buttons @selected-template]])]]

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
