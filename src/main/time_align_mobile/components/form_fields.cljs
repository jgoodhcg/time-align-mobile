(ns time-align-mobile.components.form-fields
  (:require [time-align-mobile.styles :refer [field-label-changeable-style
                                              field-label-style]]
            [re-frame.core :refer [dispatch]]
            ["react" :as react]
            [time-align-mobile.js-imports :refer [view
                                                  text
                                                  text-input
                                                  switch
                                                  picker
                                                  picker-item
                                                  format-date]]
            [time-align-mobile.components.structured-data :refer [structured-data]]))

(defn id-comp [form]
  [view {:style {:flex-direction "row"}}
   [text {:style field-label-style} ":id"]
   [text (str (:id @form))]])

(defn created-comp [form]
  [view {:style {:flex-direction "row"}}
   [text {:style field-label-style} ":created"]
   [text (format-date (:created @form))]])

(defn last-edited-comp [form]
  [view {:style {:flex-direction "row"}}
   [text {:style field-label-style} ":last-edited"]
   [text (format-date (:last-edited @form))]])

(defn label-comp [form changes update-key]
  [view {:style {:flex-direction "row"
                 :align-items    "center"}}
   [text {:style (field-label-changeable-style changes :label)} ":label"]
   [text-input {:default-value  (:label @form)
                :style          {:height 40
                                 :width  200}
                :spell-check    true
                :on-change-text (fn [text]
                                  (dispatch [update-key
                                             {:label text}]))}]])

(defn data-comp [form changes update-structured-data]
  [view {:style {:flex           1
                 :flex-direction "column"
                 :align-items    "flex-start"}}
   [text {:style (field-label-changeable-style changes :data)} ":data"]
   [structured-data {:data   (:data @form)
                     :update update-structured-data}]])

(defn parent-id-comp [form changes]
  [view {:style {:flex-direction "row"}}
   [text {:style (field-label-changeable-style changes :bucket-id)}
    ":bucket-id"]
   [text (str (:bucket-id @form))]])

(defn parent-picker-comp [form changes buckets update-key]
  [view {:style {:flex-direction "row"
                 :align-items "center"}}
   [text {:style (field-label-changeable-style changes :bucket-label)}
    ":bucket-label"]
   [picker {:selected-value  (:bucket-id @form)
            :style           {:width 250}
            :on-value-change #(dispatch [update-key {:bucket-id %}])}
    (map (fn [bucket] [picker-item {:label (:label bucket)
                                    :key (:id bucket)
                                    :value (:id bucket)}])
         @buckets)]])

(defn planned-comp [form changes update-key]
  [view {:style {:flex-direction "row"
                 :align-items    "center"}}
   [text {:style (field-label-changeable-style changes :planned)} ":planned"]
   [switch {:value (:planned @form)
            :on-value-change #(dispatch [update-key {:planned %}])}]])
