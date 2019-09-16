(ns time-align-mobile.screens.pattern-planning
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  mi
                                                  button-paper
                                                  surface
                                                  subheading
                                                  text-paper
                                                  mci
                                                  status-bar
                                                  touchable-highlight]]
            ["react-native-elements" :as rne]
            [time-align-mobile.styles :as styles]
            [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                               oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
            [time-align-mobile.helpers :as helpers :refer [xor dispatch-debounced]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]
            [time-align-mobile.components.day :as day-comp]
            [time-align-mobile.screens.template-form :refer [compact]]
            [reagent.core :as r]))

(defn top-bar [{:keys [label no-changes]}]
  [surface {:flex-direction  "column"
            :justify-content "center"
            :align-items     "center"
            :padding         8}
   [subheading label]
   [button-paper {:icon     "save"
                  :mode     "outlined"
                  :on-press #(dispatch [:save-pattern-form (new js/Date)])
                  :disabled no-changes}
    "Save"]])

(defn move-template [{:keys [selected-element start-relative-min]}]
  (let [new-start (-> start-relative-min
                      (helpers/minutes->ms)
                      (int))
        duration  (- (-> selected-element
                         :stop)
                     (-> selected-element
                         :start))
        new-stop  (-> new-start
                      (+ duration)
                      (int))]
    (dispatch-debounced [:update-template-on-pattern-planning-form
                         {:id    (:id selected-element)
                          :start new-start
                          :stop  new-stop}])))

(defn root [params]
  (let [pattern-form           (subscribe [:get-pattern-form])
        pattern-form-changes   (subscribe [:get-pattern-form-changes])
        buckets                (subscribe [:get-buckets]) ;; this is just for selecting a random bucket for new template long press
        changes                (subscribe [:get-pattern-form-changes])
        selected-template      (subscribe [:get-selection-template-movement])
        selected-template-edit (subscribe [:get-selection-template-edit])
        top-bar-height         styles/top-bar-height
        dimensions             (r/atom {:width nil :height nil})]

    [view {:style {:flex 1}}
     [status-bar {:hidden true}]
     [top-bar {:label      (:label @pattern-form)
               :no-changes (empty? @pattern-form-changes)}]
     [day-comp/root
      {:selected-element      @selected-template
       :selected-element-edit @selected-template-edit
       :in-play-element       nil
       :displayed-day         (js/Date.)
       :element-type          :template
       :edit-form             compact
       :move-element          move-template
       :elements
       {:actual  (->> @pattern-form
                      :templates
                      (remove :planned)
                      (sort-by :start)
                      (helpers/get-collision-groups))
        :planned (->> @pattern-form
                      :templates
                      (filter :planned)
                      (sort-by :start)
                      (helpers/get-collision-groups))}}]]))
