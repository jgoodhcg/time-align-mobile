(ns time-align-mobile.screens.pattern-form
(:require [time-align-mobile.js-imports :refer [view
                                                keyboard-aware-scroll-view
                                                text
                                                text-input
                                                color-picker
                                                icon-button
                                                subheading
                                                date-time-picker
                                                modal
                                                en
                                                button-paper
                                                surface
                                                platform
                                                touchable-highlight
                                                format-date]]
            [time-align-mobile.components.structured-data :refer [structured-data]]
            [time-align-mobile.components.form-buttons :as form-buttons]
            [time-align-mobile.components.top-bar :refer [top-bar]]
            [time-align-mobile.styles :refer [field-label-changeable-style
                                              field-label-style]]
            [time-align-mobile.components.form-fields :refer [id-comp
                                                              created-comp
                                                              last-edited-comp
                                                              changeable-field
                                                              filter-button
                                                              label-comp-md
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
     [top-bar {:center-content [subheading "Template Edit"]
               :right-content  [icon-button]}]

     [surface {:style {:flex       1
                       :margin-top 4}}
      [view {:flex            1
             :flex-direction  "column"
             :justify-content "flex-start"
             :align-items     "flex-start"
             :padding         4}

       [label-comp-md {:form        pattern-form
                       :changes     changes
                       :update-key  :update-pattern-form
                       :compact     false
                       :placeholder "This plan is for ..."}]

       [view {:style {:flex-direction "row"
                      :margin-top     32}}
        [icon-button]
        (changeable-field {:changes   changes
                           :field-key :templates}
                          [button-paper
                           {:mode     "contained"
                            :icon     "playlist-edit"
                            :on-press #(dispatch [:navigate-to {:current-screen :pattern-planning
                                                                :params         {:do-not-load-form true}}])}
                           "Edit template periods"])]


       [form-buttons/root
        {:changed        (> (count @changes) 0)
         :save-changes   #(dispatch [:save-pattern-form (new js/Date)])
         :cancel-changes #(dispatch [:load-pattern-form (:id @pattern-form)])
         :delete-item    #(dispatch [:delete-pattern (:id @pattern-form)])}]]]]))


