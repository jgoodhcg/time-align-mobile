(ns time-align-mobile.screens.template-form
  (:require [time-align-mobile.js-imports :refer [view text]]
            [re-frame.core :refer [subscribe dispatch]]
            ["react" :as react]
            ["react-native-elements" :as rne]
            [time-align-mobile.js-imports :refer [view
                                                  keyboard-aware-scroll-view
                                                  text
                                                  text-input
                                                  color-picker
                                                  date-time-picker
                                                  modal
                                                  switch
                                                  platform
                                                  picker
                                                  picker-item
                                                  touchable-highlight
                                                  format-time
                                                  format-date]]
            [time-align-mobile.components.form-buttons :as form-buttons]
            [time-align-mobile.components.structured-data :refer [structured-data]]
            [time-align-mobile.components.form-fields :refer [id-comp
                                                              created-comp
                                                              last-edited-comp
                                                              label-comp
                                                              pattern-parent-id-comp
                                                              pattern-parent-picker-comp
                                                              bucket-parent-id-comp
                                                              bucket-parent-picker-comp
                                                              planned-comp
                                                              data-comp]]
            [reagent.core :as r :refer [atom]]
            [time-align-mobile.helpers :as helpers]
            [time-align-mobile.styles :refer [field-label-changeable-style
                                              field-label-style]]))

(def start-modal-visible (r/atom false))

(def stop-modal-visible (r/atom false))

(defn start-comp [template-form changes]
  (let [start-ms   (:start @template-form)
        start-time (helpers/reset-relative-ms start-ms (js/Date.))]
    [view {:style {:flex-direction "row"}}
     [text {:style (merge {:margin-right 8}
                          (field-label-changeable-style @changes :stop))} ":start"]
     [touchable-highlight {:on-press #(reset! start-modal-visible true)}
      [text (format-time start-time)]]
     [date-time-picker {:is-visible @start-modal-visible
                        :date       start-time
                        :mode       "time"
                        :on-confirm (fn [d]
                                      (dispatch
                                       [:update-template-form {:start (helpers/get-ms d)}])
                                      (reset! start-modal-visible false))
                        :on-cancel  #(reset! start-modal-visible false)}]]))

(defn stop-comp [template-form changes]
  (let [stop-ms (:stop @template-form)
        stop-time (helpers/reset-relative-ms stop-ms (js/Date.))]
    [view {:style {:flex-direction "row"}}
     [text {:style (merge {:margin-right 8}
                          (field-label-changeable-style @changes :stop))} ":stop"]
     [touchable-highlight {:on-press #(reset! stop-modal-visible true)}
      [text (format-time stop-time)]]
     [date-time-picker {:is-visible @stop-modal-visible
                        :date       stop-time
                        :mode       "time"
                        :on-confirm (fn [d]
                                      (dispatch [:update-template-form {:stop (helpers/get-ms d)}])
                                      (reset! stop-modal-visible false))
                        :on-cancel  #(reset! stop-modal-visible false)}]]))

(defn compact [params]
  [text "compact form here"])

(defn root [params]
  (let [template-form                  (subscribe [:get-template-form])
        update-structured-data         (fn [new-data]
                                         (dispatch
                                          [:update-template-form {:data new-data}]))
        changes                        (subscribe [:get-template-form-changes])
        changes-from-pattern-planning  (subscribe [:get-template-form-changes-from-pattern-planning])
        buckets                        (subscribe [:get-buckets])
        patterns                       (subscribe [:get-patterns])
        template-from-pattern-planning (contains? params :pattern-form-pattern-id)]

    [keyboard-aware-scroll-view
     ;; check link for why these options https://stackoverflow.com/questions/45466026/keyboard-aware-scroll-view-android-issue?rq=1
     {:enable-on-android            true
      :enable-auto-automatic-scroll (= (.-OS platform) "ios")}
     [view {:style {:flex            1
                    :flex-direction  "column"
                    :justify-content "flex-start"
                    :align-items     "flex-start"
                    :padding-top     50
                    :padding-left    10}}

      [text "Template form"]

      [pattern-parent-id-comp template-form changes]

      (when (not template-from-pattern-planning)
        [pattern-parent-picker-comp template-form changes patterns :update-template-form])

      [bucket-parent-id-comp template-form changes]

      [bucket-parent-picker-comp
       template-form
       changes
       buckets
       :update-template-form]

      [id-comp template-form]

      [created-comp template-form]

      [last-edited-comp template-form]

      [label-comp template-form changes :update-template-form]

      [start-comp template-form changes]

      [stop-comp template-form changes]

      ;; [data-comp template-form changes update-structured-data]
      (when template-from-pattern-planning
        [:> rne/Button
         {:icon            (r/as-element [:> rne/Icon {:name  "arrow-back"
                                                       :type  "material-icons"
                                                       :color "#fff"}])
          :on-press        #(dispatch [:navigate-to {:current-screen :pattern-planning
                                                     :params         {:do-not-load-form true}}])
          :container-style {:margin-right 4}}])

      (if template-from-pattern-planning
        [form-buttons/root
         {:changed        (> (count @changes-from-pattern-planning) 0)
          :save-changes   #(dispatch [:save-template-form-from-pattern-planning
                                      (new js/Date)])
          :cancel-changes #(dispatch [:load-template-form-from-pattern-planning
                                      (:id @template-form)])
          :delete-item    #(dispatch [:delete-template-from-pattern-planning
                                      (:id @template-form)])}]
        [form-buttons/root
         {:changed        (> (count @changes) 0)
          :save-changes   #(dispatch [:save-template-form (new js/Date)])
          :cancel-changes #(dispatch [:load-template-form (:id @template-form)])
          :delete-item    #(dispatch [:delete-template (:id @template-form)])}])]]))
