(ns time-align-mobile.components.form-buttons
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  touchable-highlight]]
            ["react" :as react]))

(defn root [{:keys [changed save-changes cancel-changes delete-item]}]
  [view {:style {:flex            1
                 :flex-direction  "row"
                 :align-items     "center"
                 :justify-content "center"}}
   [touchable-highlight {:on-press save-changes
                         :style    {:padding      5
                                    :margin-right 10}}
    [text (when-not changed {:style {:color "grey"}}) "save"]]

   [touchable-highlight {:on-press cancel-changes
                         :style    {:padding      5
                                    :margin-right 10}}
    [text (when-not changed {:style {:color "grey"}}) "cancel"]]

   [touchable-highlight {:on-press delete-item
                         :style {:padding 5
                                 :margin-right 10}}
    [text "delete"]]])

