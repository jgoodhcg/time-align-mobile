(ns time-align-mobile.screens.pattern-planning
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  status-bar
                                                  touchable-highlight]]
            [time-align-mobile.styles :as styles]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]))

(defn root []
  (let [pattern-form      (subscribe [:get-pattern-form])
        top-bar-height    styles/top-bar-height
        bottom-bar-height styles/bottom-bar-height
        dimensions        (r/atom {:width nil :height nil})]

    (r/create-class
     {:reagent-render
      (fn [params]
        [view {:style     {:flex 1
                           :justify-content "flex-start"
                           :align-items "center"}

               :on-layout
               (fn [event]
                 (let [layout (-> event
                                  (oget "nativeEvent" "layout")
                                  (js->clj :keywordize-keys true))]
                   (println (str "tbh " top-bar-height
                                 "\n bbh " bottom-bar-height))
                   (if (nil? (:height dimensions))
                     (reset! dimensions {:width  (:width layout)
                                         :height (-
                                                  (:height layout)
                                                  top-bar-height
                                                  bottom-bar-height)}))))}
         ;; top bar stuff
         [status-bar {:hidden true}]
         [view {:style {:height           top-bar-height
                        :width            (:width @dimensions)
                        :background-color styles/background-color
                        :elevation        2
                        :flex-direction   "column"
                        :justify-content  "center"
                        :align-items      "center"}}
          [text "top bar here"]]

         ;; view that stretches to fill what is left of the screen
         [touchable-highlight
          {:on-long-press #(println "should make a template on this pattern")}

          [view {:style {:height           (:height @dimensions)
                         :width            (:width @dimensions)
                         :background-color "grey"}}
           [text "stuff here"]]]])})))
