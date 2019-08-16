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

(def top (r/atom 0))

(def selected (r/atom false))

(defn root [params]
  (let [pan-ref        (.createRef react)
        pinch-ref      (.createRef react)
        double-tap-ref (.createRef react)]
    [scroll-view-gesture-handler {:wait-for                double-tap-ref
                                  :on-gesture-event        #(println "scroll gesture")
                                  :on-handler-state-change #(println (str "scroll " (get-state %)))}
     [view {:style {:flex             1
                    :flex-direction   "column"
                    :height           2000
                    :background-color "grey"
                    :justify-content  "space-between"
                    :align-items      "center"}}
      [pan-gesture-handler {:ref                     pan-ref
                            :enabled                 @selected
                            :on-handler-state-change #(println (str "pan " (get-state %)))
                            :on-gesture-event        #(println "pan gesture")}
       [tap-gesture-handler {:ref                     double-tap-ref
                             :wait-for                pan-ref
                             :on-handler-state-change #(if (= (get-state %) "active")
                                                         (do (println "tapped!")
                                                             (swap! selected not))
                                                         (println "not active tap state change"))}

        [view {:style {:background-color (if @selected "purple" "green")
                       :position         "absolute"
                       :height           150
                       :width            150
                       :top              @top}}
         [text "this is a calendar page"]]]]

      [text "here is a thing"]

      [text "here is another thing"]]]))
