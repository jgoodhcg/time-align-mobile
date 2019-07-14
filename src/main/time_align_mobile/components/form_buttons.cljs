(ns time-align-mobile.components.form-buttons
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  button-paper
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]
            [reagent.core :as r :refer [atom]]
            ["react" :as react]))

(defn buttons [{:keys [changed save-changes cancel-changes delete-item]}]
  [:<>
   [button-paper
    (merge {:icon     "save"
            :mode     "outlined"
            :on-press save-changes}
           (when-not changed {:disabled true}))
    "Save"]

   [button-paper
    (merge {:icon     "cancel"
            :mode     "outlined"
            :on-press cancel-changes}
           (when-not changed {:disabled true}))
    "Revert"]

   [button-paper {:icon     "delete"
                  :mode     "outlined"
                  :on-press delete-item}
    "Delete"]])

(defn root [{:keys [changed save-changes cancel-changes delete-item]}]
  [view {:style {:flex            1
                 :flex-direction  "row"
                 :width           "100%"
                 :margin-top      4
                 :padding         10
                 :align-items     "space-between"
                 :justify-content "space-between"}}

   (buttons {:changed changed
             :save-changes save-changes
             :cancel-changes cancel-changes
             :delete-item delete-item})])

