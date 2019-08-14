(ns time-align-mobile.screens.calendar
  (:require [time-align-mobile.js-imports :refer [view text
                                                  gesture-states
                                                  pinch-gesture-handler
                                                  tap-gesture-handler
                                                  pan-gesture-handler]]
            ["react" :as react]
            [goog.object :as obj]))

(defn root [params]
  (let [pan-ref        (.createRef react)
        pinch-ref      (.createRef react)
        double-tap-ref (.createRef react)]
    [pan-gesture-handler {:wait-for                double-tap-ref
                          :on-handler-state-change
                          #(println
                            (str "pan state change: "
                                 (condp = (obj/getValueByKeys % #js["nativeEvent" "state"])
                                   (:active gesture-states)       "active"
                                   (:undetermined gesture-states) "undetermined"
                                   (:failed gesture-states)       "failed"
                                   (:began gesture-states)        "began"
                                   (:cancelled gesture-states)    "canceled"
                                   (:end gesture-states)          "end"
                                   "something else happened")))
                          :on-gesture-event        #(println "gesture on pan")}
     [tap-gesture-handler {:ref                     double-tap-ref
                           :on-handler-state-change
                           #(println
                             (str "tap state change: "
                                  (condp = (obj/getValueByKeys % #js["nativeEvent" "state"])
                                    (:active gesture-states)       "active"
                                    (:undetermined gesture-states) "undetermined"
                                    (:failed gesture-states)       "failed"
                                    (:began gesture-states)        "began"
                                    (:cancelled gesture-states)    "canceled"
                                    (:end gesture-states)          "end"
                                    "something else happened")))
                           :on-gesture-event        #(println "double tap baby!")}
      [view {:style {:flex 1 :justify-content "center" :align-items "center"}}
       [text "calendar pan and tap"]]]]))
