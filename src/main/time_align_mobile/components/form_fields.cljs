(ns time-align-mobile.components.form-fields
  (:require [time-align-mobile.styles :refer [field-label-changeable-style
                                              modal-style
                                              theme
                                              field-label-style]]
            [re-frame.core :refer [dispatch]]
            [reagent.core :as r :refer [atom]]
            ["react" :as react]
            ["react-native-elements" :as rne]
            ["react-native" :as rn]
            [time-align-mobile.components.list-items :as list-items]
            [time-align-mobile.helpers :refer [ms->hhmm get-duration]]
            [time-align-mobile.js-imports :refer [view
                                                  text
                                                  text-input
                                                  surface
                                                  subheading
                                                  ic
                                                  chip
                                                  badge
                                                  list-section
                                                  text-paper
                                                  list-subheader
                                                  color-readable-background
                                                  modal
                                                  flat-list
                                                  touchable-highlight
                                                  icon-button
                                                  scroll-view
                                                  card
                                                  text-input-paper
                                                  switch-paper
                                                  button-paper
                                                  switch
                                                  picker
                                                  picker-item
                                                  format-date]]
            [time-align-mobile.components.structured-data :refer [structured-data]]))

(defn changeable-field [{:keys [changes field-key]} field]
  (let [badge-size 16]

    [view {:style {:position    "relative"}}
     (when (-> @changes (contains? field-key))
       [view {:style {:position "absolute"
                      :right    (- 0 (/ badge-size 2))
                      :top      (- 0 (/ badge-size 2))}}
        [badge {:size badge-size
                :style
                {:background-color
                 (-> theme :colors :accent-light)
                 :color (-> theme :colors :text-light)}} "!"]])

     field]))

(def field-style {:flex-direction "row"
                  :margin-bottom  20})

(def info-field-style {:padding-left   8
                       :margin-left    16
                       :margin-bottom  2
                       :align-items    "center"
                       :flex-direction "row"})

(def label-style {:margin-right 8})

(defn id-comp [form]
  [view {:style info-field-style}
   [subheading {:style label-style} "ID"]
   [text (str (:id @form))]])

(defn created-comp [form]
  [view {:style info-field-style}
   [subheading {:style label-style} "Created"]
   [text (str (format-date (:created @form)))]])

(defn last-edited-comp [form]
  [view {:style info-field-style}
   [subheading {:style label-style} "last-edited"]
   [text (str (format-date (:last-edited @form)))]])

(defn label-comp
  ([form changes update-key]
   (label-comp form changes update-key false))
  ([form changes update-key compact]
   (label-comp form changes update-key compact ""))
  ([form changes update-key compact placeholder]
   [view {:style {:flex-direction  "column"
                  :height          90
                  :width           (if compact 300 350)
                  :justify-content "center"}}
    (changeable-field {:changes changes :field-key :label} [subheading "Label"])
    [text-input-paper {:label           ""
                       :underline-color (:color (field-label-changeable-style
                                                 changes :label))
                       :dense           true
                       :style           {:margin-bottom 4
                                         :margin-right  16}
                       :default-value   (:label @form)
                       :placeholder     placeholder
                       :on-change-text  (fn [text]
                                          (dispatch [update-key
                                                     {:label text
                                                      :id    (:id @form)}]))}]]))

(defn label-comp-md [{:keys [form changes update-key compact placeholder]}]
  [view {:style {:flex-direction "row"
                 :margin-top     8}}
   [icon-button {:icon "label-outline"}]
   [label-comp form changes update-key compact placeholder]])

(defn data-comp [form changes update-structured-data]
  [view {:style {:flex           1
                 :flex-direction "column"
                 :align-items    "flex-start"}}
   [text {:style (field-label-changeable-style @changes :data)} ":data"]
   [structured-data {:data   (:data @form)
                     :update update-structured-data}]])

(def picker-comp-outer-style
  (merge field-style
         {:flex-direction  "row"
          :padding-bottom  4
          :margin-bottom   4
          :justify-content "space-between"
          :align-items     "center"}))

(def picker-comp-inner-style {:flex-direction "column"
                              :margin-right   8})

