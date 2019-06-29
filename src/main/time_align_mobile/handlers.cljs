(ns time-align-mobile.handlers
  (:require
    [time-align-mobile.js-imports :refer [write-file-to-dd! alert share format-date]]
    [re-frame.core :refer [reg-event-db ->interceptor reg-event-fx reg-fx]]
    ;; [zprint.core :refer [zprint]]
    [cljs.reader :refer [read-string]]
    [clojure.spec.alpha :as s]
    [time-align-mobile.db :as db :refer [app-db app-db-spec period-data-spec]]
    [time-align-mobile.helpers :as helpers :refer [same-day?]]
    [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]))

;; -- Interceptors ----------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/blob/develop/docs/Interceptors.md
;;
(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db]
  (when-not (s/valid? spec db)
    (let [explaination (s/explain-str spec db)]
      ;; (zprint (::clojure.spec.alpha/problems explaination) {:map {:force-nl? true}})
      (println explaination)
      ;; (throw (ex-info (str "Spec check failed: " explain-data) explain-data))
      (alert "Failed spec validation" "Check the command line output.")
      true)))

(def validate-spec
  (if true ;;goog.DEBUG ;; TODO reinstate this after pre-alpha
    (->interceptor
        :id :validate-spec
        :after (fn [context]
                 (let [db (-> context :effects :db)
                       old-db (-> context :coeffects :db)]
                   (if (some? (check-and-throw app-db-spec db))
                     (assoc-in context [:effects :db] old-db) ;; put the old db back as the new db
                     context))))
    ->interceptor))

(def alert-message
  (->interceptor
   :id :alert
   :after (fn [context]
            (let [alert-message (-> context :effects :alert)]
              ;; message is ussually in the following format
              ;; "Failed data json validation SyntaxError: JSON Parse error: Expected '}'"
              ;; The user only cares about the Expected bit and the alert has limited space
              (when (some? alert-message) (alert
                                           "Validation failed"
                                           (str alert-message)
                                           ;; (last (.split (str alert-message)
                                           ;;               ":"))
                                           ))
              (setval [:effects :alert] sp/NONE context)))))

(def persist-secure-store ;; TODO rename this
  (->interceptor
   :id :persist-secure-store
   :after (fn [context]
            (write-file-to-dd! "app-db" (-> context :effects :db str))
            context)))

;; -- Helpers ---------------------------------------------------------------
(defn clean-period [period]
  (select-keys period (keys period-data-spec)))

;; -- Handlers --------------------------------------------------------------

(defn initialize-db [_ _] app-db)

(defn load-db [old-db [_ db]] db)

(defn navigate-to [{:keys [db]} [_ {:keys [current-screen params]}]]
  (merge {:db (-> db
                  (assoc-in [:navigation] {:current-screen current-screen
                                           :params         params})
                  ;; prevents using incompatible filters
                  (assoc-in [:active-filter] nil))}

         ;; bucket
         (when (= current-screen :bucket)
           {:dispatch [:load-bucket-form (:bucket-id params)]})

         ;; pattern
         (when (and (= current-screen :pattern)
                    (not (:do-not-load-form params)) )
           {:dispatch [:load-pattern-form (:pattern-id params)]})

         ;; pattern-planning
         (when (and (= current-screen :pattern-planning)
                    (not (:do-not-load-form params)))
           {:dispatch [:load-pattern-form (:pattern-id params)]})

         ;; period
         (when (= current-screen :period)
           {:dispatch [:load-period-form (:period-id params)]})

         ;; template
         (when (= current-screen :template)
           (if (contains? params :pattern-form-pattern-id)
             {:dispatch [:load-template-form-from-pattern-planning (:template-id params)]}
             {:dispatch [:load-template-form (:template-id params)]}))

         ;; filter
         (when (= current-screen :filter)
           {:dispatch [:load-filter-form (:filter-id params)]})))

