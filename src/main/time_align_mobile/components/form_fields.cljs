(ns time-align-mobile.components.form-fields
  (:require [time-align-mobile.styles :refer [field-label-changeable-style
                                              theme
                                              field-label-style]]
            [re-frame.core :refer [dispatch]]
            [reagent.core :as r :refer [atom]]
            ["react" :as react]
            ["react-native-elements" :as rne]
            ["react-native" :as rn]
            [time-align-mobile.js-imports :refer [view
                                                  text
                                                  text-input
                                                  surface
                                                  subheading
                                                  ic
                                                  chip
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
                                                     {:label text}]))}])))

(defn data-comp [form changes update-structured-data]
  [view {:style {:flex           1
                 :flex-direction "column"
                 :align-items    "flex-start"}}
   [text {:style (field-label-changeable-style @changes :data)} ":data"]
   [structured-data {:data   (:data @form)
                     :update update-structured-data}]])

(defn bucket-parent-id-comp [form changes]
  [view {:style field-style}
   [:> rne/Input {:label          "Bucket ID"
                  :label-style    (field-label-changeable-style @changes :bucket-id)
                  :value  (str (:bucket-id @form))
                  :editable false}]])

(defn pattern-parent-id-comp [form changes]
  [view {:style field-style}
   [:> rne/Input {:label          "Pattern ID"
                  :label-style    (field-label-changeable-style @changes :pattern-id)
                  :value  (str (:pattern-id @form))
                  :editable false}]])

(defn bucket-parent-picker-comp [{:keys [form changes buckets update-key compact]}]
  [view {:style (merge field-style
                       {:flex-direction  "row"
                        :padding         8
                        :border-bottom-color (:bucket-color @form)
                        :border-bottom-width 8
                        :padding-top     24
                        :justify-content "space-between"
                        :align-items     "center"})}

   [view {:style {:flex-direction "column"
                  :margin-right   8}}

    (changeable-field {:changes changes
                       :field-key :bucket-id}
                      [view {:flex-direction "column"}
                       (when (not compact)
                         [subheading "Bucket"])
                       [surface {:style {:border-radius 4}
                                 :elevation 1}
                        [:> rn/Picker {:selected-value  (str (:bucket-id @form))
                                       :style           (if compact
                                                          {:width 150}
                                                          {:width 250})
                                       :on-value-change #(dispatch
                                                          ;; use uuid because picker works with strings
                                                          [update-key {:bucket-id (uuid %)}])}
                         (map (fn [bucket]
                                [picker-item {:label (:label bucket)
                                              :key   (str (:id bucket))
                                              :value (str (:id bucket))}])
                              @buckets)]]])]

   (when (not compact)
     [button-paper {:icon    "edit"
                    :mode    "outlined"
                    :compact true
                    :on-press
                    #(dispatch
                      [:navigate-to
                       {:current-screen :bucket
                        :params         {:bucket-id (:bucket-id @form)}}])}])])

(defn pattern-parent-picker-comp [form changes patterns update-key]
  [view {:style (merge field-style {:flex-direction  "column"
                                    :padding         8
                                    :justify-content "flex-start"
                                    :align-items     "flex-start"})}
   [:> rne/Text {:h4 true :h4-style
                 (merge
                  (field-label-changeable-style @changes :pattern-label)
                  {:font-size 16})} "Pattern Label"]
   [:> rn/Picker {:selected-value  (str (:pattern-id @form))
                  :style           {:width 250}
                  :on-value-change #(dispatch [update-key {:pattern-id (uuid %)}])}
    (map (fn [pattern] [picker-item {:label (:label pattern)
                                     :key   (str (:id pattern))
                                     :value (str (:id pattern))}])
         @patterns)]])

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
