(ns time-align-mobile.screens.day-planning
  (:require [time-align-mobile.js-imports :refer [view
                                                  scroll-view
                                                  text
                                                  flat-list
                                                  format-date
                                                  format-time
                                                  touchable-highlight
                                                  status-bar
                                                  animated-view
                                                  mi
                                                  mci
                                                  fa
                                                  modal
                                                  animated-xy
                                                  pan-responder]]
            ["react-native-elements" :as rne]
            [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers :refer [same-day? xor]]
            [time-align-mobile.components.list-items :as list-items]
            [time-align-mobile.styles :as styles :refer [theme]]
            [goog.string :as gstring]
            ;; [zprint.core :refer [zprint]]
            ["react" :as react]
            [goog.string.format]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [time-align-mobile.helpers :as helpers]
            [time-align-mobile.components.day :as day-comp]
            [reagent.core :as r]))

(defn top-bar []
  [view
   {:style (merge
            ;; testing styles
            {:border-width 8
             :border-color "yellow"}
            ;; actual styles
            {})}

   [text {:style {:height 60}} "Day planning top bar"]])

(defn root [params]
  (let [dimensions        (r/atom {:width nil :height nil})
        top-bar-height    styles/top-bar-height
        bottom-bar-height 0 ;; styles/bottom-bar-height
        periods           (subscribe [:get-collision-grouped-periods])
        displayed-day     (subscribe [:get-day-time-navigator])
        selected-period   (subscribe [:get-selected-period])
        period-in-play    (subscribe [:get-period-in-play])
        now               (subscribe [:get-now])
        buckets           (subscribe [:get-buckets])
        patterns          (subscribe [:get-patterns])
        templates         (subscribe [:get-templates])
        time-alignment-fn #(cond (nil? %)           :center
                                 (:planned %)       :left
                                 (not (:planned %)) :right)]
    [view {:style {:flex 1}}
     [status-bar {:hidden true}]
     [top-bar]
     [day-comp/root {:selected-element @selected-period
                     :in-play-element  @period-in-play
                     :elements         @periods}]]))

