(ns time-align-mobile.components.form-buttons
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]
            ["react" :as react]))

(defn root [{:keys [changed save-changes cancel-changes delete-item]}]
  [view {:style {:flex            1
                 :flex-direction  "row"
                 :width           "100%"
                 :margin-top      10
                 :padding         10
                 :align-items     "space-between"
                 :justify-content "space-between"}}

   [:> rne/Button
    (merge {:title    "save"
            :on-press save-changes}
           (when-not changed {:disabled true}))]

   [:> rne/Button
    (merge {:title    "cancel"
            :on-press cancel-changes}
           (when-not changed {:disabled true}))]

   [:> rne/Button {:title    "delete"
                   :on-press delete-item}]])

