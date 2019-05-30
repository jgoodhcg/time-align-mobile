(ns time-align-mobile.screens.pattern-form
(:require [time-align-mobile.js-imports :refer [view
                                                  keyboard-aware-scroll-view
                                                  text
                                                  text-input
                                                  color-picker
                                                  date-time-picker
                                                  modal
                                                  platform
                                                  touchable-highlight
                                                  format-date]]
            [time-align-mobile.components.structured-data :refer [structured-data]]
            [time-align-mobile.components.form-buttons :as form-buttons]
            [time-align-mobile.styles :refer [field-label-changeable-style
                                              field-label-style]]
            [time-align-mobile.components.form-fields :refer [id-comp
                                                              created-comp
                                                              last-edited-comp
                                                              label-comp
                                                              data-comp]]
            ["react" :as react]
            ["react-native-elements" :as rne]
            [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]))

(defn templates-comp [pattern-form]
  [view {:style {:flex-direction "row"}}
   [text {:style field-label-style} ":templates"]
   [touchable-highlight
    {:on-press #(println "navigate to templates list with filter")}
    [text (str (count (:templates @pattern-form)))]]])

(defn filter-button [pattern-form]
  [:> rne/Button {:icon     (r/as-element [:> rne/Icon {:name  "filter"
                                                        :type  "font-awesome"
                                                        :color "#fff"}])
                  :title    "Add Filter"
                  :on-press #(dispatch
                              [:add-auto-filter
                               {:id          (random-uuid)
                                :label       (str (:label @pattern-form)
                                                  " pattern filter")
                                :created     (js/Date.)
                                :last-edited (js/Date.)
                                :compatible  [:pattern]
                                :sort        {:path      [:start]
                                              :ascending true}
                                :predicates  [{:path   [:pattern-id]
                                               :negate false
                                               :value  (str (:id @pattern-form))}]}])}])

(defn filter-for-id? [filters id]
  (let [values (->> filters
                    (map :predicates)
                    (flatten)
                    (map :value))]

    (some #(= % (str id)) values)))

(defn root [params]
  (let [pattern-form           (subscribe [:get-pattern-form])
        filters                (subscribe [:get-filters])
        update-structured-data (fn [new-data]
                                 (dispatch
                                  [:update-pattern-form {:data new-data}]))
        changes                (subscribe [:get-pattern-form-changes])]

    [view {:flex 1}
     [keyboard-aware-scroll-view
      ;; check link for why these options https://stackoverflow.com/questions/45466026/keyboard-aware-scroll-view-android-issue?rq=1
      {:enable-on-android            true
       :enable-auto-automatic-scroll (= (.-OS platform) "ios")}
      [view {:style {:flex            1
                     :flex-direction  "column"
                     :justify-content "flex-start"
                     :align-items     "flex-start"
                     :padding-top     50
                     :padding-left    4
                     :padding-right   4}}

       [label-comp pattern-form changes :update-pattern-form]
       ;; [data-comp pattern-form changes update-structured-data]
       ;; [templates-comp pattern-form]
       [id-comp pattern-form]
       [view
        {:style {:padding 10}}
        [:> rne/Button
         {:title                "Edit Plan"
          :icon                 (r/as-element [:> rne/Icon {:name  "air"
                                                            :type  "entypo"
                                                            :color "#fff"}])
          :on-press             #(dispatch [:navigate-to {:current-screen :pattern-planning
                                                          :params         {:do-not-load-form true}}])
          :container-style      {:margin-right 4}}]]
       [last-edited-comp pattern-form]
       [created-comp pattern-form]]]

     [view {:flex           1
            :flex-direction "column"
            :align-items    "center"
            :padding        4}

      (when-not (filter-for-id? @filters (:id @pattern-form))
        [filter-button pattern-form])

      [form-buttons/root
       {:changed        (> (count @changes) 0)
        :save-changes   #(dispatch [:save-pattern-form (new js/Date)])
        :cancel-changes #(dispatch [:load-pattern-form (:id @pattern-form)])
        :delete-item    #(dispatch [:delete-pattern (:id @pattern-form)])}]]]))


