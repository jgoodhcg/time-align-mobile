(ns time-align-mobile.subs
  (:require [re-frame.core :refer [reg-sub]]
            [time-align-mobile.helpers :as helpers]
            [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]))

(defn get-navigation [db _]
  (get-in db [:navigation]))

(defn get-bucket-form [db _]
  (let [bucket-form (get-in db [:forms :bucket-form])]
    (if (some? (:id bucket-form))
      bucket-form
      {:id          "nothing"
       :created     (new js/Date 2018 4 28 15 57)
       :last-edited (new js/Date 2018 4 28 15 57)
       :label       "here yet"
       :color       "#323232"
       :data        {:please "wait"}})))

(defn get-bucket-form-changes [db _]
  (let [bucket-form (get-in db [:forms :bucket-form])]
    (if (some? (:id bucket-form))
      (let [bucket (first
                  (select [:buckets sp/ALL #(= (:id %) (:id bucket-form))]
                          db))
            ;; data needs to be coerced to compare to form
            new-data (helpers/print-data (:data bucket))
            ;; (.stringify js/JSON
            ;;                      (clj->js (:data bucket))
            ;;                      nil 2)
            altered-bucket (merge bucket {:data new-data})
            different-keys (->> (clojure.data/diff bucket-form altered-bucket)
                                (first))]
        (if (nil? different-keys)
          {} ;; empty map if no changes
          different-keys))
      ;; return an empty map if there is no loaded bucket in the form
      {})))

(defn get-period-form [db _]
  (let [period-form (get-in db [:forms :period-form])]
    (if (some? (:id period-form))
      period-form
      {:id           "nothing"
       :bucket-color "#2222aa"
       :bucket-label "nothing here yet"
       :bucket-id    "nope"
       :created      (new js/Date 2018 4 28 15 57)
       :last-edited  (new js/Date 2018 4 28 15 57)
       :label        "here yet"
       :planned      false
       :start        nil
       :stop         nil
       :data         {:please "wait"}})))

(defn get-period-form-changes [db _]
  (let [period-form (get-in db [:forms :period-form])]
    (if (some? (:id period-form))
      (let [[sub-bucket period] (select-one [:buckets sp/ALL
                                (sp/collect-one (sp/submap [:id :color :label]))
                                :periods sp/ALL #(= (:id %) (:id period-form))]
                               db)
            ;; data needs to be coerced to compare to form
            new-data (helpers/print-data (:data period))
            altered-period (merge period {:data new-data
                                          :bucket-id (:id sub-bucket)
                                          :bucket-color (:color sub-bucket)
                                          :bucket-label (:label sub-bucket)})
            different-keys (->> (clojure.data/diff period-form altered-period)
                                (first))]
        (if (nil? different-keys)
          {} ;; empty map if no changes
          different-keys))
      ;; return an empty map if there is no loaded period in the form
      {})))

(defn get-buckets [db _]
  (:buckets db))

(defn get-template-form [db _]
  (let [template-form    (get-in db [:forms :template-form])
        template-form-id (:id template-form)]
    (if (and (some? template-form-id)
             (uuid? template-form-id))
      template-form
      {:id           "****"
       :bucket-color "#2222aa"
       :bucket-label "****"
       :bucket-id    "****"
       :created      (new js/Date 2018 4 28 15 57)
       :last-edited  (new js/Date 2018 4 28 15 57)
       :label        "****"
       :planned      false
       :start        nil
       :stop         nil
       :data         {:please "wait"}})))

(defn get-template-form-changes [db _]
  (let [template-form (get-in db [:forms :template-form])]
    (if (some? (:id template-form))
      (let [[sub-bucket template] (select-one [:buckets sp/ALL
                                               (sp/collect-one (sp/submap [:id :color :label]))
                                               :templates sp/ALL #(= (:id %) (:id template-form))]
                                              db)
            ;; data needs to be coerced to compare to form
            new-data              (helpers/print-data (:data template))
            altered-template      (merge template {:data         new-data
                                                   :bucket-id    (:id sub-bucket)
                                                   :bucket-color (:color sub-bucket)
                                                   :bucket-label (:label sub-bucket)})
            different-keys        (->> (clojure.data/diff template-form altered-template)
                                       (first))]
        (if (nil? different-keys)
          {} ;; empty map if no changes
          different-keys))
      ;; return an empty map if there is no loaded template in the form
      {})))

(defn get-templates [db _]
  (->> (select [:buckets sp/ALL
                (sp/collect-one (sp/submap [:id :color :label]))
                :templates sp/ALL] db)
       (map (fn [[bucket template]]
              (merge template {:bucket-id (:id bucket)
                               :bucket-label (:label bucket)
                               :color (:color bucket)})))))

(defn get-filter-form [db _]
  (let [filter-form    (get-in db [:forms :filter-form])
        filter-form-id (:id filter-form)]
    (if (and (some? filter-form-id)
             (uuid? filter-form-id))
      filter-form
      {:id          "****"
       :created     (new js/Date 2018 4 28 15 57)
       :last-edited (new js/Date 2018 4 28 15 57)
       :label       "****"
       :predicates  "{:nothing \"here yet\"}"})))

(defn get-filter-form-changes [db _]
  (let [filter-form (get-in db [:forms :filter-form])]
    (if (some? (:id filter-form))
      (let [filter         (select-one [:filters sp/ALL #(= (:id %) (:id filter-form))]
                                       db)
            ;; data needs to be coerced to compare to form
            new-predicates (helpers/print-data (:predicates filter))
            new-sort       (helpers/print-data (:sort filter))
            altered-filter (merge filter {:predicates new-predicates
                                          :sort       new-sort})

            different-keys (->> (clojure.data/diff filter-form altered-filter)
                                (first))]
        (if (nil? different-keys)
          {} ;; empty map if no changes
          different-keys))
      ;; return an empty map if there is no loaded filter in the form
      {})))

(defn get-filters [db _]
  (select [:filters sp/ALL] db))

(defn get-active-filter [db _]
  (let  [id (:active-filter db)]
    (select-one [:filters sp/ALL #(= (:id %) id)] db)))

(defn get-periods [db _]
  (->> (select [:buckets sp/ALL
                (sp/collect-one (sp/submap [:id :color :label]))
                :periods sp/ALL] db)
       (map (fn [[bucket period]]
              (merge period {:bucket-id    (:id bucket)
                             :bucket-label (:label bucket)
                             :color        (:color bucket)})))))

(defn get-selected-period [db _]
  (let [selected-id               (get-in db [:selected-period])
        [bucket selected-period ] (select-one [:buckets sp/ALL
                                               (sp/collect-one (sp/submap [:id :color :label]))
                                               :periods sp/ALL
                                               #(= (:id %) selected-id)] db)]
    (if (some? selected-id)
      (merge selected-period {:bucket-id    (:id bucket)
                              :bucket-label (:label bucket)
                              :color        (:color bucket)})
      nil)))

(defn get-day-time-navigator [db _]
  (get-in db [:time-navigators :day]))

(defn get-now [db _]
  (get-in db [:now]))

(defn get-period-in-play [db _]
  (let [period-in-play-id       (get-in db [:period-in-play-id])
        [bucket period-in-play] (select-one [:buckets sp/ALL
                                             (sp/collect-one (sp/submap [:id :color :label]))
                                             :periods sp/ALL
                                             #(= (:id %) period-in-play-id)] db)]
    (if (some? period-in-play-id)
      (merge period-in-play {:bucket-id     (:id bucket)
                             :bucket-label (:label bucket)
                             :color        (:color bucket)})
      nil) ))

(reg-sub :get-navigation get-navigation)
(reg-sub :get-bucket-form get-bucket-form)
(reg-sub :get-bucket-form-changes get-bucket-form-changes)
(reg-sub :get-period-form get-period-form)
(reg-sub :get-period-form-changes get-period-form-changes)
(reg-sub :get-buckets get-buckets)
(reg-sub :get-templates get-templates)
(reg-sub :get-template-form get-template-form)
(reg-sub :get-template-form-changes get-template-form-changes)
(reg-sub :get-filter-form get-filter-form)
(reg-sub :get-filter-form-changes get-filter-form-changes)
(reg-sub :get-filters get-filters)
(reg-sub :get-active-filter get-active-filter)
(reg-sub :get-periods get-periods)
(reg-sub :get-selected-period get-selected-period)
(reg-sub :get-day-time-navigator get-day-time-navigator)
(reg-sub :get-now get-now)
(reg-sub :get-period-in-play get-period-in-play)
