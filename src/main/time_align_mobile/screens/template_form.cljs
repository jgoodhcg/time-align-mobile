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
                                                              parent-id-comp
                                                              parent-picker-comp
                                                              planned-comp
                                                              data-comp]]
            [reagent.core :as r :refer [atom]]
            [time-align-mobile.styles :refer [field-label-changeable-style
                                              field-label-style]]))

(def start-modal-visible (r/atom false))

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
     [text {:style (field-label-changeable-style @changes :start)} ":start"]
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

(defn duration-comp [template-form changes]
  (let [duration (:duration @template-form) ;; in ms
        hours    (quot duration (* 60 60 1000))
        minutes  (quot (- duration
                          (* hours 60 60 1000))
                       (* 60 1000))
        seconds  (quot (- duration
                          (* hours 60 60 1000)
                          (* minutes 60 1000))
                       1000)
        row-style {:flex-direction "row"
                   :align-items    "center"}]

    [view {:style {:flex-direction "row"}}
     [text {:style (field-label-changeable-style @changes :duration)} ":duration"]
     [view {:style {:flex-direction "column"}}
      [view {:style row-style}
       ;; hours
       [text {:style {:color "grey" :margin-right 10}} "hours"]
       [text-input {:default-value  (.toString hours)
                    :style          {:height 40
                                     :width  200}
                    :on-change-text (fn [val]
                                      (let [hours (js/parseFloat val)]
                                        (if (not (js/isNaN hours))
                                          (dispatch [:update-template-form
                                                     {:duration
                                                      (+ (* hours 60 60 1000)
                                                         (* minutes 60 1000)
                                                         (* seconds 1000))}])
                                          ;; TODO should this be a noop?
                                          (println "not a number...."))))}]]

      ;; minutes
      [view {:style row-style}
       [text {:style {:color "grey" :margin-right 10}} "minutes"]
       [text-input {:default-value  (.toString minutes)
                    :style          {:height 40
                                     :width  200}
                    :on-change-text (fn [val]
                                      (let [minutes (js/parseFloat val)]
                                        (if (not (js/isNaN minutes))
                                          (dispatch [:update-template-form
                                                     {:duration
                                                      (+ (* hours 60 60 1000)
                                                         (* minutes 60 1000)
                                                         (* seconds 1000))}])
                                          ;; TODO should this be a noop?
                                          (println "not a number...."))))}]]

      ;; seconds
      [view {:style row-style}
       [text {:style {:color "grey" :margin-right 10}} "seconds"]
       [text-input {:default-value  (.toString seconds)
                    :style          {:height 40
                                     :width  200}
                    :on-change-text (fn [val]
                                      (let [seconds (js/parseFloat val)]
                                        (if (not (js/isNaN seconds))
                                          (dispatch [:update-template-form
                                                     {:duration
                                                      (+ (* hours 60 60 1000)
                                                         (* minutes 60 1000)
                                                         (* seconds 1000))}])
                                          ;; TODO should this be a noop?
                                          (println "not a number...."))))}]]]]))

(defn root [params]
  (let [template-form            (subscribe [:get-template-form])
        update-structured-data (fn [new-data]
                                 (dispatch
                                  [:update-template-form {:data new-data}]))
        changes                (subscribe [:get-template-form-changes])
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

      [text "Template form"]

      [parent-id-comp template-form changes]

      [parent-picker-comp template-form changes buckets :update-template-form]

      [id-comp template-form]

      [created-comp template-form]

      [last-edited-comp template-form]

      [label-comp template-form changes :update-template-form]

      [planned-comp template-form changes :update-template-form]

      [start-comp template-form changes]

      [duration-comp template-form changes]

      ;; [data-comp template-form changes update-structured-data]

      [form-buttons/root
       {:changed        (> (count @changes) 0)
        :save-changes   #(dispatch [:save-template-form (new js/Date)])
        :cancel-changes #(dispatch [:load-template-form (:id @template-form)])
        :delete-item    #(dispatch [:delete-template (:id @template-form)])}]]]))
