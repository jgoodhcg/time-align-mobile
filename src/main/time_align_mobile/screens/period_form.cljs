(ns time-align-mobile.screens.period-form
  (:require [time-align-mobile.js-imports :refer [view text]]
            [re-frame.core :refer [subscribe dispatch]]
            ["react" :as react]
            [time-align-mobile.js-imports :refer [view
                                                  keyboard-aware-scroll-view
                                                  text
                                                  text-paper
                                                  surface
                                                  divider
                                                  subheading
                                                  button-paper
                                                  color-lighten
                                                  color-readable-background
                                                  menu
                                                  menu-item
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
                                                              bucket-selection-content
                                                              created-comp
                                                              last-edited-comp
                                                              label-comp
                                                              label-comp-md
                                                              bucket-parent-picker-button
                                                              label-style
                                                              bucket-modal
                                                              changeable-field
                                                              duration-comp
                                                              bucket-parent-picker-comp
                                                              info-field-style
                                                              planned-comp
                                                              data-comp]]
            [reagent.core :as r :refer [atom]]
            [time-align-mobile.styles :refer [field-label-changeable-style
                                              theme
                                              divider-style
                                              field-label-style]]))

(def start-modal (r/atom {:visible false
                          :mode    "date"})) ;; TODO spec type for "date" "time"

(def stop-modal (r/atom {:visible false
                          :mode    "date"})) ;; TODO spec type for "date" "time"

(def bucket-picker-modal (r/atom {:visible false}))

(def compact-menu (r/atom {:visible false}))

(defn time-comp-button [{:keys [modal time field-key]}]
  [:<>
   [button-paper {:on-press #(reset! modal {:visible true
                                            :mode    "time"})
                  :mode     "text"}
    [text (if (some? time)
            (format-time time)
            "Add a time time")]]])

(defn date-time-comp-buttons [period-form changes modal field-key label time]
  [:<>
   ;; Date
   [button-paper {:on-press #(reset! modal {:visible true
                                            :mode    "date"})
                  :style    {:margin-right  4
                             :margin-bottom 4}
                  :mode     "text"
                  :icon     "calendar-range"}
      [text (if (some? time)
              (format-date-day time)
              "Add a time date")]]

     ;; Time
   [time-comp-button {:modal     modal
                      :time time}]])

(defn time-comp [period-form changes modal field-key label]
  (let [time (field-key @period-form)]
    [view {:style info-field-style}
     (changeable-field {:changes changes
                        :field-key field-key}
                       [subheading {:style label-style} label])
     [date-time-comp-buttons period-form changes modal field-key label time]

     ;; modal
     [date-time-picker {:is-visible (:visible @modal)
                        :date       (if (some? time) time (js/Date.))
                        :mode       (:mode @modal)
                        :on-confirm (fn [d]
                                      (println "regular")
                                      (dispatch [:update-period-form {field-key d}])
                                      (reset! modal {:visible false
                                                     :mode    "date"}))
                        :on-cancel  #(reset! modal {:visible false
                                                    :mode    "date"})}]]))

(defn time-comp-compact [period-form changes modal field-key label]
  (let [time (field-key @period-form)]
    [view {:style {:flex-direction  "column"
                   :justify-content "center"
                   :align-items     "flex-start"}}
     (changeable-field {:changes   changes
                        :field-key field-key}
                       [subheading label])
     [time-comp-button {:modal     modal
                        :time      time
                        :field-key field-key}]

     ;; modal
     [date-time-picker {:is-visible (:visible @modal)
                        :date       (if (some? time) time (js/Date.))
                        :mode       (:mode @modal)
                        :on-confirm (fn [d]
                                      (println "compact")
                                      (dispatch [:update-period-form {field-key d}])
                                      (reset! modal {:visible false
                                                     :mode    "date"}))
                        :on-cancel  #(reset! modal {:visible false
                                                    :mode    "date"})}]]))

