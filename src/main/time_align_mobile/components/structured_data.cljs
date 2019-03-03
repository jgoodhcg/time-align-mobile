(ns time-align-mobile.components.structured-data
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  text-input
                                                  touchable-highlight
                                                  data-font-family
                                                  switch
                                                  alert]]
            [reagent.core :as r :refer [atom]]
            ["react" :as react]
            [re-frame.core :refer [subscribe dispatch]]))

(defn structured-data [{:keys [data update]}]
  ;; TODO spec this and all component entry points
  [view {}
   [text-input {:style {:width 350
                        :padding-bottom 10}
                :font-family data-font-family
                :multiline true
                :default-value data
                :editable true
                :on-change-text update}]])
