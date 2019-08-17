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
    [scroll-view-gesture-handler {:wait-for                double-tap-ref
                                  :on-gesture-event        #(println "scroll gesture")
                                  :on-handler-state-change #(println (str "scroll " (get-state %)))}
     [view {:style {:flex             1
                    :flex-direction   "column"
                    :height           1440
                    :background-color "grey"
                    :justify-content  "space-between"
                    :align-items      "center"}}
      [pan-gesture-handler {:ref                     pan-ref
                            :enabled                 @selected
                            :on-handler-state-change #(when (= "end" (get-state %))
                                                        (swap! buffer-atom
                                                               (fn [b]
                                                                 {:buffer      []
                                                                  :working-avg 0})))
                            :on-gesture-event        #(let [smooth-factor 0.25
                                                            new-val
                                                            (obj/getValueByKeys % #js["nativeEvent" "y"])
                                                            buffer-stuff  @buffer-atom
                                                            mode          (->> buffer-stuff
                                                                               :buffer
                                                                               frequencies
                                                                               (sort-by second)
                                                                               last
                                                                               first)
                                                            working-avg   (+
                                                                           (* new-val smooth-factor)
                                                                           (* (- 1 smooth-factor)
                                                                              (:working-avg buffer-stuff)))
                                                            buffer-count  (count (:buffer buffer-stuff))]

                                                        (println (str new-val ", " mode ", " working-avg))
                                                        ;; wait for the buffer to fill up
                                                        (when (-> buffer-count (>= 10))
                                                          ;; then use the working avg to update time
                                                          (reset! top new-val))
                                                        ;; always keep pushing stuff to the buffer
                                                        (swap! buffer-atom
                                                               (fn [b]
                                                                 {:buffer      (->> b
                                                                                    :buffer
                                                                                    (cons new-val)
                                                                                    (take 10))
                                                                  :working-avg working-avg}))
                                                        )}
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
         [text "this is a calendar page"]]]]

      [text "here is a thing"]

      [text "here is another thing"]]]))
