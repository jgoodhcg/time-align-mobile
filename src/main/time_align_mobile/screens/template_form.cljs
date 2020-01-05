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
                                                  menu
                                                  menu-item
                                                  date-time-picker
                                                  subheading
                                                  modal
                                                  divider
                                                  icon-button
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
                                                              planned-comp
                                                              planned-md
                                                              label-comp
                                                              label-comp-md
                                                              bucket-parent-picker-button
                                                              bucket-modal
                                                              label-style
                                                              start-stop-compact
                                                              pattern-parent-picker-comp
                                                              bucket-parent-picker-comp
                                                              changeable-field
                                                              info-field-style
                                                              planned-comp
                                                              data-comp]]
            [reagent.core :as r :refer [atom]]
            [time-align-mobile.helpers :as helpers]
            [time-align-mobile.styles :refer [field-label-changeable-style
                                              field-label-style
                                              divider-style]]))

(def start-modal-visible (r/atom {:visible false}))

(def stop-modal-visible (r/atom {:visible false}))

(def compact-menu (r/atom {:visible false}))

(def bucket-picker-modal (r/atom {:visible false})) ;; TODO refactor to bucket-picker-modal-atom

(defn time-comp-buttons [time modal form update-key field-key]
  [:<>
   [button-paper {:on-press #(reset! modal {:visible true})
                  :mode     "outlined"
                  :icon     "clock-outline"}
    [text (if (some? time)
            (format-time time)
            "Add a time time")]]
   [date-time-picker {:is-visible (:visible @modal)
                      :date       time
                      :mode       "time"
                      :on-confirm (fn [d]
                                    (dispatch
                                     [update-key {field-key (helpers/get-ms d)
                                                  :id    (:id @form)}])
                                    (reset! modal {:visible false}))
                      :on-cancel  #(reset! modal {:visible false})}]])

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

(defn top-button-row [{:keys [close-callback template-form pixel-to-minute-ratio scroll-to changed]}]
  [view {:flex-direction  "row"
         :justify-content "space-between"
         :align-items     "center"
         :width           "100%"
         :padding-left    16
         :padding-right   16}
      ;; close
   [icon-button {:icon     "close"
                 :size     20
                 :on-press close-callback}]
   [view {:flex-direction "row"
          :align-items    "center"}
       ;; save
    [button-paper {:on-press #(do
                                (dispatch [:update-template-on-pattern-planning-form
                                           ;; TODO figure out something else if data is ever needed here
                                           (dissoc @template-form :data)]))
                   :mode     "outlined"
                   :disabled (not changed)
                   :icon     "content-save"
                   :style    {:margin-right 8}}
        "save"]

       ;; menu
    [menu {:anchor
           (r/as-element
            [icon-button {:on-press #(reset! compact-menu {:visible true})
                          :icon     "dots-vertical"}])
           :visible    (:visible @compact-menu)
           :on-dismiss #(reset! compact-menu {:visible false})}
        ;; select prev
     [menu-item {:title    "select above"
                 :icon     "chevron-up"
                 :on-press #(do
                              (dispatch [:select-next-or-prev-template-in-form :prev])
                              (reset! compact-menu {:visible false}))}]
        ;; select next
     [menu-item {:title    "select below"
                 :icon     "chevron-down"
                 :on-press #(do
                              (dispatch [:select-next-or-prev-template-in-form :next])
                              (reset! compact-menu {:visible false}))}]
        ;; jump to
     [menu-item {:title    "jump to"
                 :icon     "swap-vertical"
                 :on-press #(do
                              (scroll-to (->> @template-form
                                              :start
                                              ((fn [time-stamp]
                                                 (helpers/element-time-stamp-info
                                                  time-stamp
                                                  pixel-to-minute-ratio
                                                  (js/Date.))))
                                              :y-pos))
                              (reset! compact-menu {:visible false}))}]
        ;; copy over
     [menu-item {:title    "copy over"
                 :icon     "content-duplicate"
                 :on-press #(do
                              (dispatch
                               [:add-new-template-to-planning-form
                                (merge @template-form
                                       {:id      (random-uuid)
                                        :data    {} ;; TODO this will need to be evaled from string in form
                                        :planned (-> @template-form
                                                     :planned
                                                     not)
                                        :now     (js/Date.)})])
                              (reset! compact-menu {:visible false})
                              (close-callback))}]
        ;; clear changes
     [menu-item {:title    "clear changes"
                 :icon     "cancel"
                 :on-press #(do (dispatch
                                 [:load-template-form-from-pattern-planning
                                  (:id @template-form)])
                                (reset! compact-menu {:visible false}))}]
        ;; edit full
     [menu-item {:title    "edit full"
                 :icon     "pencil"
                 :on-press #(do
                              (dispatch
                               [:navigate-to
                                {:current-screen :template
                                 :params
                                 {:template-id             (:id @template-form)
                                  :pattern-form-pattern-id (:pattern-id @template-form)}}])
                              (reset! compact-menu {:visible false})
                              (close-callback))}]
        ;; delete
     [menu-item {:title    "delete"
                 :icon     "delete"
                 :on-press #(do
                              (dispatch [:delete-template-from-pattern-planning
                                         (:id @template-form)])
                              (reset! compact-menu {:visible false})
                              (close-callback))}]]]])

(defn compact
  "This comp looks at the template form (similar to period-form and period) but saves to the *pattern form* NOT templates on the pattern in the app-db"
  [{:keys [close-callback scroll-to]}]

  (let [pattern-form          (subscribe [:get-pattern-form])
        template-form         (subscribe [:get-template-form])
        template-form-changes (subscribe [:get-template-form-changes-from-pattern-planning])
        changed               (> (count @template-form-changes) 0)
        buckets               (subscribe [:get-buckets])
        patterns              (subscribe [:get-patterns])
        px-ratio-config       (subscribe [:get-pixel-to-minute-ratio])
        pixel-to-minute-ratio (:current @px-ratio-config)]

    [view {:style {:flex            1
                   :width           "100%"
                   :flex-direction  "column"
                   :justify-content "space-between"
                   :align-items     "flex-start"}}

     [top-button-row {:close-callback        close-callback
                      :scroll-to             scroll-to
                      :template-form         template-form
                      :pixel-to-minute-ratio pixel-to-minute-ratio
                      :changed               changed}]

     [label-comp-md {:form        template-form
                     :changes     template-form-changes
                     :update-key  :update-template-form
                     :compact     true
                     :placeholder "During this time I will ..."}]

     [divider {:style divider-style}]

     [bucket-modal
      buckets
      bucket-picker-modal
      (fn [item] (fn [_]
                   (dispatch
                    [:update-template-form
                     {:bucket-id (:id item)}])
                   (swap! bucket-picker-modal
                          (fn [m] (assoc-in m [:visible] false)))))]

     [bucket-parent-picker-button {:form                template-form
                                   :bucket-picker-modal bucket-picker-modal
                                   :changes             template-form-changes}]

     [divider {:style divider-style}]

     [start-stop-compact {:form        template-form
                          :changes     template-form-changes
                          :start-modal start-modal-visible
                          :stop-modal  stop-modal-visible
                          :update-key  :update-template-form}]

     [divider {:style divider-style}]

     ;; planning
     [planned-md template-form template-form-changes :update-template-form]]))

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

     [planned-comp template-form changes :update-template-form]

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
