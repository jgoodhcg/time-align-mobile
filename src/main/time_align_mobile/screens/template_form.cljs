(ns time-align-mobile.screens.template-form
  (:require [time-align-mobile.js-imports :refer [view text]]
            [re-frame.core :refer [subscribe dispatch]]
            ["react" :as react]
            [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]
            ["react-native-elements" :as rne]
            [time-align-mobile.js-imports :refer [view
                                                  keyboard-aware-scroll-view
                                                  text
                                                  text-input
                                                  color-picker
                                                  date-time-picker
                                                  subheading
                                                  modal
                                                  switch
                                                  platform
                                                  button-paper
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
                                                              duration-comp
                                                              label-comp
                                                              label-style
                                                              pattern-parent-picker-comp
                                                              bucket-parent-picker-comp
                                                              changeable-field
                                                              info-field-style
                                                              planned-comp
                                                              data-comp]]
            [reagent.core :as r :refer [atom]]
            [time-align-mobile.helpers :as helpers]
            [time-align-mobile.styles :refer [field-label-changeable-style
                                              field-label-style]]))

(def start-modal-visible (r/atom false))

(def stop-modal-visible (r/atom false))

(defn time-comp-buttons [time modal form update-key field-key]
  [:<>
   [button-paper {:on-press #(reset! modal true)
                  :mode     "outlined"
                  :icon     "access-time"}
    [text (if (some? time)
            (format-time time)
            "Add a time time")]]
   [date-time-picker {:is-visible @modal
                      :date       time
                      :mode       "time"
                      :on-confirm (fn [d]
                                    (dispatch
                                     [update-key {field-key (helpers/get-ms d)
                                                  :id    (:id @form)}])
                                    (reset! modal false))
                      :on-cancel  #(reset! modal false)}]])

;; TODO consolidate both comps into one
(defn time-comp [{:keys [template-form
                         changes
                         update-key
                         modal
                         field-key
                         label]}]
  (let [time-ms   (field-key @template-form)
        time-as-date (helpers/reset-relative-ms time-ms (js/Date.))]
    [view {:style info-field-style}
     (changeable-field {:changes changes
                        :field-key field-key}
                       [subheading {:style label-style} label])
     [time-comp-buttons time-as-date modal template-form update-key field-key]]))

(defn compact []
  (let [pattern-form          (subscribe [:get-pattern-form])
        template-form         (subscribe [:get-template-form])
        template-form-changes (subscribe [:get-template-form-changes-from-pattern-planning])
        buckets               (subscribe [:get-buckets])
        patterns              (subscribe [:get-patterns])]

    [view {:style {:flex             1
                   :width            "100%"
                   :flex-direction   "column"
                   :justify-content  "space-between"
                   :align-items      "flex-start"
                   :padding-top      8
                   :border-top-width 8
                   :border-color     (:bucket-color @template-form)}}
     [label-comp template-form template-form-changes :update-template-form]

     ;; start
     [time-comp {:template-form template-form
                 :changes       template-form-changes
                 :update-key    :update-template-form
                 :modal         start-modal-visible
                 :field-key     :start
                 :label         "Start"}]
     ;; stop
     [time-comp {:template-form template-form
                 :changes       template-form-changes
                 :update-key    :update-template-form
                 :modal         stop-modal-visible
                 :field-key     :stop
                 :label         "Stop"}]


     [duration-comp (:start @template-form) (:stop @template-form)]

     [view {:style {:flex-direction  "row" ;; TODO abstract this style from here and period form
                    :padding         8
                    :margin-top      16
                    :width           "100%"
                    :align-self      "center"
                    :justify-content "space-between"
                    :align-items     "space-between"}}

      [form-buttons/buttons
       {:compact        true
        :changed        (> (count @template-form-changes) 0)
        :save-changes   #(str "noop")
        :cancel-changes #(str "noop")
        :delete-item    #(dispatch [:delete-template-from-pattern-planning
                                    (:id @template-form)])}]]]))

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

    [view {:style {:margin-top 16
                   :flex 1}}
     [pattern-parent-picker-comp
      template-form
      changes
      patterns
      :update-template-form
      template-from-pattern-planning]

     [bucket-parent-picker-comp
      {:form       template-form
       :changes    changes
       :buckets    buckets
       :update-key :update-template-form
       :compact    false}]

     [label-comp template-form changes :update-template-form]

     ;; start
     [time-comp {:template-form template-form
                 :changes       changes
                 :update-key    :update-template-form
                 :modal         start-modal-visible
                 :field-key     :start
                 :label         "Start"}]
     ;; stop
     [time-comp {:template-form template-form
                 :changes       changes
                 :update-key    :update-template-form
                 :modal         stop-modal-visible
                 :field-key     :stop
                 :label         "Stop"}]

     [duration-comp (:start @template-form) (:stop @template-form)]

     [id-comp template-form]

     [created-comp template-form]

     [last-edited-comp template-form]
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
         :delete-item    #(dispatch [:delete-template (:id @template-form)])}])]))
