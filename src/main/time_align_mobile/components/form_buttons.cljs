(ns time-align-mobile.components.form-buttons
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  button-paper
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]
            [reagent.core :as r :refer [atom]]
            ["react" :as react]))

(defn buttons [{:keys [changed save-changes cancel-changes delete-item compact edit-item]}]
  [:<>
   (when (some? edit-item)
     [button-paper
      {:icon    "pencil"
       :mode    "text"
       :compact compact
       :on-press edit-item}])

   [button-paper
    (merge {:icon     "content-save"
            :mode     (if compact "text" "outlined")
            :compact  compact
            :on-press save-changes}
           (when-not changed {:disabled true}))
    (when (not compact) "Save")]

   [button-paper
    (merge {:icon     "cancel"
            :mode     (if compact "text" "outlined")
            :compact  compact
            :on-press cancel-changes}
           (when-not changed {:disabled true}))
    (when (not compact) "Revert")]

   [button-paper {:icon     "delete"
                  :mode     (if compact "text" "outlined")
                  :compact  compact
                  :on-press delete-item}
    (when (not compact) "Delete")]])

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
             :compact        false})])

