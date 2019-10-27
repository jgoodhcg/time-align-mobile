(ns time-align-mobile.screens.period-form
  (:require [time-align-mobile.js-imports :refer [view text]]
            [re-frame.core :refer [subscribe dispatch]]
            ["react" :as react]
            [time-align-mobile.js-imports :refer [view
                                                  keyboard-aware-scroll-view
                                                  text
                                                  text-paper
                                                  surface
                                                  subheading
                                                  button-paper
                                                  text-input
                                                  text-input-paper
                                                  icon-button
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
                                                              changeable-field
                                                              duration-comp
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
                  :style    {:margin-right  4
                             :margin-bottom 4}
                  :mode     "outlined"
                  :icon     "calendar-range"}
      [text (if (some? time)
              (format-date-day time)
              "Add a time date")]]

     ;; Time
   [button-paper {:on-press #(reset! modal {:visible true
                                            :mode    "time"})
                  :style    {:margin-right  4
                             :margin-bottom 4}
                  :mode     "outlined"
                  :icon     "clock-outline"}
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

(defn time-comp-compact [period-form changes modal field-key label]
  (let [time (field-key @period-form)]
    [view {:style {:flex-direction "row"
                   :margin-bottom  4}}
     (changeable-field {:changes   changes
                        :field-key field-key} [view])
     [view {:style {:flex-direction "row"}}
      [time-comp-buttons period-form changes modal field-key label time]]]))

(defn compact [{:keys [delete-callback save-callback close-callback] :as params}]
  (let [period-form            (subscribe [:get-period-form])
        update-structured-data (fn [new-data]
                                 (dispatch
                                  [:update-period-form {:data new-data}]))
        changes                (subscribe [:get-period-form-changes])
        buckets                (subscribe [:get-buckets])]

    [view {:style {:flex            1
                   :width           "100%"
                   :flex-direction  "column"
                   :justify-content "space-between"
                   :align-items     "flex-start"}}

     [view {:flex-direction  "row"
            :justify-content "space-between"
            :width           "100%"
            :padding-left    16
            :padding-right   16}
      [icon-button {:icon     "close"
                    :size     20
                    :on-press close-callback}]
      [view {:flex-direction "row"}
       [button-paper {:on-press #(dispatch
                                  [:play-from-period
                                   {:id           (:id period-form)
                                    :time-started (js/Date.)
                                    :new-id       (random-uuid)}])
                      :mode     "text"
                      :icon     "play-circle"
                      :style    {:margin-right 8}}
        "play"]
       [button-paper {:on-press save-callback
                      :mode     "contained"
                      :icon     "content-save"}
        "save"]]]

     [label-comp period-form changes :update-period-form true]

     [view {:style {:flex-direction "column"}}
      [time-comp-compact period-form changes start-modal :start "start"]
      [time-comp-compact period-form changes stop-modal :stop "stop"]]

     [duration-comp (:start @period-form) (:stop @period-form)]

     [planned-comp period-form changes :update-period-form]

     [view {:style {:width           "100%"
                    :justify-content "center"}}
      [bucket-parent-picker-comp
       {:form       period-form
        :changes    changes
        :buckets    buckets
        :update-key :update-period-form
        :compact    false}]]


     [view {:style {:flex-direction  "row"
                    :padding         8
                    :margin-top      16
                    :width           "100%"
                    :align-self      "center"
                    :justify-content "space-between"
                    :align-items     "space-between"}}
      [form-buttons/buttons
       {:changed        (> (count @changes) 0)
        :save-changes   #(do
                           (dispatch [:save-period-form (new js/Date)])
                           (when (and (some? save-callback))
                             (save-callback)))
        :cancel-changes #(dispatch [:load-period-form {:period-id (:id @period-form)
                                                       :bucket-id (:bucket-id @period-form)}])
        :delete-item    #(do
                           (dispatch [:delete-period {:period-id (:id @period-form)
                                                      :bucket-id (:bucket-id @period-form)}])
                           (when (and (some? delete-callback))
                             (delete-callback)))
        :edit-item      #(dispatch [:navigate-to {:current-screen :period
                                                  :params         {:period-id (:id @period-form)
                                                                   :bucket-id (:bucket-id @period-form)}}])
        :compact        true}]]]))

(defn root [params]
  (let [period-form            (subscribe [:get-period-form])
        update-structured-data (fn [new-data]
                                 (dispatch
                                  [:update-period-form {:data new-data}]))
        changes                (subscribe [:get-period-form-changes])
        buckets                (subscribe [:get-buckets])]

    [view {:style {:margin-top 16
                   :flex 1}}
     [bucket-parent-picker-comp {:form       period-form
                                 :changes    changes
                                 :buckets    buckets
                                 :update-key :update-period-form
                                 :compact    false}]

     [label-comp period-form changes :update-period-form false]

     [planned-comp period-form changes :update-period-form]

     [time-comp period-form changes start-modal :start "start"]

     [time-comp period-form changes stop-modal :stop "stop"]

     [duration-comp (:start @period-form) (:stop @period-form)]

     [id-comp period-form]

     [created-comp period-form]

     [last-edited-comp period-form]

     ;; [data-comp period-form changes update-structured-data]

     [form-buttons/root
      {:changed        (> (count @changes) 0)
       :save-changes   #(dispatch [:save-period-form (new js/Date)])
       :cancel-changes #(dispatch [:load-period-form {:period-id (:id @period-form)
                                                      :bucket-id (:bucket-id @period-form)}])
       :delete-item    #(dispatch [:delete-period {:period-id (:id @period-form)
                                                   :bucket-id (:bucket-id @period-form)}])}]]))
