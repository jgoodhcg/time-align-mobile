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
                                                  surface
                                                  touchable-highlight
                                                  icon-button
                                                  format-date]]
            [time-align-mobile.components.top-bar :refer [top-bar]]
            [time-align-mobile.components.structured-data :refer [structured-data]]
            [time-align-mobile.components.form-buttons :as form-buttons]
            [time-align-mobile.styles :refer [field-label-changeable-style
                                              field-label-style]]
            [time-align-mobile.components.form-fields :refer [id-comp
                                                              created-comp
                                                              changeable-field
                                                              filter-button
                                                              last-edited-comp
                                                              label-comp-md
                                                              changeable-field
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
  [view {:style {:flex-direction "row"
                 :margin-top     8}}

   [icon-button {:icon "format-color-fill"}]

   [view {:style {:flex-direction  "column"}}
    (changeable-field {:changes   changes
                       :field-key :color} [subheading "Color"])
    [button-paper {:color         (:color @bucket-form)
                   :mode          "contained"
                   :content-style {:height 40 :width "100%"}
                   :on-press      #(reset! color-modal-visible true)}
     (:color @bucket-form)]]])

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
     [top-bar {:center-content [subheading "Group Form"]
               :right-content  [icon-button]}]

     ;; FORM

     ;; [view {:style {:flex            1
     ;;                :flex-direction  "column"
     ;;                :justify-content "flex-start"
     ;;                :align-items     "flex-start"
     ;;                :padding-top     50
     ;;                :padding-left    4
     ;;                :padding-right   4}}

     ;;  [view {:style {:height        64
     ;;                 :margin-bottom 8}}
     ;;     [label-comp bucket-form changes :update-bucket-form]]
     ;;    [color-comp bucket-form changes]
     ;;    ;; [data-comp bucket-form changes update-structured-data]
     ;;    ;; [periods-comp bucket-form]
     ;;    ;; [templates-comp bucket-form]
;; ]

     [view {:flex            1
            :flex-direction  "column"
            :justify-content "flex-start"
            :align-items     "flex-start"
            :padding         4}

      [label-comp-md {:form        bucket-form
                      :changes     changes
                      :update-key  :update-bucket-form
                      :compact     false
                      :placeholder "Group name ..."}]


      [modal {:animation-type   "slide"
              :transparent      false
              :on-request-close #(reset! color-modal-visible false)
              :visible          @color-modal-visible}
       [view {:style {:flex 1}}
        [color-picker {:on-color-selected (fn [color]
                                            (dispatch [:update-bucket-form {:color color}])
                                            (reset! color-modal-visible false))
                       :old-color         (:color @bucket-form)
                       :style             {:flex 1}}]]]

      [color-comp bucket-form changes]

      ;; TODO details collapsible section?
      ;; [id-comp bucket-form]
      ;; [last-edited-comp bucket-form]
      ;; [created-comp bucket-form]

      ;; FILTER BUTTON
      ;; (when-not (filter-for-id? @filters (:id @bucket-form))
      ;;   [filter-button
      ;;    bucket-form
      ;;    #(dispatch
      ;;      [:add-auto-filter
      ;;       {:id          (random-uuid)
      ;;        :label       (str (:label @bucket-form)
      ;;                          " bucket filter")
      ;;        :created     (js/Date.)
      ;;        :last-edited (js/Date.)
      ;;        :compatible  [:period :template]
      ;;        :sort        {:path      [:start]
      ;;                      :ascending true}
      ;;        :predicates  [{:path   [:bucket-id]
      ;;                       :negate false
      ;;                       :value  (str (:id @bucket-form))}]}])])

      [form-buttons/root
       {:changed        (> (count @changes) 0)
        :save-changes   #(dispatch [:save-bucket-form (new js/Date)])
        :cancel-changes #(dispatch [:load-bucket-form (:id @bucket-form)])
        :delete-item    #(dispatch [:delete-bucket (:id @bucket-form)])}]

      ]]))
