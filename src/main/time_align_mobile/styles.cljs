(ns time-align-mobile.styles
  (:require
   ;; [cljs.spec.test.alpha :as stest]
   ;; [cljs.spec.alpha :as s]
   ))

;; https://github.com/react-native-training/react-native-elements/blob/master/src/config/colors.js#L9
(defn field-label-changeable-style [changes field]
  (if (contains? changes field)
    {:color "#00ff00"}
    {:color "#86939e"}))

;; (s/fdef field-label-changeable-style
;;   :args (s/and #(map? (first %))
;;                #(keyword? (second %)))
;;   :ret map?)

;; (stest/instrument 'field-label-changeable-style)

(def field-label-style {:color         "grey"
                        :padding-right 5
                        :width 75})
