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
                                                  divider
                                                  subheading
                                                  modal
                                                  icon-button
                                                  surface
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
            [time-align-mobile.components.top-bar :refer [top-bar]]
            [time-align-mobile.components.form-fields :refer [id-comp
                                                              created-comp
                                                              last-edited-comp
                                                              duration-comp
                                                              planned-comp
                                                              planned-md
                                                              label-comp
                                                              label-comp-md
                                                              label-style
                                                              pattern-parent-picker-comp
                                                              bucket-parent-picker-button
                                                              bucket-modal
                                                              bucket-parent-picker-comp
                                                              changeable-field
                                                              info-field-style
                                                              planned-comp
                                                              data-comp]]
            [reagent.core :as r :refer [atom]]
            [time-align-mobile.helpers :as helpers]
            [time-align-mobile.styles :as styles :refer [field-label-changeable-style
                                                         divider-style
                                                         field-label-style]]))

(def bucket-picker-modal (r/atom {:visible false})) ;; TODO refactor to bucket-picker-modal-atom (in period form too)

(def start-modal-visible (r/atom false))

(def stop-modal-visible (r/atom false))

(def start-modal (r/atom {:visible false
                          :mode    "date"})) ;; TODO spec type for "date" "time"

(def stop-modal (r/atom {:visible false
                         :mode    "date"})) ;; TODO spec type for "date" "time"

(def compact-menu (r/atom {:visible false}))

(defn time-comp-buttons [time modal form update-key field-key]
  [:<>
   [button-paper {:on-press #(reset! modal true)
                  :mode     "outlined"
                  :icon     "clock-outline"}
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

;; TODO DRY (check period form)
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
                       [subheading {:style styles/form-heading} label])
     [time-comp-buttons time-as-date modal template-form update-key field-key]]))

;; TODO DRY (check period form)
(defn time-comp-button [{:keys [modal time field-key]}]
  [:<>
   [button-paper {:on-press #(reset! modal {:visible true
                                            :mode    "time"})
                  :style    {:margin-bottom 8}
                  :mode     "text"}
    [text (if (some? time)
            (format-time time)
            "Add a time time")]]])

;; TODO DRY (check period form)
(defn time-comp-compact [form changes modal field-key label]
  (let [time (-> @form
                 (field-key)
                 (helpers/reset-relative-ms (js/Date.)))]
    [view {:style {:flex-direction  "column"
                   :justify-content "flex-start"
                   :margin-right    8
                   :align-items     "flex-start"}}
     (changeable-field {:changes   changes
                        :field-key field-key}
                       [subheading {:style styles/form-heading} label])
     [time-comp-button {:modal     modal
                        :time      time
                        :field-key field-key}]

     ;; modal
     [date-time-picker {:is-visible (:visible @modal)
                        :date       (if (some? time) time (js/Date.))
                        :mode       (:mode @modal)
                        :on-confirm (fn [d]
                                      (println "compact")
                                      (dispatch [:update-template-form {field-key (helpers/get-ms d)}])
                                      (reset! modal {:visible false
                                                     :mode    "date"}))
                        :on-cancel  #(reset! modal {:visible false
                                                    :mode    "date"})}]]))

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
                   :mode     "text"
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
                   :justify-content "flex-start"
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

     ;; start /stop/ duration
     [view {:style {:flex-direction "row"
                    :align-items    "center"
                    :flex           1
                    :margin-top     8}}
      [icon-button {:icon "clock-outline"
                    :color (->> styles/theme :colors :disabled)} ]
      [time-comp-compact template-form template-form-changes start-modal :start "Start"]
      [time-comp-compact template-form template-form-changes stop-modal :stop "Stop"]
      [view {:style {:flex         1
                     :justify-self "flex-end"}}
       [duration-comp
        (-> @template-form
            (:start)
            (helpers/reset-relative-ms (js/Date.)))
        (-> @template-form
            (:stop)
            (helpers/reset-relative-ms (js/Date.)))]]]

     [divider {:style divider-style}]

     [planned-md template-form template-form-changes :update-template-form]]))

;; This is effectively deprecated (removed the last link to it via "edit full")
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

    [view {:style {:flex 1}}
     [top-bar {:center-content [subheading "Template Form"]
               :right-content  [icon-button]}]

     [surface {:style {:flex       1
                       :margin-top 4}}
      [view {:flex            1
             :flex-direction  "column"
             :justify-content "flex-start"
             :align-items     "flex-start"
             :padding         4}

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
         :delete-item    #(dispatch [:delete-template (:id @template-form)])}])]]]))
