(ns time-align-mobile.components.filter-picker
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  picker
                                                  picker-item
                                                  flat-list
                                                  touchable-highlight]]
            [reagent.core :as r :refer [atom]]
            ["react-native" :as rn]
            ["react" :as react]
            [re-frame.core :refer [subscribe dispatch]]))

(defn filter-items [items active-filter]
  (if (some? active-filter)
    (->> items
         (filter
          (fn [item]
            (->> (:predicates active-filter)
                 (every? (fn [{:keys [negate path value]}]
                           (let [item-val (str (get-in item path))]
                             (if negate
                               (not= item-val value)
                               (= item-val value)))))))))
    items))

(defn sort-items [items active-filter]
  (if (some? active-filter)
    (->> items

         (sort-by
          (fn [item]
            (let [path  (get-in active-filter [:sort :path])
                  value (get-in item path)]
              (cond
                (coll? value) (count value)
                (inst? value) (.valueOf value)
                :else value))))

         (#(if (not (get-in active-filter [:sort :ascending]))
              (reverse %) %)))
    items))

(defn filter-sort [items active-filter]
  (sort-items (filter-items items active-filter) active-filter))

(defn filter-picker [type]
  (let [filters (subscribe [:get-filters])
        selected-filter (subscribe [:get-active-filter])]
    [view {:style {:flex-direction "column"
                   :justify-content "flex-start"
                   :align-items "flex-start"}}
     [view {:style {:flex-direction "row"
                    :justify-content "center"
                    :align-items "center"}}
      [text {:style {:color "grey"}} ":filter"]
      [:> rn/Picker {:selected-value  (if-let [id (:id @selected-filter)]
                                  id
                                  "none")
               :style           {:width 250}
               :on-value-change #(dispatch [:update-active-filter (if (= % "none")
                                                                    nil
                                                                    %)])}
       (map (fn [filter] [picker-item {:label (:label filter)
                                       :key (:id filter)
                                       :value (:id filter)}])
            (cons {:label "none" :id "none"}
                  (filter #(some #{type} (:compatible %)) @filters)))]]
     [view {:style {:flex-direction "row"
                    :justify-content "center"
                    :align-items "center"}}
     [text {:style {:color "grey"}}  ":sort  "]
      [text (str (get-in @selected-filter [:sort :path])
                 " -- "
                 (if (get-in @selected-filter [:sort :ascending])
                   "ascending"
                     "descending"))]]]))
