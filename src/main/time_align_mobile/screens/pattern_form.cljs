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
     [top-bar {:center-content [subheading "Plan Form"]
               :right-content  [icon-button]}]
     ;; [keyboard-aware-scroll-view
     ;;  ;; check link for why these options https://stackoverflow.com/questions/45466026/keyboard-aware-scroll-view-android-issue?rq=1
     ;;  {:enable-on-android            true
     ;;   :enable-auto-automatic-scroll (= (.-OS platform) "ios")}
     ;;  [view {:style {:flex            1
     ;;                 :flex-direction  "column"
     ;;                 :justify-content "flex-start"
     ;;                 :align-items     "flex-start"
     ;;                 :padding-top     50
     ;;                 :padding-left    4
     ;;                 :padding-right   4}}

     ;;   [label-comp pattern-form changes :update-pattern-form]
     ;;   ;; [data-comp pattern-form changes update-structured-data]
     ;;   ;; [templates-comp pattern-form]
     ;;   (changeable-field {:changes changes
     ;;                      :field-key :templates}
     ;;                     [button-paper
     ;;                      {:mode     "outlined"
     ;;                       :icon     (fn [obj]
     ;;                                   (let [{:keys [color size]} (js->clj obj :keywordize-keys)]
     ;;                                     (r/as-element [view {:style {:flex-direction "row"}}
     ;;                                                    [en {:name "edit" :color color :size size}]
     ;;                                                    [en {:name "air" :color color :size size}]])))
     ;;                       :on-press #(dispatch [:navigate-to {:current-screen :pattern-planning
     ;;                                                           :params         {:do-not-load-form true}}])}
     ;;                      "Edit templates"])

     ;;   [id-comp pattern-form]
     ;;   [last-edited-comp pattern-form]
     ;;   [created-comp pattern-form]]]

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
                           "Edit templates"])]


       [form-buttons/root
        {:changed        (> (count @changes) 0)
         :save-changes   #(dispatch [:save-pattern-form (new js/Date)])
         :cancel-changes #(dispatch [:load-pattern-form (:id @pattern-form)])
         :delete-item    #(dispatch [:delete-pattern (:id @pattern-form)])}]]]]))


