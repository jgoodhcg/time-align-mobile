(ns time-align-mobile.screens.period-form
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
                                                  format-date]]
            [time-align-mobile.components.form-buttons :as form-buttons]
            [time-align-mobile.components.structured-data :refer [structured-data]]
            [time-align-mobile.components.form-fields :refer [id-comp
                                                              created-comp
                                                              last-edited-comp
                                                              label-comp
                                                              bucket-parent-id-comp
                                                              bucket-parent-picker-comp
                                                              planned-comp
                                                              data-comp]]
            [reagent.core :as r :refer [atom]]
            [time-align-mobile.styles :refer [field-label-changeable-style
                                              field-label-style]]))

(def start-modal-visible (r/atom false))

(def stop-modal-visible (r/atom false))

(defn start-comp [period-form changes]
  (let [start (:start @period-form)]
    [view {:style {:flex-direction "row"}}
     [text {:style (field-label-changeable-style @changes :start)} ":start"]
     [touchable-highlight {:on-press #(reset! start-modal-visible true)}
      [text (if (some? start)
              (format-date start)
              "Add a start date-time")]]
     [date-time-picker {:is-visible @start-modal-visible
                        :date (if (some? start) start (js/Date.))
                        :mode "datetime"
                        :on-confirm (fn [d]
                                      (dispatch [:update-period-form {:start d}])
                                      (reset! start-modal-visible false))
                        :on-cancel #(reset! start-modal-visible false)}]]))

(defn stop-comp [period-form changes]
  (let [stop (:stop @period-form)]
    [view {:style {:flex-direction "row"}}
     [text {:style (field-label-changeable-style @changes :stop)} ":stop"]
     [touchable-highlight {:on-press #(reset! stop-modal-visible true)} [text (if (some? stop)
              (format-date stop)
              "Add a stop date-time")]]
     [date-time-picker {:is-visible @stop-modal-visible
                        :date (if (some? stop) stop (js/Date.))
                        :mode "datetime"
                        :on-confirm (fn [d]
                                      (dispatch [:update-period-form {:stop d}])
                                      (reset! stop-modal-visible false))
                        :on-cancel #(reset! stop-modal-visible false)}]]))

(defn selection-menu-form []
  )

(defn root [params]
  (let [period-form            (subscribe [:get-period-form])
        update-structured-data (fn [new-data]
                                 (dispatch
                                  [:update-period-form {:data new-data}]))
        changes                (subscribe [:get-period-form-changes])
        buckets                (subscribe [:get-buckets])]
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

      [text "Period form"]

      [bucket-parent-id-comp period-form changes]

      [bucket-parent-picker-comp period-form changes buckets :update-period-form]

      [id-comp period-form]

      [created-comp period-form]

      [last-edited-comp period-form]

      [label-comp period-form changes :update-period-form]

      [planned-comp period-form changes :update-period-form]

      [start-comp period-form changes]

      [stop-comp period-form changes]

      ;; [data-comp period-form changes update-structured-data]

      [form-buttons/root
       {:changed        (> (count @changes) 0)
        :save-changes   #(dispatch [:save-period-form (new js/Date)])
        :cancel-changes #(dispatch [:load-period-form (:id @period-form)])
        :delete-item    #(dispatch [:delete-period (:id @period-form)])}]]]))
