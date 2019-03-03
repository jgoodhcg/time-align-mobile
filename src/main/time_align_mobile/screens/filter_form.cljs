(ns time-align-mobile.screens.filter-form
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
                                                  flat-list
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
                                                              label-comp]]
            [reagent.core :as r :refer [atom]]
            [time-align-mobile.styles :refer [field-label-changeable-style
                                              field-label-style]]))

(defn predicates-comp [form changes update-structured-data]
  [view {:style {:flex           1
                 :flex-direction "row"
                 :align-items    "flex-start"}}
   [text {:style (field-label-changeable-style changes :predicates)}
    ":predicates"]
   [structured-data {:data   (:predicates @form)
                     :update update-structured-data}]])

(defn sort-comp [form changes update-structured-data]
  [view {:style {:flex           1
                 :flex-direction "row"
                 :align-items    "flex-start"}}
   [text {:style (field-label-changeable-style changes :sort)}
    ":sort"]
   [structured-data {:data   (:sort @form)
                     :update update-structured-data}]])

(defn compatible-list-comp [form changes]
  (let [compatible-list (:compatible @form)
        style (fn [comp-key]
                      {:color (if (some #{comp-key} compatible-list)
                                "black"
                                "grey")
                       :margin-right 10})
        on-press (fn [comp-key]
                   (if (some #{comp-key} compatible-list)
                     #(dispatch [:update-filter-form
                                 {:compatible (remove #{comp-key}
                                                      compatible-list)}])
                     #(dispatch [:update-filter-form
                                 {:compatible (conj compatible-list
                                                    comp-key)}])))]
    [view {:style {:flex           1
                   :flex-direction "row"
                   :align-items    "flex-start"}}
     [text {:style (field-label-changeable-style changes :compatible)}
      ":compatible"]
     [view {:style {:flex-direction "row"}}
      ;; TODO pull all compatible-options from common place?
      (->> [:bucket :period :template :filter]
           (map (fn [comp-key]
                  [touchable-highlight
                   {:key (str comp-key "-compatible-list-option")
                    :on-press (on-press comp-key)}
                   [text {:style (style comp-key)} (str comp-key)]])))]]))

(defn root [params]
  (let [filter-form (subscribe [:get-filter-form])

        update-structured-data-predicates
        (fn [new-data]
          (dispatch
           [:update-filter-form {:predicates new-data}]))

        update-structured-data-sort
        (fn [new-data]
          (dispatch [:update-filter-form {:sort new-data}]))

        changes     (subscribe [:get-filter-form-changes])]
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

      [text "Filter form"]

      [id-comp filter-form]

      [created-comp filter-form]

      [last-edited-comp filter-form]

      [label-comp filter-form changes :update-filter-form]

      [compatible-list-comp filter-form changes]

      [sort-comp filter-form changes update-structured-data-sort]

      [predicates-comp filter-form changes update-structured-data-predicates]

      [form-buttons/root
       {:changed        (> (count @changes) 0)
        :save-changes   #(dispatch [:save-filter-form (new js/Date)])
        :cancel-changes #(dispatch [:load-filter-form (:id @filter-form)])
        :delete-item    #(dispatch [:delete-filter (:id @filter-form)])}]]]))
