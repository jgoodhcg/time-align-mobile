(ns time-align-mobile.components.form-buttons
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  button-paper
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]
            [reagent.core :as r :refer [atom]]
            ["react" :as react]))

(defn buttons [{:keys [changed
                       save-changes
                       cancel-changes
                       delete-item
                       compact
                       labels
                       edit-item]}]
  [:<>
   (when (some? edit-item)
     [button-paper
      {:icon     "pencil"
       :mode     "text"
       :compact  compact
       :on-press edit-item} (when labels "Edit")])

   [button-paper {:icon     "delete"
                  :mode     "text"
                  :compact  compact
                  :on-press delete-item}
    (when labels "Delete")]

   [button-paper
    (merge {:icon     "cancel"
            :mode     "text"
            :compact  compact
            :on-press cancel-changes}
           (when-not changed {:disabled true}))
    (when labels "Revert")]

   [button-paper
    (merge {:icon     "content-save"
            :mode     (cond compact "text"
                            changed "contained"
                            :else   "text")
            :compact  compact
            :on-press save-changes}
           (when-not changed {:disabled true}))
    (when labels "Save")]])

(defn root [{:keys [changed save-changes cancel-changes delete-item]}]
  [view {:style {:flex            1
                 :flex-direction  "row"
                 :width           "100%"
                 :height          60
                 :margin-top      4
                 :padding         10
                 :align-items     "space-between"
                 :justify-content "space-between"}}

   (buttons {:changed        changed
             :save-changes   save-changes
             :cancel-changes cancel-changes
             :delete-item    delete-item
             :edit-item      nil
             :labels         true
             :compact        false})])

(defn buttons-md [{:keys [changed save-changes cancel-changes delete-item compact edit-item] :as params}]
  [view {:style {:flex            1
                 :flex-direction  "row"
                 :justify-content "space-between"
                 :width           "100%"
                 :height          64
                 :margin-top      4
                 :padding         8}}
   [buttons params]])
