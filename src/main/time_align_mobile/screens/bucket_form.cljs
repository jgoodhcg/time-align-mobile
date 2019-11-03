(ns time-align-mobile.screens.bucket-form
  (:require [time-align-mobile.js-imports :refer [view
                                                  keyboard-aware-scroll-view
                                                  text
                                                  text-input
                                                  subheading
                                                  color-picker
                                                  button-paper
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
                                                              changeable-field
                                                              filter-button
                                                              last-edited-comp
                                                              label-comp
                                                              data-comp]]
            ["react" :as react]
            ["react-native-elements" :as rne]
            [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]))

(def color-modal-visible (r/atom false))

(defn periods-comp [bucket-form]
  [view {:style {:flex-direction "row"}}
   [text {:style field-label-style} ":periods"]
   [touchable-highlight
    {:on-press #(println "navigate to periods list with filter")}
    [text (str (count (:periods @bucket-form)))]]])

(defn color-comp [bucket-form changes]
  (changeable-field {:changes   changes
                     :field-key :color}
                    [view {:flex-direction "column"}
                     [button-paper {:color         (:color @bucket-form)
                                    :mode          "contained"
                                    :content-style {:height 40 :width "100%"}
                                    :style         {:margin 8}
                                    :icon          "format-color-fill"
                                    :on-press      #(reset! color-modal-visible true)}]]))

(defn filter-for-id? [filters id]
  (let [values (->> filters
                    (map :predicates)
                    (flatten)
                    (map :value))]

    (some #(= % (str id)) values)))

(defn root [params]
  (let [bucket-form            (subscribe [:get-bucket-form])
        filters                (subscribe [:get-filters])
        update-structured-data (fn [new-data]
                                 (dispatch
                                  [:update-bucket-form {:data new-data}]))
        changes                (subscribe [:get-bucket-form-changes])]
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

       [view {:style {:height        64
                      :margin-bottom 8}}
        [label-comp bucket-form changes :update-bucket-form]]
       [color-comp bucket-form changes]
       ;; [data-comp bucket-form changes update-structured-data]
       ;; [periods-comp bucket-form]
       ;; [templates-comp bucket-form]
       [id-comp bucket-form]
       [last-edited-comp bucket-form]
       [created-comp bucket-form]
       [modal {:animation-type   "slide"
               :transparent      false
               :on-request-close #(reset! color-modal-visible false)
               :visible          @color-modal-visible}
        [view {:style {:flex 1}}
         [color-picker {:on-color-selected (fn [color]
                                             (dispatch [:update-bucket-form {:color color}])
                                             (reset! color-modal-visible false))
                        :old-color         (:color @bucket-form)
                        :style             {:flex 1}}]]]]]

     [view {:flex           1
            :flex-direction "column"
            :align-items    "center"
            :padding        4}

      (when-not (filter-for-id? @filters (:id @bucket-form))
        [filter-button
         bucket-form
         #(dispatch
           [:add-auto-filter
            {:id          (random-uuid)
             :label       (str (:label @bucket-form)
                               " bucket filter")
             :created     (js/Date.)
             :last-edited (js/Date.)
             :compatible  [:period :template]
             :sort        {:path      [:start]
                           :ascending true}
             :predicates  [{:path   [:bucket-id]
                            :negate false
                            :value  (str (:id @bucket-form))}]}])])

      [form-buttons/root
       {:changed        (> (count @changes) 0)
        :save-changes   #(dispatch [:save-bucket-form (new js/Date)])
        :cancel-changes #(dispatch [:load-bucket-form (:id @bucket-form)])
        :delete-item    #(dispatch [:delete-bucket (:id @bucket-form)])}]]]))
