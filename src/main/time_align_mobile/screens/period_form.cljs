(ns time-align-mobile.screens.period-form
  (:require [time-align-mobile.js-imports :refer [view text]]
            [re-frame.core :refer [subscribe dispatch]]
            ["react" :as react]
            [time-align-mobile.js-imports :refer [view
                                                  keyboard-aware-scroll-view
                                                  text
                                                  surface
                                                  subheading
                                                  button-paper
                                                  text-input
                                                  text-input-paper
                                                  color-picker
                                                  date-time-picker
                                                  modal
                                                  switch
                                                  platform
                                                  ic
                                                  picker
                                                  picker-item
                                                  touchable-highlight
                                                  format-date
                                                  format-time
                                                  format-date-day]]
            [time-align-mobile.components.form-buttons :as form-buttons]
            [time-align-mobile.components.structured-data :refer [structured-data]]
            [time-align-mobile.components.form-fields :refer [id-comp
                                                              created-comp
                                                              last-edited-comp
                                                              label-comp
                                                              label-style
                                                              bucket-parent-id-comp
                                                              changeable-field
                                                              bucket-parent-picker-comp
                                                              info-field-style
                                                              planned-comp
                                                              data-comp]]
            [reagent.core :as r :refer [atom]]
            [time-align-mobile.styles :refer [field-label-changeable-style
                                              theme
                                              field-label-style]]))

(def start-modal (r/atom {:visible false
                          :mode    "date"})) ;; TODO spec type for "date" "time"

(def stop-modal (r/atom {:visible false
                          :mode    "date"})) ;; TODO spec type for "date" "time"

(defn time-comp-buttons [period-form changes modal field-key label time]
  [:<>
   ;; Date
     [button-paper {:on-press #(reset! modal {:visible true
                                              :mode    "date"})
                    :style    {:margin-right 4}
                    :mode     "outlined"
                    :icon     "date-range"}
      [text (if (some? time)
              (format-date-day time)
              "Add a time date")]]

     ;; Time
     [button-paper {:on-press #(reset! modal {:visible true
                                              :mode    "time"})
                    :mode     "outlined"
                    :icon     "access-time"}
      [text (if (some? time)
              (format-time time)
              "Add a time time")]]

     ;; Modal
     [date-time-picker {:is-visible (:visible @modal)
                        :date       (if (some? time) time (js/Date.))
                        :mode       (:mode @modal)
                        :on-confirm (fn [d]
                                      (dispatch [:update-period-form {field-key d}])
                                      (reset! modal {:visible false
                                                     :mode    "date"}))
                        :on-cancel  #(reset! modal {:visible false
                                                    :mode    "date"})}]])

(defn time-comp [period-form changes modal field-key label]
  (let [time (field-key @period-form)]
    [view {:style info-field-style}
     (changeable-field {:changes changes
                        :field-key field-key}
                       [subheading {:style label-style} label])
     [time-comp-buttons period-form changes modal field-key label time]]))

(defn compact [params]
  [text "compact form here"])

(defn root [params]
  (let [period-form            (subscribe [:get-period-form])
        update-structured-data (fn [new-data]
                                 (dispatch
                                  [:update-period-form {:data new-data}]))
        changes                (subscribe [:get-period-form-changes])
        buckets                (subscribe [:get-buckets])]
    [:<>
     [bucket-parent-picker-comp period-form changes buckets :update-period-form]

     [label-comp period-form changes :update-period-form]

     [time-comp period-form changes start-modal :start "start"]

     [time-comp period-form changes stop-modal :stop "stop"]

     [planned-comp period-form changes :update-period-form]

     [id-comp period-form]

     [created-comp period-form]

     [last-edited-comp period-form]

     ;; [data-comp period-form changes update-structured-data]

     [form-buttons/root
      {:changed        (> (count @changes) 0)
       :save-changes   #(dispatch [:save-period-form (new js/Date)])
       :cancel-changes #(dispatch [:load-period-form (:id @period-form)])
       :delete-item    #(dispatch [:delete-period (:id @period-form)])}]]))
