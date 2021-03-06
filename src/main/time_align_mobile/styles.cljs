(ns time-align-mobile.styles
  (:require
   ;; [cljs.spec.test.alpha :as stest]
   ;; [cljs.spec.alpha :as s]
   [time-align-mobile.js-imports :refer [default-theme dark-theme]]))

(def theme (-> dark-theme
               (js->clj :keywordize-keys true)
               ;; https://material.io/tools/color/#!/?view.left=1&view.right=0&primary.color=212121&secondary.color=757575
               (update-in [:colors] #(merge % {:primary            "#e0e0e0"
                                               :accent             "#f5f5f5"
                                               :accent-light       "#ffffff"
                                               :element-text-light "#ffffff"
                                               :element-text-dark  "#000000"}))
               (merge {})))

;; https://github.com/react-native-training/react-native-elements/blob/master/src/config/colors.js#L9
(defn field-label-changeable-style [changes field]
  (if (contains? changes field)
    {:background-color (get-in theme [:colors :accent-light])}
    {}))

;; (s/fdef field-label-changeable-style
;;   :args (s/and #(map? (first %))
;;                #(keyword? (second %)))
;;   :ret map?)

;; (stest/instrument 'field-label-changeable-style)

(def field-label-style {:color         "grey"
                        :padding-right 5
                        :width 75})

(def border-color "#dadce0")
(def background-color "#fff")
(def background-color-dark "#333333")
(def time-indicator-line-style {:background-color border-color
                                :height           2})
(def time-indicator-text-style {:color border-color})

(def top-bar-height 50)
(def bottom-bar-height 40)
(def text-light "#fff")
(defn styled-icon-factory [icon-class style]
  (fn [params]
    [icon-class (merge params style)]))

(def divider-style {:height        1
                    :width         "100%"
                    :margin-top    16
                    :margin-bottom 16
                    :margin-left   8
                    :margin-right  8})

(def modal-style {:margin           32
                  :margin-top       128
                  :padding          8
                  :elevation        8
                  :height           512
                  :background-color (-> theme :colors :background)})

(def form-heading {:color (->> theme :colors :disabled)})
