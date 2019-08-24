(ns time-align-mobile.screens.calendar
  (:require [time-align-mobile.js-imports :refer [view text
                                                  gesture-states
                                                  scroll-view-gesture-handler
                                                  pinch-gesture-handler
                                                  tap-gesture-handler
                                                  pan-gesture-handler]]
            ["react" :as react]
            [reagent.core :as r]
            [goog.object :as obj]))

(defn get-state [native-event]
  (condp = (obj/getValueByKeys native-event #js["nativeEvent" "state"])
    (:active gesture-states)       "active"
    (:undetermined gesture-states) "undetermined"
    (:failed gesture-states)       "failed"
    (:began gesture-states)        "began"
    (:cancelled gesture-states)    "canceled"
    (:end gesture-states)          "end"
    "something else happened"))

;; this simulates some storage (might not be in redux state)
(def buffer-atom (atom {:buffer []
                        :working-avg 0}))

;; r atom simulate redux state
(def top (r/atom 0))

(def selected (r/atom false))

(defn get-ys [nativeEvent]
  {:y
   (obj/getValueByKeys
    nativeEvent #js["nativeEvent" "y"])

   :absolute
   (obj/getValueByKeys
    nativeEvent #js["nativeEvent" "absoluteY"])

   :translation
   (obj/getValueByKeys
    nativeEvent #js["nativeEvent" "translationY"])

   :velocity
   (obj/getValueByKeys
    nativeEvent #js["nativeEvent" "velocityY"])})

(defn determine-direction [start stop ys]
  (let [midpoint       (-> stop
                           (- start)
                           (/ 2))
        above-midpoint (-> ys
                           :y
                           (< midpoint))]
    ))

(defn root [params]
  (let [pinch-ref (.createRef react)]
    [scroll-view-gesture-handler {:enabled                 (not @selected)
                                  :wait-for                pinch-ref
                                  :on-gesture-event        #(println "scroll gesture")
                                  :on-handler-state-change #(println (str "scroll " (get-state %)))}


     [pinch-gesture-handler {:ref                     pinch-ref
                             :on-gesture-event        #(do (println "Pinch gesture")
                                                           (println %))
                             :on-handler-state-change #(println (str "pinch " (get-state %)))}
      [view {:style {:flex             1
                     :flex-direction   "column"
                     :height           1440
                     :background-color "white"
                     :justify-content  "space-between"
                     :align-items      "center"}}
       [text "Stuff above"]

       [view {:style {:background-color (if @selected "yellow" "green")
                      :position         "absolute"
                      :height           150
                      :width            150
                      :top              10}}

        [text "Element"]]


       [text "Stuff below"]]]]))
