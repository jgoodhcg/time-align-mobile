(ns time-align-mobile.screens.template-form
  (:require [time-align-mobile.js-imports :refer [view text]]
            [re-frame.core :refer [subscribe dispatch]]
            ["react" :as react]
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
            [time-align-mobile.styles :refer [field-label-changeable-style
                                              field-label-style]]))

(def start-modal-visible (r/atom false))

(def stop-modal-visible (r/atom false))

(defn start-comp [template-form changes]
  (let [{:keys [hour minute]} (:start @template-form)
        std                   (new js/Date)
        start-time            (new js/Date
                                   (.getFullYear std)
                                   (.getMonth std)
                                   (.getDate std)
                                   hour
                                   minute)]
    [view {:style {:flex-direction "row"}}
     [text {:style (merge {:margin-right 8}
                          (field-label-changeable-style @changes :stop))} ":start"]
     [touchable-highlight {:on-press #(reset! start-modal-visible true)}
      [text (format-time start-time)]]
     [date-time-picker {:is-visible @start-modal-visible
                        :date       start-time
                        :mode       "time"
                        :on-confirm (fn [d]
                                      (dispatch [:update-template-form {:start {:hour   (.getHours d)
                                                                                :minute (.getMinutes d)}}])
                                      (reset! start-modal-visible false))
                        :on-cancel  #(reset! start-modal-visible false)}]]))

(defn stop-comp [template-form changes]
  (let [{:keys [hour minute]} (:stop @template-form)
        std                   (new js/Date)
        stop-time            (new js/Date
                                   (.getFullYear std)
                                   (.getMonth std)
                                   (.getDate std)
                                   hour
                                   minute)]
    [view {:style {:flex-direction "row"}}
     [text {:style (merge {:margin-right 8}
                          (field-label-changeable-style @changes :stop))} ":stop"]
     [touchable-highlight {:on-press #(reset! stop-modal-visible true)}
      [text (format-time stop-time)]]
     [date-time-picker {:is-visible @stop-modal-visible
                        :date       stop-time
                        :mode       "time"
                        :on-confirm (fn [d]
                                      (dispatch [:update-template-form {:stop {:hour   (.getHours d)
                                                                                :minute (.getMinutes d)}}])
                                      (reset! stop-modal-visible false))
                        :on-cancel  #(reset! stop-modal-visible false)}]]))

(defn root [params]
  (let [template-form          (subscribe [:get-template-form])
        update-structured-data (fn [new-data]
                                 (dispatch
                                  [:update-template-form {:data new-data}]))
        changes                (subscribe [:get-template-form-changes])
        patterns               (subscribe [:get-patterns])]
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

      [pattern-parent-picker-comp template-form changes patterns :update-template-form]


      [bucket-parent-id-comp template-form changes]

      [bucket-parent-picker-comp template-form changes patterns :update-template-form]

      [id-comp template-form]

      [created-comp template-form]

      [last-edited-comp template-form]

      [label-comp template-form changes :update-template-form]

      [start-comp template-form changes]

      [stop-comp template-form changes]

      ;; [data-comp template-form changes update-structured-data]

      [form-buttons/root
       {:changed        (> (count @changes) 0)
        :save-changes   #(dispatch [:save-template-form (new js/Date)])
        :cancel-changes #(dispatch [:load-template-form (:id @template-form)])
        :delete-item    #(dispatch [:delete-template (:id @template-form)])}]]]))
