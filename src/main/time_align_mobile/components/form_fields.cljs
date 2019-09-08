(ns time-align-mobile.components.form-fields
  (:require [time-align-mobile.styles :refer [field-label-changeable-style
                                              theme
                                              field-label-style]]
            [re-frame.core :refer [dispatch]]
            [reagent.core :as r :refer [atom]]
            ["react" :as react]
            ["react-native-elements" :as rne]
            ["react-native" :as rn]
            [time-align-mobile.helpers :refer [ms->hhmm]]
            [time-align-mobile.js-imports :refer [view
                                                  text
                                                  text-input
                                                  surface
                                                  subheading
                                                  ic
                                                  chip
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
  [view {:flex-direction "row"}
   [view {:style {:width           16
                  :margin-right    4
                  :justify-content "center"
                  :align-items     "center"}}
    (when (-> @changes (contains? field-key))
      [ic {:name  "ios-alert"
           :size  16
           :color (get-in theme [:colors :primary])}])]
   field])

(def field-style {:flex-direction "row"
                  :margin-bottom  20})

(def info-field-style {:padding-left   8
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
   (changeable-field
    {:changes changes
     :field-key :label}
    [text-input-paper {:label           ""
                       :underline-color (:color (field-label-changeable-style
                                                 changes :label))
                       :dense           true
                       :style           (merge {:margin-bottom 4}
                                               (if compact
                                                 {:width "85%"}
                                                 {:width "100%"}))
                       :default-value   (:label @form)
                       :placeholder     "Label"
                       :on-change-text  (fn [text]
                                          (dispatch [update-key
                                                     {:label text
                                                      :id (:id @form)}]))}])))

(defn data-comp [form changes update-structured-data]
  [view {:style {:flex           1
                 :flex-direction "column"
                 :align-items    "flex-start"}}
   [text {:style (field-label-changeable-style @changes :data)} ":data"]
   [structured-data {:data   (:data @form)
                     :update update-structured-data}]])

(def picker-comp-outer-style
  (merge field-style
         {:flex-direction      "row"
          :padding-bottom      4
          :justify-content     "space-between"
          :align-items         "center"}))

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
                                       :style           (if compact
                                                          {:width 120}
                                                          {:width 250})
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
     [button-paper {:icon    "edit"
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
  [view {:style {:flex-direction  "row"
                 :padding         8
                 :justify-content "flex-start"
                 :align-items     "flex-start"}}
   (changeable-field {:changes   changes
                      :field-key :planned}
                     [subheading {:style label-style} "Planned"]                  )
   [switch-paper {:value           (:planned @form)
                  :on-value-change #(dispatch [update-key {:planned %}])}]])

(defn duration-comp [start stop]
  (let [duration (if (and (inst? start)
                          (inst? stop))
                   (->> (.valueOf start)
                        (- (.valueOf stop))
                        ms->hhmm)
                   "no duration")]

    [view {:style info-field-style}
     [subheading {:style label-style} "Duration"]
     [text duration]]))

(defn filter-button [pattern-form on-press]
  [button-paper {:icon     "filter-list"
                 :mode     "contained"
                 :on-press on-press}
   "Add filter"])
