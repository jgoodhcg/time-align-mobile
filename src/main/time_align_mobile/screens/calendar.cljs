(ns time-align-mobile.screens.calendar
  (:require [time-align-mobile.js-imports :refer [view text
                                                  gesture-states
                                                  scroll-view-gesture-handler
                                                  pinch-gesture-handler
                                                  tap-gesture-handler
                                                  long-press-gesture-handler
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

(def movement-selection (r/atom false))

(def edit-selection (r/atom false))

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
                           (< midpoint))]))

(defn root [params]
  (let [pinch-ref (.createRef react)
        long-ref  (.createRef react)]
    [scroll-view-gesture-handler {:enabled                 (not @movement-selection)
                                  :wait-for                pinch-ref
                                  :on-gesture-event        #(println "scroll gesture")
                                  :on-handler-state-change #(println (str "scroll " (get-state %)))}

     [pan-gesture-handler {:enabled                 @movement-selection
                           :wait-for                pinch-ref
                           :on-gesture-event        #(reset! top (:y (get-ys %)))
                           :on-handler-state-change #(println (str "pan " (get-state %)))}

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

        [long-press-gesture-handler {:ref                     long-ref
                                     :on-handler-state-change #(let [state (get-state %)]
                                                                 (println (str "long " state))
                                                                 (if (= "active" state)
                                                                   (swap! movement-selection not)))}
         [view {:style {:background-color (if @movement-selection "red" "blue")
                        :position         "absolute"
                        :height           150
                        :width            150
                        :top              @top}}

          [text "Element"]]]


        [text "Stuff below"]]]]]))
