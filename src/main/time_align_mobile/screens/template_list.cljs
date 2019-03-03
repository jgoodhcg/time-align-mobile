(ns time-align-mobile.screens.template-list
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  flat-list
                                                  modal
                                                  touchable-highlight]]
            ["react" :as react]
            [time-align-mobile.components.filter-picker :refer [filter-picker
                                                                filter-sort]]
            [time-align-mobile.components.list-buttons :as list-buttons]
            [time-align-mobile.components.list-items :as list-items]
            [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]))

(def bucket-modal-visible (atom false))

(defn root [params]
  (let [templates     (subscribe [:get-templates])
        buckets       (subscribe [:get-buckets])
        active-filter (subscribe [:get-active-filter])]
    [view {:style {:flex 1 :justify-content "center" :align-items "center"}}
     [text "Templates"]
     [filter-picker :template]
     [flat-list {:data (filter-sort @templates @active-filter)
                 :render-item
                 (fn [i]
                   (let [item (:item (js->clj i :keywordize-keys true))]
                     (r/as-element (list-items/template
                                    (merge
                                     item
                                     {:on-press
                                      #(dispatch
                                        [:navigate-to
                                         {:current-screen :template
                                          :params         {:template-id (:id item)}}])})))))}]

     [modal {:animation-type "slide"
             :transparent    false
             :visible        @bucket-modal-visible}
      [view {:style {:flex    1
                     :padding 10}}
       [text "Select a bucket to add the template to"]
       [flat-list {:data @buckets
                   :render-item
                   (fn [i]
                     (let [item (:item (js->clj i :keywordize-keys true))]
                       (r/as-element
                        (list-items/bucket
                         (merge
                          item
                          {:on-press
                           (fn [_]
                             (reset! bucket-modal-visible false)
                             ;; passing dispatch the parent bucket id
                             ;; for the period about to be created
                             (dispatch [:add-new-template {:bucket-id (:id item)
                                                           :id (random-uuid)
                                                           :now (js/Date.)}]))})))))}]]]

     [list-buttons/root #(reset! bucket-modal-visible true)]]))
