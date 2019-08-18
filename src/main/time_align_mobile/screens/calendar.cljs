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

(defn root [params]
  (let [pan-ref        (.createRef react)
        pinch-ref      (.createRef react)
        double-tap-ref (.createRef react)]
    [scroll-view-gesture-handler {:enabled                 (not @selected)
                                  :on-gesture-event        #(println "scroll gesture")
                                  :on-handler-state-change #(println (str "scroll " (get-state %)))}
     [pan-gesture-handler {:ref                     pan-ref
                           :enabled                 @selected
                           :on-handler-state-change #(when (= "end" (get-state %))
                                                       (swap! buffer-atom
                                                              (fn [b]
                                                                {:buffer      []
                                                                 :working-avg 0})))
                           :on-gesture-event        #(let [y
                                                           (obj/getValueByKeys
                                                            % #js["nativeEvent" "y"])

                                                           absolute-y
                                                           (obj/getValueByKeys
                                                            % #js["nativeEvent" "absoluteY"])

                                                           translation-y
                                                           (obj/getValueByKeys
                                                            % #js["nativeEvent" "translationY"])

                                                           velocity-y
                                                           (obj/getValueByKeys
                                                            % #js["nativeEvent" "velocityY"])]

                                                       (println {:y             y :absolute-y absolute-y
                                                                 :translation-y translation-y
                                                                 :velocity-y    velocity-y})
                                                       (reset! top y)
                                                       )}

      [view {:style {:flex             1
                     :flex-direction   "column"
                     :height           1440
                     :background-color "white"
                     :justify-content  "space-between"
                     :align-items      "center"}}
       [tap-gesture-handler {:ref                     double-tap-ref
                             :wait-for                pan-ref
                             :on-handler-state-change #(if (= (get-state %) "active")
                                                         (do (println "tapped!")
                                                             (swap! selected not))
                                                         (println "not active tap state change"))}

        [view {:style {:background-color (if @selected "red" "pink")
                       :position         "absolute"
                       :height           150
                       :width            150
                       :top              @top}}
         [text "this is a calendar page"]]]

       [text "here is a thing"]

       [text "here is another thing"]]]]))
