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
            [time-align-mobile.helpers :refer [element-time-stamp-info]]
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
                  :style    {:margin-bottom 8}
                  :mode     "outlined"}
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
                  :mode     "outlined"}
      [text (if (some? time)
              (format-date-day time)
              "Add a time date")]]

     ;; Time
   [time-comp-button {:modal     modal
                      :time time}]])

(defn time-comp [period-form changes modal field-key label]
  (let [time (field-key @period-form)]
    [view {:style {:flex-direction  "column"
                   :justify-content "flex-start"
                   :align-items     "flex-start"}}
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
                   :justify-content "flex-start"
                   :margin-right    8
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

(defn compact [{:keys [scroll-to
                       delete-callback
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
        displayed-day          (subscribe [:get-day-time-navigator])
        px-ratio-config        (subscribe [:get-pixel-to-minute-ratio])
        pixel-to-minute-ratio  (:current @px-ratio-config)
        changed                (> (count @changes) 0)
        buckets                (subscribe [:get-buckets])
        playing                (= (:id in-play-element)
                                  (:id @period-form))]

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
                                   (save-callback))
                      :mode     "outlined"
                      :disabled (not changed)
                      :icon     "content-save"
                      :style    {:margin-right 8}}
        "save"]
       ;; play
       [button-paper {:on-press #(do
                                   (if playing
                                     (dispatch
                                      [:stop-playing-period])
                                     (dispatch
                                      [:play-from-period
                                       {:id           (:id @period-form)
                                        :time-started (js/Date.)
                                        :new-id       (random-uuid)}]))
                                   (play-callback))
                      :color    (-> @period-form
                                    :bucket-color)
                      :mode     (if playing "outlined" "contained")
                      :icon     "play-circle"
                      :style    {:margin-right 8}}
        (if playing "stop" "play")]
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
                                 (dispatch [:select-next-or-prev-period :prev])
                                 (reset! compact-menu {:visible false}))}]
        ;; select next
        [menu-item {:title    "select below"
                    :icon     "chevron-down"
                    :on-press #(do
                                 (dispatch [:select-next-or-prev-period :next])
                                 (reset! compact-menu {:visible false}))}]
        ;; jump to
        [menu-item {:title    "jump to"
                    :icon     "swap-vertical"
                    :on-press #(do
                                 (scroll-to (->> @period-form
                                                 :start
                                                 ((fn [time-stamp]
                                                    (element-time-stamp-info
                                                     time-stamp
                                                     pixel-to-minute-ratio
                                                     @displayed-day)))
                                                 :y-pos))
                                 (reset! compact-menu {:visible false}))}]
        ;; copy over
        [menu-item {:title    "copy over"
                    :icon     "content-duplicate"
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
        ;; clear changes
        [menu-item {:title    "clear changes"
                    :icon     "cancel"
                    :on-press #(do
                                 (dispatch
                                  [:load-period-form
                                   {:period-id (:id @period-form)
                                    :bucket-id (:bucket-id @period-form)}])
                                 (reset! compact-menu {:visible false}))}]
        ;; edit full
        [menu-item {:title    "edit full"
                    :icon     "pencil"
                    :on-press #(do
                                 (dispatch
                                  [:navigate-to
                                   {:current-screen :period
                                    :params
                                    {:period-id (:id @period-form)
                                     :bucket-id (:bucket-id @period-form)}}])
                                 (reset! compact-menu {:visible false}))}]
        ;; delete
        [menu-item {:title    "delete"
                    :icon     "delete"
                    :on-press #(do
                                 (dispatch [:delete-period {:period-id (:id @period-form)
                                                            :bucket-id (:bucket-id @period-form)}])
                                 (when (and (some? delete-callback))
                                   (reset! compact-menu {:visible false})
                                   (delete-callback)))}]]]]

     [label-comp-md {:form        period-form
                     :changes     changes
                     :update-key  :update-period-form
                     :compact     true
                     :placeholder "During this time I was ..."}]

     [divider {:style divider-style}]

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

     [divider {:style divider-style}]

     ;; start /stop/ duration
     [view {:style {:flex-direction "row"
                    :align-items    "center"
                    :flex           1
                    :margin-top     8}}
      [icon-button {:icon "clock-outline"} ]
      ;; [time-comp period-form changes start-modal :start "Start"]
      ;; [time-comp period-form changes stop-modal :stop "Stop"]
      [time-comp-compact period-form changes start-modal :start "Start"]
      [time-comp-compact period-form changes stop-modal :stop "Stop"]
      [view {:style {:flex         1
                     :justify-self "flex-end"}}
       [duration-comp (:start @period-form) (:stop @period-form)]]]

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
