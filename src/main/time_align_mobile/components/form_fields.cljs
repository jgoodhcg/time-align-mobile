(ns time-align-mobile.components.form-fields
  (:require [time-align-mobile.styles :refer [field-label-changeable-style
                                              field-label-style]]
            [re-frame.core :refer [dispatch]]
            [reagent.core :as r :refer [atom]]
            ["react" :as react]
            ["react-native-elements" :as rne]
            ["react-native" :as rn]
            [time-align-mobile.js-imports :refer [view
                                                  text
                                                  text-input
                                                  switch
                                                  picker
                                                  picker-item
                                                  format-date]]
            [time-align-mobile.components.structured-data :refer [structured-data]]))

(def field-style {:flex-direction "row"
                  :margin-bottom  20})

(defn id-comp [form]
  [view {:style field-style}
   [:> rne/Input {:label "ID"
                  :value (str (:id @form))
                  :editable false}]])

(defn created-comp [form]
  [view {:style field-style}
   [:> rne/Input {:label "Created"
                  :value (str (:created @form))
                  :editable false}]])

(defn last-edited-comp [form]
  [view {:style field-style}
   [:> rne/Input {:label "Last Edited"
                  :value (str (:last-edited @form))
                  :editable false}]])

(defn label-comp [form changes update-key]
  [view {:style field-style}
   [:> rne/Input {:label          "Label"
                  :label-style    (field-label-changeable-style changes :label)
                  :default-value  (:label @form)
                  :spell-check    true
                  :on-change-text (fn [text]
                                    (dispatch [update-key
                                               {:label text}]))}]])

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

(defn bucket-parent-picker-comp [form changes buckets update-key]
  [view {:style (merge field-style {:flex-direction  "column"
                                    :padding         8
                                    :justify-content "flex-start"
                                    :align-items     "flex-start"})}
   [:> rne/Text {:h4 true :h4-style
                 (merge
                  (field-label-changeable-style @changes :bucket-label)
                  {:font-size 16})} "Bucket Label"]
   [:> rn/Picker {:selected-value  (str (:bucket-id @form))
                  :style           {:width 250}
                  :on-value-change #(dispatch [update-key {:bucket-id %}])}
    (map (fn [bucket] [picker-item {:label (:label bucket)
                                    :key   (:id bucket)
                                    :value (:id bucket)}])
         @buckets)]])

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
                  :on-value-change #(dispatch [update-key {:pattern-id %}])}
    (map (fn [pattern] [picker-item {:label (:label pattern)
                                    :key   (:id pattern)
                                    :value (:id pattern)}])
         @patterns)]])

(defn planned-comp [form changes update-key]
  [view {:style (merge field-style {:flex-direction  "column"
                                    :padding         8
                                    :justify-content "flex-start"
                                    :align-items     "flex-start"})}
   [:> rne/Text {:h4 true :h4-style (merge
                                     (field-label-changeable-style @changes :planned)
                                     {:font-size 16})} "Planned"]
   [switch {:value (:planned @form)
            :on-value-change #(dispatch [update-key {:planned %}])}]])