(defn load-bucket-form [db [_ bucket-id]]
  (let [bucket      (select-one [:buckets sp/ALL #(= (:id %) bucket-id)] db)
        bucket-form (merge bucket {:data (helpers/print-data (:data bucket))})]
    (assoc-in db [:forms :bucket-form] bucket-form)))

(defn load-pattern-form [db [_ pattern-id]]
  (let [pattern      (select-one [:patterns sp/ALL #(= (:id %) pattern-id)] db)
        pattern-form (merge pattern {:data (helpers/print-data (:data pattern))})]
    (assoc-in db [:forms :pattern-form] pattern-form)))

(defn update-bucket-form [db [_ bucket-form]]
  (transform [:forms :bucket-form] #(merge % bucket-form) db))

(defn update-pattern-form [db [_ pattern-form]]
  (transform [:forms :pattern-form] #(merge % pattern-form) db))

(defn save-bucket-form [{:keys [db]} [_ date-time]]
  (let [bucket-form (get-in db [:forms :bucket-form])]
    (try
       (let [new-data (read-string (:data bucket-form))
             new-bucket (merge bucket-form {:data new-data
                                       :last-edited date-time})
            new-db (setval [:buckets sp/ALL #(= (:id %) (:id new-bucket))]
                           new-bucket
                           db)]
        {:db new-db
         ;; load bucket form so that the data string gets re-formatted prettier
         :dispatch [:load-bucket-form (:id new-bucket)]})
      (catch js/Error e
        {:db db
         :alert (str "Failed data json validation " e)}))))

(defn save-pattern-form [{:keys [db]} [_ date-time]]
  (let [pattern-form (get-in db [:forms :pattern-form])]
    (try
      (let [new-data    (read-string (:data pattern-form))
            new-pattern (merge pattern-form {:data        new-data
                                             :last-edited date-time})
            new-db      (setval [:patterns sp/ALL #(= (:id %) (:id new-pattern))]
                                new-pattern
                                db)]
        {:db       new-db
         ;; load pattern form so that the data string gets re-formatted prettier
         :dispatch [:load-pattern-form (:id new-pattern)]})
      (catch js/Error e
        {:db    db
         :alert (str "Failed data json validation " e)}))))

(defn load-period-form [db [_ period-id]]
  (let [[sub-bucket period] (select-one
                             [:buckets sp/ALL
                              (sp/collect-one (sp/submap [:id :color :label]))
                              :periods sp/ALL #(= (:id %) period-id)] db)
        sub-bucket-remap    {:bucket-id    (:id sub-bucket)
                             :bucket-color (:color sub-bucket)
                             :bucket-label (:label sub-bucket)}
        period-form         (merge period
                                   {:data (helpers/print-data (:data period))}
                                   sub-bucket-remap)]
    (assoc-in db [:forms :period-form] period-form)))

(defn update-period-form [db [_ period-form]]
  (let [period-form (if (contains? period-form :bucket-id)
                      (merge period-form
                             {:bucket-label (:label
                                             (select-one
                                              [:buckets
                                               sp/ALL
                                               #(= (:id %) (:bucket-id period-form))]
                                              db))})
                      ;; ^ pulls out the label when selecting new parent
                      ;; because all that comes from the picker is id
                      period-form)]
    (transform [:forms :period-form] #(merge % period-form) db)))

(defn save-period-form [{:keys [db]} [_ date-time]]
  (let [period-form (get-in db [:forms :period-form])]
    (try
      (let [new-data          (read-string (:data period-form))
            keys-wanted       (->> period-form
                                   (keys)
                                   ;; TODO use spec to get only keys wanted
                                   (remove #(or (= :bucket-id %)
                                                (= :bucket-label %)
                                                (= :bucket-color %))))
            new-period        (-> period-form
                                  (merge {:data        new-data
                                          :last-edited date-time})
                                  (select-keys keys-wanted))
            [old-bucket
             old-period]      (select-one [:buckets sp/ALL
                                       (sp/collect-one (sp/submap [:id]))
                                       :periods sp/ALL
                                       #(= (:id %) (:id new-period))] db)
            removed-period-db (setval [:buckets sp/ALL
                                       #(= (:id %) (:id old-bucket))
                                       :periods sp/ALL
                                       #(= (:id %) (:id old-period))]
                                      sp/NONE db)
            new-db            (setval [:buckets sp/ALL
                                       ;; TODO should the bucket-id come from period form?
                                       #(= (:id %) (:bucket-id period-form))
                                       :periods
                                       sp/NIL->VECTOR
                                       sp/AFTER-ELEM]
                                      new-period removed-period-db)]

        {:db       new-db
         ;; load period form so that the data string gets re-formatted prettier
         :dispatch [:load-period-form (:id new-period)]})
      (catch js/Error e
        {:db    db
         :alert (str "Failed data json validation " e)}))))

(defn load-template-form [db [_ template-id]]
  (let [[pattern template] (select-one
                            [:patterns sp/ALL
                             (sp/collect-one (sp/submap [:id]))
                             :templates sp/ALL #(= (:id %) template-id)] db)
        bucket             (select-one [:buckets sp/ALL
                                        #(= (:bucket-id template) (:id %))] db)
        external-data      {:pattern-id   (:id pattern)
                            :bucket-id    (:id bucket)
                            :bucket-color (:color bucket)
                            :bucket-label (:label bucket)}
        template-form      (merge template
                                  external-data
                                  {:data (helpers/print-data (:data template))})]

    (assoc-in db [:forms :template-form] template-form)))

(defn load-template-form-from-pattern-planning [db [_ template-id]]
  ;; TODO this and possibly other spots _assume_ that data in the rest of the db will be there
  ;; Determine if that is an anti pattern
  (let [[pattern template] (select-one
                            [:forms :pattern-form
                             (sp/collect-one (sp/submap [:id]))
                             :templates
                             sp/ALL #( = (:id %) template-id)]
                            db)
        bucket             (select-one [:buckets sp/ALL
                                        #(= (:bucket-id template) (:id %))] db)
        external-data      {:pattern-id   (:id pattern)
                            :bucket-id    (:id bucket)
                            :bucket-color (:color bucket)
                            :bucket-label (:label bucket)}
        template-form      (merge template
                                  external-data
                                  {:data (helpers/print-data (:data template))})]

    (assoc-in db [:forms :template-form] template-form)))

(defn update-template-form [db [_ template-form]]
  (let [template-form-with-labels
        (->> template-form

             ;; add bucket-label if needed
             (fn [template-form]
               (if (contains? template-form :bucket-id)
                 (merge
                  template-form
                  {:bucket-label (:label
                                  (select-one
                                   [:buckets
                                    sp/ALL
                                    #(= (:id %) (:bucket-id template-form))]
                                   db))})
                 template-form))

             ;; add pattern-label if needed
             (fn [template-form]
               (if (contains? template-form :pattern-id)
                 (merge
                  template-form
                  {:pattern-label (:label
                                   (select-one
                                    [:patterns
                                     sp/ALL
                                     #(= (:id %) (:pattern-id template-form))]
                                    db))})template-form)))]

    (transform [:forms :template-form] #(merge % template-form) db)))

(defn save-template-form [{:keys [db]} [_ date-time]]
  (let [template-form (get-in db [:forms :template-form])]
    (try
      (let [new-data            (read-string (:data template-form))
            keys-wanted         (->> template-form
                                     (keys)
                                     (remove #(or (= :bucket-label %)
                                                  (= :bucket-color %)
                                                  (= :pattern-id %))))
            new-template        (-> template-form
                                    (merge {:data        new-data
                                            :last-edited date-time})
                                    (select-keys keys-wanted))
            [old-pattern
             old-template]      (select-one [:patterns sp/ALL
                                             (sp/collect-one (sp/submap [:id]))
                                             :templates sp/ALL
                                             #(= (:id %) (:id new-template))] db)
            removed-template-db (setval [:patterns sp/ALL
                                         #(= (:id %) (:id old-pattern))
                                         :templates sp/ALL
                                         #(= (:id %) (:id old-template))]
                                        sp/NONE db)
            new-db              (setval [:patterns sp/ALL
                                         #(= (:id %) (:pattern-id template-form))
                                         :templates
                                         sp/NIL->VECTOR
                                         sp/AFTER-ELEM]
                                        new-template removed-template-db)]

        {:db       new-db
         ;; load template form so that the data string gets re-formatted prettier
         :dispatch [:load-template-form (:id new-template)]})
      (catch js/Error e
        {:db    db
         :alert (str "Failed data read validation " e)}))))

(defn save-template-form-from-pattern-planning [{:keys [db]} [_ date-time]]
  (let [template-form (get-in db [:forms :template-form])]
    (try
      (let [new-data            (read-string (:data template-form))
            ;; TODO use the spec to get the wanted keys
            keys-wanted         (->> template-form
                                     (keys)
                                     (remove #(or (= :bucket-label %)
                                                  (= :bucket-color %)
                                                  (= :pattern-id %))))
            new-template        (-> template-form
                                    (merge {:data        new-data
                                            :last-edited date-time})
                                    (select-keys keys-wanted))
            old-template        (select-one [:forms :pattern-form
                                             :templates sp/ALL
                                             #(= (:id %) (:id new-template))] db)
            removed-template-db (setval [:forms :pattern-form
                                         :templates sp/ALL
                                         #(= (:id %) (:id old-template))]
                                        sp/NONE db)
            new-db              (setval [:forms :pattern-form
                                         :templates
                                         sp/NIL->VECTOR
                                         sp/AFTER-ELEM]
                                        new-template removed-template-db)]

        {:db       new-db
         ;; load template form so that the data string gets re-formatted prettier
         :dispatch [:load-template-form-from-pattern-planning (:id new-template)]})
      (catch js/Error e
        {:db    db
         :alert (str "Failed data read validation " e)}))))

(defn load-filter-form [db [_ filter-id]]
  (let [filter     (select-one
                    [:filters sp/ALL #(= (:id %) filter-id)] db)
        filter-form (merge filter
                           {:predicates (helpers/print-data (:predicates filter))}
                           {:sort (helpers/print-data (:sort filter))})]
    (assoc-in db [:forms :filter-form] filter-form)))

(defn update-filter-form [db [_ filter-form]]
  (transform [:forms :filter-form] #(merge % filter-form) db))

(defn save-filter-form [{:keys [db]} [_ date-time]]
  (let [filter-form (get-in db [:forms :filter-form])]
    (try
      (let [new-predicates {:predicates (read-string (:predicates filter-form))}
            new-sort {:sort (read-string (:sort filter-form))}
            new-filter        (-> filter-form
                                  (merge {:last-edited date-time}
                                         new-predicates
                                         new-sort))
            old-filter        (select-one [:filters sp/ALL
                                           #(= (:id %) (:id new-filter))] db)
            removed-filter-db (setval [:filters sp/ALL
                                       #(= (:id %) (:id old-filter))]
                                      sp/NONE db)
            new-db            (setval [:filters
                                       sp/NIL->VECTOR
                                       sp/AFTER-ELEM]
                                      new-filter removed-filter-db)]

        {:db       new-db
         ;; load filter form so that the data string gets re-formatted prettier
         :dispatch [:load-filter-form (:id new-filter)]})
      (catch js/Error e
        {:db    db
         :alert (str "Failed predicate read validation " e)}))))

(defn update-active-filter [db [_ id]]
  (assoc db :active-filter id))

(defn add-new-bucket [{:keys [db]} [_ {:keys [id now]}]]
  {:db (->> db
            (setval [:buckets
                     sp/NIL->VECTOR
                     sp/AFTER-ELEM]
                    {:id          id
                     :label       ""
                     :created     now
                     :last-edited now
                     :data        {}
                     :color       "#ff1122"
                     :templates   nil
                     :periods     nil}))
   :dispatch [:navigate-to {:current-screen :bucket
                            :params {:bucket-id id}}]})

(defn add-new-pattern [{:keys [db]} [_ {:keys [id now]}]]
  {:db       (->> db
                  (setval [:patterns
                           sp/NIL->VECTOR
                           sp/AFTER-ELEM]
                          {:id          id
                           :label       ""
                           :created     now
                           :last-edited now
                           :data        {}
                           :templates   nil}))
   :dispatch [:navigate-to {:current-screen :pattern ;; this will trigger loading
                            :params         {:pattern-id id}}]})

(defn add-new-period [{:keys [db]} [_ {:keys [bucket-id id now]}]]
  {:db (setval [:buckets sp/ALL
                #(= (:id %) bucket-id)
                :periods
                sp/NIL->VECTOR
                sp/AFTER-ELEM]
               {:id id
                :created now
                :last-edited now
                :label ""
                :data {}
                :planned true
                :start now
                :stop (new js/Date (+ (.valueOf now) (* 1000 60)))}
               db)
   :dispatch [:navigate-to {:current-screen :period
                            :params {:period-id id}}]})

;; (defn add-template-period [{:keys [db]} [_ {:keys [template id now]}]]
;;   ;; template needs bucket-id
;;   ;; TODO refactor so that this function takes in a template id (maybe bucket id)
;;   ;; and then queries the db for the template
;;   (let [new-data       (merge (:data template)
;;                               {:template-id (:id template)})
;;         start-relative (:start template)
;;         duration       (:duration template)
;;         start          (if (some? start-relative)
;;                          (new js/Date
;;                               (.getFullYear now)
;;                               (.getMonth now)
;;                               (.getDate now)
;;                               (:hour start-relative)
;;                               (:minute start-relative))
;;                          now)
;;         stop           (if (some? duration)
;;                          (new js/Date (+ (.valueOf start) duration))
;;                          (new js/Date (+ (.valueOf start) (* 1000 60))))
;;         period         (merge template
;;                               {:id    id
;;                                :data  new-data
;;                                :created now
;;                                :last-edited now
;;                                :start start
;;                                :stop  stop})
;;         period-clean   (clean-period period)]

;;     {:db       (setval [:buckets sp/ALL
;;                         #(= (:id %) (:bucket-id template))
;;                         :periods
;;                         sp/NIL->VECTOR
;;                         sp/AFTER-ELEM]
;;                        period-clean
;;                        db)
;;      :dispatch [:navigate-to {:current-screen :period
;;                               :params         {:period-id id}}]}))

(defn add-new-template [{:keys [db]} [_ {:keys [pattern-id bucket-id id now]}]]
  {:db       (setval [:patterns sp/ALL
                      #(= (:id %) pattern-id)
                      :templates
                      sp/NIL->VECTOR
                      sp/AFTER-ELEM]
                     {:id          id
                      :bucket-id   bucket-id
                      :created     now
                      :last-edited now
                      :label       ""
                      :data        {}
                      :planned     true
                      :start       (helpers/get-ms now)
                      :stop        (+ (helpers/minutes->ms 5)
                                      (helpers/get-ms now))}
                     db)
   :dispatch [:navigate-to {:current-screen :template
                            :params         {:template-id id}}]})

(defn add-new-template-to-planning-form
  [{:keys [db]} [_ {:keys [pattern-id
                           bucket-id
                           id
                           now
                           start]}]]
  {:db       (setval [:forms :pattern-form
                      :templates
                      sp/NIL->VECTOR
                      sp/AFTER-ELEM]
                     {:id          id
                      :bucket-id   bucket-id
                      :created     now
                      :last-edited now
                      :label       ""
                      :data        {}
                      :planned     true
                      :start       (helpers/get-ms start)
                      :stop        (+ (helpers/minutes->ms 60)
                                      (helpers/get-ms start))}
                     db)})

(defn add-new-filter [{:keys [db]} [_ {:keys [id now]}]]
  {:db (setval [:filters
                sp/NIL->VECTOR
                sp/AFTER-ELEM]
               {:id          id
                :label       ""
                :created     now
                :last-edited now
                :compatible []
                :sort nil
                :predicates []}
               db)
   :dispatch [:navigate-to {:current-screen :filter
                            :params {:filter-id id}}]})

(defn delete-bucket [{:keys [db]} [_ id]]
  {:db (->> db
            (setval [:buckets sp/ALL #(= id (:id %))] sp/NONE)
            (setval [:forms :bucket-form] nil))
   ;; TODO pop stack when possible
   :dispatch [:navigate-to {:current-screen :buckets}]})

(defn delete-period [{:keys [db]} [_ id]]
  {:db (->> db
            (setval [:buckets sp/ALL :periods sp/ALL #(= id (:id %))] sp/NONE)
            (setval [:forms :period-form] nil)
            (setval [:selected-period] nil))
   ;; TODO pop stack when possible
   :dispatch [:navigate-to {:current-screen :periods}]})

(defn delete-template [{:keys [db]} [_ id]]
  {:db (->> db
            (setval [:patterns sp/ALL :templates sp/ALL #(= id (:id %))] sp/NONE)
            (setval [:forms :template-form] nil)
            (setval [:forms :pattern-form] nil))
   ;; TODO pop stack when possible
   :dispatch [:navigate-to {:current-screen :templates}]})

(defn delete-template-from-pattern-planning [{:keys [db]} [_ id]]
  {:db (->> db
            (setval [:forms :pattern-form :templates
                     sp/ALL #(= id (:id %))] sp/NONE)
            (setval [:forms :template-form] nil))
   ;; TODO pop stack when possible
   :dispatch [:navigate-to {:current-screen :pattern-planning
                            :params {:pattern-id (get-in db [:forms :pattern-form :id])}}]})

(defn delete-filter [{:keys [db]} [_ id]]
  {:db (->> db
            (setval [:filters sp/ALL #(= id (:id %))] sp/NONE)
            (setval [:forms :filter-form] nil))
   ;; TODO pop stack when possible
   :dispatch [:navigate-to {:current-screen :filters}]})

(defn select-period [db [_ id]]
  (assoc-in db [:selected-period] id))

(defn select-template [db [_ id]]
  (assoc-in db [:selected-template] id))

(defn update-period [db [_ {:keys [id update-map]}]]
  ;; TODO add an interceptor? for last edited
  (transform [:buckets sp/ALL
              :periods sp/ALL
              #(= id (:id %))]
             #(merge % update-map)
             db))

(defn add-period [db [_ {:keys [period bucket-id]}]]
  (let [random-bucket-id (->> db
                              (select-one [:buckets sp/FIRST])
                              (:id))
        bucket-id (if (some? bucket-id)
                    bucket-id
                    random-bucket-id)]
    (->> db
         (setval [:buckets sp/ALL
                  #(= (:id %) bucket-id)
                  :periods
                  sp/NIL->VECTOR
                  sp/AFTER-ELEM]
                 (clean-period period)))))

(defn select-next-or-prev-period [db [_ direction]]
  (if-let [selected-period-id (get-in db [:selected-period])]
    (let [displayed-day (get-in db [:time-navigators :day])
          selected-period (select-one [:buckets sp/ALL :periods sp/ALL
                                       #(= selected-period-id (:id %))] db)
          sorted-periods (->> db
                              (select [:buckets sp/ALL :periods sp/ALL])
                              ;; Next period needs to be on this displayed day
                              (filter #(and (some? (:start %))
                                            (some? (:stop %))
                                            (or (same-day? (:start %) displayed-day)
                                                (same-day? (:stop %) displayed-day))))
                              ;; Next period needs to be visible on this track
                              (filter #(= (:planned selected-period) (:planned %)))
                              (sort-by #(.valueOf (:start %)))
                              (#(if (= direction :prev)
                                  (reverse %)
                                  %)))
          next-period    (->> sorted-periods
                              ;; Since they are sorted, drop them until you get to
                              ;; the current selected period.
                              ;; Then take the next one.
                              (drop-while #(not (= (:id %) selected-period-id)))
                              (second))]
      (if (some? next-period)
        (assoc-in db [:selected-period] (:id next-period))
        db))
    db))

(defn select-next-or-prev-template-in-form [db [_ direction]]
  (if-let [selected-template-id (get-in db [:selected-template])]
    (let [[pattern selected-template]
          (select-one [:patterns sp/ALL
                       (sp/collect-one (sp/submap [:id]))
                       :templates sp/ALL
                       #(= selected-template-id (:id %))] db)

          sorted-templates (->> db
                                (select [:patterns sp/ALL
                                         #(= (:id %) (:id pattern))
                                         :templates sp/ALL])
                              (sort-by :start)
                              (#(if (= direction :prev)
                                  (reverse %)
                                  %)))
          next-template    (->> sorted-templates
                                ;; Since they are sorted,
                                ;; drop them until you get to
                                ;; the current selected period.
                                ;; Then take the next one.
                                (drop-while
                                 #(not (= (:id %) selected-template-id)))
                                (second))]
      (if (some? next-template)
        (assoc-in db [:selected-template] (:id next-template))
        db))
    db))

(defn update-day-time-navigator [db [_ new-date]]
  (assoc-in db [:time-navigators :day] new-date))

(defn tick [db [_ date-time]]
  (let [period-in-play-id (get-in db [:period-in-play-id])]
    ;; Update period in play if there is one
    (-> (if (some? period-in-play-id)
          (transform [:buckets sp/ALL
                      :periods sp/ALL
                      #(= (:id %) period-in-play-id)]

                     #(merge % {:stop date-time})

                     db)
          db)
        ;; update now regardless
        (assoc-in [:now] date-time))))

(defn play-from-period [db [_ {:keys [id time-started new-id]}]]
  (let [[bucket-just-id
         period-to-play-from] (select-one [:buckets sp/ALL
                                           (sp/collect-one (sp/submap [:id]))
                                           :periods sp/ALL
                                           #(= (:id %) id)] db)
        new-period            (merge period-to-play-from
                                     {:id      new-id
                                      :planned false
                                      :start   time-started
                                      :stop    (->> time-started
                                                    (.valueOf)
                                                    (+ 1000)
                                                    (js/Date.))})]
    (->> db
         ;; Add new period
         (setval [:buckets sp/ALL
                  #(= (:id %) (:id bucket-just-id))
                  :periods
                  sp/NIL->VECTOR
                  sp/AFTER-ELEM]
                 new-period )
         ;; Set it as playing
         (setval [:period-in-play-id] new-id)
         ;; Set it as selected
         (setval [:selected-period] new-id))))

(defn stop-playing-period [db [_ _]]
  (assoc-in db [:period-in-play-id] nil))

(defn play-from-bucket [db [_ {:keys [bucket-id id now]}]]
  (let [new-period {:id          id
                    :planned     false
                    :start       now
                    :stop        (->> now
                                      (.valueOf)
                                      (+ 1000)
                                      (js/Date.))
                    :created     now
                    :last-edited now
                    :label       ""
                    :data        {}}]

    (->> db
         ;; Add new period
         (setval [:buckets sp/ALL
                  #(= (:id %) bucket-id)
                  :periods
                  sp/NIL->VECTOR
                  sp/AFTER-ELEM]
                 new-period )
         ;; Set it as playing
         (setval [:period-in-play-id] id)
         ;; Set it as selected
         (setval [:selected-period] id))))

(defn play-from-template [db [_ {:keys [template id now]}]]
  (let [new-period (merge template
                          {:id          id
                           :planned     false
                           :start       now
                           :stop        (->> now
                                             (.valueOf)
                                             (+ 1000)
                                             (js/Date.))
                           :created     now
                           :last-edited now})]
    (->> db
         ;; Add new period
         (setval [:buckets sp/ALL
                  #(= (:id %) (:bucket-id template))
                  :periods
                  sp/NIL->VECTOR
                  sp/AFTER-ELEM]
                 new-period )
         ;; Set it as playing
         (setval [:period-in-play-id] id)
         ;; Set it as selected
         (setval [:selected-period] id))))

(reg-fx
 :share
 (fn [app-db]
   (share (str (format-date (js/Date.)) "app-db.json") (str app-db))))

(defn share-app-db [{:keys [db]} [_ _]]
  {:db db
   :share db})

(defn import-app-db [_ [_ new-app-db]] new-app-db)

(defn add-auto-filter [db [_ filter]]
  (->> db
       (setval
        [:filters sp/NIL->VECTOR sp/AFTER-ELEM]
        filter)))

(defn pseudo-template->perfect-period [displayed-day period-to-be]
  (-> period-to-be
      (merge
       {:start
        (helpers/reset-relative-ms
         (:start period-to-be)
         displayed-day)
        :stop
        (helpers/reset-relative-ms
         (:stop period-to-be)
         displayed-day)})
      (select-keys (keys period-data-spec))))

(defn apply-pattern-to-displayed-day [db [_ {:keys [pattern-id new-periods]}]]
  (let [displayed-day     (get-in db [:time-navigators :day])
        pattern           (->> db
                               (select-one [:patterns sp/ALL
                                            #(= (:id %) pattern-id)]))

        all-bucket-ids (->> new-periods
                            (select [sp/ALL :bucket-id]))]

    ;; put the periods in the buckets
    (->> db
         (transform [:buckets sp/ALL
                     ;; find the buckets that need to be transformed
                     #(some? (some #{(:id %)} all-bucket-ids))
                     (sp/collect-one (sp/submap [:id]))
                     :periods]

                    (fn [bucket old-period-list]
                      (let [periods-to-add
                            (->> new-periods
                                 ;; select periods for this bucket
                                 (filter #(= (:bucket-id %) (:id bucket)))

                                 ;; make the periods valid
                                 (map #(pseudo-template->perfect-period
                                        displayed-day %)))]

                        ;; put the periods in the bucket
                        (into old-period-list periods-to-add)))))))

(reg-event-db :initialize-db [validate-spec] initialize-db)
(reg-event-fx :navigate-to [validate-spec persist-secure-store] navigate-to)
(reg-event-db :load-bucket-form [validate-spec persist-secure-store] load-bucket-form)
(reg-event-db :update-bucket-form [validate-spec persist-secure-store] update-bucket-form)
(reg-event-fx :save-bucket-form [alert-message validate-spec persist-secure-store] save-bucket-form)
(reg-event-db :load-period-form [validate-spec persist-secure-store] load-period-form)
(reg-event-db :update-period-form [validate-spec persist-secure-store] update-period-form)
(reg-event-fx :save-period-form [alert-message validate-spec persist-secure-store] save-period-form)
(reg-event-db :load-template-form [validate-spec persist-secure-store] load-template-form)
(reg-event-db :load-template-form-from-pattern-planning [validate-spec persist-secure-store]
              load-template-form-from-pattern-planning)
(reg-event-db :update-template-form [validate-spec persist-secure-store] update-template-form)
(reg-event-fx :save-template-form [alert-message validate-spec persist-secure-store] save-template-form)
(reg-event-fx :save-template-form-from-pattern-planning [alert-message validate-spec persist-secure-store]
              save-template-form-from-pattern-planning)
(reg-event-db :load-filter-form [validate-spec persist-secure-store] load-filter-form)
(reg-event-db :update-filter-form [validate-spec persist-secure-store] update-filter-form)
(reg-event-fx :save-filter-form [alert-message validate-spec persist-secure-store] save-filter-form)
(reg-event-db :update-active-filter [validate-spec persist-secure-store] update-active-filter)
(reg-event-fx :add-new-bucket [validate-spec persist-secure-store] add-new-bucket)
(reg-event-fx :add-new-period [validate-spec persist-secure-store] add-new-period)
;; (reg-event-fx :add-template-period [validate-spec persist-secure-store] add-template-period)
(reg-event-fx :add-new-template [validate-spec persist-secure-store] add-new-template)
(reg-event-fx :add-new-template-to-planning-form [validate-spec persist-secure-store] add-new-template-to-planning-form)
(reg-event-fx :add-new-filter [validate-spec persist-secure-store] add-new-filter)
(reg-event-fx :delete-bucket [validate-spec persist-secure-store] delete-bucket)
(reg-event-fx :delete-period [validate-spec persist-secure-store] delete-period)
(reg-event-fx :delete-template [validate-spec persist-secure-store] delete-template)
(reg-event-fx :delete-template-from-pattern-planning [validate-spec persist-secure-store]
              delete-template-from-pattern-planning)
(reg-event-fx :delete-filter [validate-spec persist-secure-store] delete-filter)
(reg-event-db :select-period [validate-spec persist-secure-store] select-period)
(reg-event-db :select-template [validate-spec persist-secure-store] select-template)
(reg-event-db :update-period [validate-spec persist-secure-store] update-period)
(reg-event-db :add-period [validate-spec persist-secure-store] add-period)
(reg-event-db :select-next-or-prev-period [validate-spec persist-secure-store] select-next-or-prev-period)
(reg-event-db :update-day-time-navigator [validate-spec persist-secure-store] update-day-time-navigator)
(reg-event-db :tick [validate-spec persist-secure-store] tick)
(reg-event-db :play-from-period [validate-spec persist-secure-store] play-from-period)
(reg-event-db :stop-playing-period [validate-spec persist-secure-store] stop-playing-period)
(reg-event-db :play-from-bucket [validate-spec persist-secure-store] play-from-bucket)
(reg-event-db :play-from-template [validate-spec persist-secure-store] play-from-template)
(reg-event-db :load-db [validate-spec] load-db)
(reg-event-fx :share-app-db [validate-spec] share-app-db)
(reg-event-db :add-auto-filter [validate-spec persist-secure-store] add-auto-filter)
(reg-event-db :load-pattern-form [validate-spec persist-secure-store] load-pattern-form)
(reg-event-db :update-pattern-form [validate-spec persist-secure-store] update-pattern-form)
(reg-event-fx :save-pattern-form [validate-spec persist-secure-store] save-pattern-form)
(reg-event-fx :add-new-pattern [validate-spec persist-secure-store] add-new-pattern)
(reg-event-db :select-next-or-prev-template-in-form [validate-spec persist-secure-store] select-next-or-prev-template-in-form)
(reg-event-db :apply-pattern-to-displayed-day [validate-spec persist-secure-store] apply-pattern-to-displayed-day)
(reg-event-db :import-app-db [validate-spec persist-secure-store] import-app-db)
