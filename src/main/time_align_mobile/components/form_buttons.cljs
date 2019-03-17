(ns time-align-mobile.components.form-buttons
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]
            [reagent.core :as r :refer [atom]]
            ["react" :as react]))

(defn root [{:keys [changed save-changes cancel-changes delete-item]}]
  [view {:style {:flex            1
                 :flex-direction  "row"
                 :width           "100%"
                 :margin-top      4
                 :padding         10
                 :align-items     "space-between"
                 :justify-content "space-between"}}

   [:> rne/Button
    (merge {:icon     (r/as-element [:> rne/Icon {:name  "save"
                                                  :type  "font-awesome"
                                                  :color "#fff"}])
            :on-press save-changes}
           (when-not changed {:disabled true}))]

   [:> rne/Button
    (merge {:icon     (r/as-element [:> rne/Icon {:name  "cancel"
                                                  :type  "material"
                                                  :color "#fff"}])
            :on-press cancel-changes}
           (when-not changed {:disabled true}))]

   [:> rne/Button {:icon     (r/as-element [:> rne/Icon {:name  "delete-forever"
                                                         :type  "material-community"
                                                         :color "#fff"}])
                   :on-press delete-item}]])