(defn parent-picker-base [{:keys [form
                                  changes
                                  entities
                                  field-key
                                  screen-key
                                  disabled
                                  label
                                  update-key
                                  bucket-underline
                                  compact]}]
  [view {:style (merge
                 picker-comp-outer-style
                 (when bucket-underline
                   {:border-bottom-width 8
                    :border-bottom-color (:bucket-color @form)}))}

   [view {:style picker-comp-inner-style}

    (changeable-field {:changes   changes
                       :field-key field-key}
                      [view {:flex-direction "column"}
                       (when (not compact)
                         [subheading label])
                       [card {:style     {:border-radius 4}
                              :elevation 0}
                        [:> rn/Picker {:selected-value  (str (field-key @form))
                                       :style           {:width 250}
                                       :enabled         (not disabled)
                                       :on-value-change #(dispatch
                                                          ;; use uuid because picker works with strings
                                                          [update-key {field-key (uuid %)
                                                                       :id       (:id @form)}])}
                         (map (fn [entity]
                                [picker-item {:label (if (clojure.string/blank? (:label entity))
                                                       (str "no label for "
                                                            (clojure.string/join "" (take 8 (str (:id entity)))))
                                                       (:label entity))
                                              :key   (str (:id entity))
                                              :value (str (:id entity))}])
                              @entities)]]])]

   (when (not compact)
     [button-paper {:icon    "pencil"
                    :mode    "outlined"
                    :compact true
                    :on-press
                    #(dispatch
                      [:navigate-to
                       {:current-screen screen-key
                        :params         {field-key (field-key @form)}}])}])])

(defn bucket-parent-picker-comp [{:keys [form
                                         changes
                                         buckets
                                         update-key
                                         compact]}]

  (parent-picker-base {:form             form
                       :changes          changes
                       :entities         buckets
                       :field-key        :bucket-id
                       :screen-key       :bucket
                       :label            "Bucket"
                       :update-key       update-key
                       :compact          compact
                       :disabled         false
                       :bucket-underline true}))

(defn pattern-parent-picker-comp [form changes patterns update-key disabled]

  (parent-picker-base {:form             form
                       :changes          changes
                       :entities         patterns
                       :field-key        :pattern-id
                       :screen-key       :pattern
                       :label            "Pattern"
                       :update-key       update-key
                       :compact          false
                       :bucket-underline false
                       :disabled         disabled}))

(defn planned-comp [form changes update-key]
  [view {:style {:flex-direction  "column"
                 :padding         2
                 :justify-content "flex-start"
                 :align-items     "flex-start"}}
   (changeable-field {:changes   changes
                      :field-key :planned}
                     [subheading "Planned"])
   [view {:style {:margin-left 16}}
    [switch-paper {:value           (:planned @form)
                   :on-value-change #(dispatch [update-key {:planned %}])}]]])

(defn planned-md [form changes update-key]
  [view {:style {:flex-direction "row"
                 :margin-top     8}}
   [icon-button {:icon "view-dashboard-outline"} ]
   [view {:style {:margin-right 32}}
    [planned-comp form changes update-key]]])

(defn duration-comp [start stop]
  (let [duration (get-duration {:start start :stop stop})]

    [view {:style (merge info-field-style
                         {:flex-direction "column"})}
     [subheading {:style label-style} "Duration"]
     [text-paper (if-some [d duration]
                   (ms->hhmm d)
                   "No duration")]]))

(defn filter-button [pattern-form on-press]
  [button-paper {:icon     "filter-variant"
                 :mode     "contained"
                 :on-press on-press}
   "Add filter"])

(defn bucket-selection-content [{:keys [buckets-atom
                                        on-press-generator
                                        modal-atom]}]
  [surface {:style modal-style}

   [icon-button
    {:icon     "close"
     :size     20
     :on-press #(swap! modal-atom (fn [m] (assoc-in m [:visible] false)))}]

   [scroll-view
    [list-subheader "What are you about to do?"]
    [list-section
     (->> @buckets-atom
          (map (fn [item]
                 (list-items/bucket
                  (merge
                   item
                   {:on-press (on-press-generator item)})))))]]])

(defn bucket-modal [buckets modal-atom on-press-generator]
  [modal {:animation-type   "slide"
          :transparent      true
          :on-request-close #(swap! modal-atom
                                    (fn [m] (assoc-in m [:visible] false)))
          :visible          (:visible @modal-atom)}

   [bucket-selection-content {:buckets-atom       buckets
                              :on-press-generator on-press-generator
                              :modal-atom         modal-atom}]])

(defn bucket-parent-picker-button [{:keys [form
                                           bucket-picker-modal
                                           changes]}]
  [view {:style {:flex-direction "row"
                 :margin-top     8}}
   [icon-button {:icon "google-circles-communities"}]
   [view {:style {:flex-direction  "column"
                  :justify-content "center"}}
    (changeable-field {:changes   changes
                       :field-key :bucket-id}
                      [subheading {:style label-style} "Group"])
    [button-paper {:on-press
                   #(swap! bucket-picker-modal
                           (fn [m] (assoc-in m [:visible] true)))

                   :color (-> @form
                              :bucket-color)
                   :mode  "outlined"}
     [text (:bucket-label @form)]]]])
