(ns time-align-mobile.screens.pattern-planning
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  mi
                                                  button-paper
                                                  mci
                                                  status-bar
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]
            [time-align-mobile.styles :as styles]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers :as helpers :refer [xor]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]
            [time-align-mobile.components.day :as day-comp]
            [reagent.core :as r]))

(defn top-bar []
  [view
   {:style (merge
            ;; testing styles
            {:border-width 8
             :border-color "red"}
            ;; actual styles
            {})}

   [text {:style {:height 60}} "Pattern planning top bar"]])

(defn root [params]
  (let [pattern-form      (subscribe [:get-pattern-form])
        buckets           (subscribe [:get-buckets]) ;; this is just for selecting a random bucket for new template long press
        changes           (subscribe [:get-pattern-form-changes])
        selected-template (subscribe [:get-selected-template])
        top-bar-height    styles/top-bar-height
        dimensions        (r/atom {:width nil :height nil})]

    [view {:style {:flex 1}}
     [status-bar {:hidden true}]
     [top-bar]
     [day-comp/root
      {:selected-element @selected-template
       :in-play-element  nil
       :displayed-day    (js/Date.)
       :elements
       {:actual  []
        :planned (->> @pattern-form
                      :templates
                      (sort-by :start)
                      (helpers/get-collision-groups))}}]]))