(defn compact [{:keys [delete-callback
                       save-callback
                       close-callback
                       in-play-element
                       copy-over-callback
                       play-callback]
                :as   params}]

  (let [period-form            (subscribe [:get-period-form])
        update-structured-data (fn [new-data]
                                 (dispatch
                                  [:update-period-form {:data new-data}]))
        changes                (subscribe [:get-period-form-changes])
        changed                (> (count @changes) 0)
        buckets                (subscribe [:get-buckets])]

    [view {:style {:flex            1
                   :width           "100%"
                   :flex-direction  "column"
                   :justify-content "flex-start"
                   :align-items     "flex-start"}}

     ;; top button row
     [view {:flex-direction  "row"
            :justify-content "space-between"
            :align-items     "center"
            :width           "100%"
            :margin-bottom   16
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
                                   (dispatch [:save-period-form (new js/Date)])
                                   (when (and (some? save-callback))
                                     (save-callback)))
                      :mode     "outlined"
                      :disabled (not changed)
                      :icon     "content-save"
                      :style    {:margin-right 8}}
        "save"]
       ;; play
       [button-paper {:on-press #(do (dispatch
                                      [:play-from-period
                                       {:id           (:id @period-form)
                                        :time-started (js/Date.)
                                        :new-id       (random-uuid)}])
                                     play-callback)
                      :color    (-> @period-form
                                    :bucket-color)
                      :disabled (= (:id in-play-element)
                                   (:id @period-form))
                      :mode     "contained"
                      :icon     "play-circle"
                      :style    {:margin-right 8}}
        "play"]
       ;; menu
       [menu {:anchor
              (r/as-element
               [icon-button {:on-press #(reset! compact-menu {:visible true})
                             :icon     "dots-vertical"}])
              :visible    (:visible @compact-menu)
              :on-dismiss #(reset! compact-menu {:visible false})}
        ;; copy over
        [menu-item {:title "copy over"
                    :icon  "content-duplicate"
                    :on-press #(do
                                 (dispatch
                                    [:add-period
                                     {:period
                                      (merge @period-form
                                             {:id      (random-uuid)
                                              :data    {} ;; TODO this will need to be evaled from string in form
                                              :planned (-> @period-form
                                                           :planned
                                                           not)})
                                      :bucket-id (:bucket-id @period-form)}])
                                 (reset! compact-menu {:visible false})
                                 (copy-over-callback))}]
        ;; TODO delete
        [menu-item {:title "delete"
                    :icon  "content-duplicate"
                    :on-press #(println "TODO")}]
        ;; TODO select prev
        [menu-item {:title "select above"
                    :icon  "content-duplicate"
                    :on-press #(println "TODO")}]
        ;; TODO select next
        [menu-item {:title "select below"
                    :icon  "content-duplicate"
                    :on-press #(println "TODO")}]
        ;; TODO clear changes
        [menu-item {:title "clear changes"
                    :icon  "content-duplicate"
                    :on-press #(println "TODO")}]
        ;; TODO edit full
        [menu-item {:title "edit full"
                    :icon  "content-duplicate"
                    :on-press #(println "TODO")}]
        ]]]

     [label-comp-md {:form        period-form
                     :changes     changes
                     :update-key  :update-period-form
                     :compact     true
                     :placeholder "During this I was ..."}]

     [divider {:style divider-style}]

     ;; start /stop/ duration
     [view {:style {:flex-direction "row"
                    :margin-top     8}}
      [icon-button {:icon "clock-outline"} ]
      [time-comp-compact period-form changes start-modal :start "Start"]
      [time-comp-compact period-form changes stop-modal :stop "Stop"]
      [duration-comp (:start @period-form) (:stop @period-form)]]

     [divider {:style divider-style}]

     ;; planning
     [view {:style {:flex-direction "row"
                    :margin-top     8}}
      [icon-button {:icon "floor-plan"} ]
      [view {:style {:margin-right 32}}
       [planned-comp period-form changes :update-period-form]]]]))

(defn root [params]
  (let [period-form            (subscribe [:get-period-form])
        update-structured-data (fn [new-data]
                                 (dispatch
                                  [:update-period-form {:data new-data}]))
        changes                (subscribe [:get-period-form-changes])
        buckets                (subscribe [:get-buckets])]

    [view {:style {:margin-top 16
                   :flex       1}}

     [label-comp period-form changes :update-period-form false]


     [bucket-modal
      buckets
      bucket-picker-modal
      (fn [item] (fn [_]
                   (dispatch
                    [:update-period-form
                     {:bucket-id (:id item)}])
                   (swap! bucket-picker-modal
                          (fn [m] (assoc-in m [:visible] false)))))]

     [bucket-parent-picker-button {:period-form         period-form
                                   :bucket-picker-modal bucket-picker-modal
                                   :changes             changes}]

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
