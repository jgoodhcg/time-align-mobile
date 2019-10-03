(ns time-align-mobile.node-safe-handler-functions
  (:require [time-align-mobile.helpers :as helpers :refer [same-day?
                                                           get-ms
                                                           deep-merge
                                                           bucket-path
                                                           buckets-path
                                                           combine-paths
                                                           period-selections-path
                                                           period-path-sub-bucket
                                                           period-path-insert
                                                           period-path-no-bucket-id
                                                           period-path
                                                           periods-path
                                                           template-selections-path
                                                           template-path-no-pattern-id]]
            [cljs.reader :refer [read-string]]
            [time-align-mobile.js-imports :refer [format-date]]
            [com.rpl.specter :as sp :refer-macros [select select-one setval transform selected-any?]]))

(defn clean-period [period]
  (select-keys period (keys period-data-spec)))

(defn initialize-db [_ _]
  app-db)

(defn load-db [old-db [_ db]] db)

(defn navigate-back [{:keys [db]}]
  {:go-back-nav-screen true
   :db db})

(defn navigate-to [{:keys [db]} [_ {:keys [current-screen params]}]]
  (merge {:db (-> db
                  (assoc-in [:navigation] {:current-screen current-screen
                                           :params         params})
                  ;; prevents using incompatible filters
                  (assoc-in [:active-filter] nil))}
         {:save-nav-screen {:current-screen current-screen
                            :params         params}}
         (let [dispatch
               (case current-screen
                 :bucket           [:load-bucket-form (:bucket-id params)]
                 :pattern          (when (not (:do-not-load-form params))
                                     [:load-pattern-form (:pattern-id params)])
                 :pattern-planning (when (not (:do-not-load-form params))
                                     [:load-pattern-form (:pattern-id params)])
                 :period           [:load-period-form (select-keys params [:period-id :bucket-id])]
                 :template         (if (contains? params :pattern-form-pattern-id)
                                     [:load-template-form-from-pattern-planning
                                      (:template-id params)]
                                     [:load-template-form (:template-id params)])
                 :filter           [:load-filter-form (:filter-id params)]
                 nil)]
           (when (some? dispatch)
             {:dispatch dispatch}))))

(defn load-bucket-form [db [_ bucket-id]]
  (let [bucket      (select-one (bucket-path {:bucket-id bucket-id}) db)
        bucket-form (merge bucket {:data (helpers/print-data (:data bucket))})]
    (assoc-in db [:forms :bucket-form] bucket-form)))

(defn load-pattern-form [db [_ pattern-id]]
  (let [pattern      (select-one [:patterns sp/ALL #(= (:id %) pattern-id)] db)
        pattern-form (merge pattern {:data (helpers/print-data (:data pattern))})]
    (-> db
     (assoc-in [:forms :pattern-form] pattern-form)
     (assoc-in [:selected-template] nil))))

(defn update-bucket-form [db [_ bucket-form]]
  (transform [:forms :bucket-form] #(merge % bucket-form) db))

(defn update-pattern-form [{:keys [db]} [_ pattern-form]]
  (let [selected-template (:selected-template db)]
    (merge
     {:db (transform [:forms :pattern-form] #(merge % pattern-form) db)}
     (when (some? selected-template)
       {:dispatch [:load-template-form-from-pattern-planning selected-template]}))))

(defn save-bucket-form [{:keys [db]} [_ date-time]]
  (let [bucket-form (get-in db [:forms :bucket-form])]
    (try
       (let [new-data (read-string (:data bucket-form))
             new-bucket (merge bucket-form {:data new-data
                                       :last-edited date-time})
            new-db (setval (bucket-path {:bucket-id (:id new-bucket)})
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

(defn load-period-form [db [_ {:keys [period-id bucket-id]}]]
  (let [[sub-bucket period] (->> db
                                 (select-one
                                  (period-path-sub-bucket {:period-id period-id
                                                           :bucket-id bucket-id})))
        sub-bucket-remap    {:bucket-id    (:id sub-bucket)
                             :bucket-color (:color sub-bucket)
                             :bucket-label (:label sub-bucket)}
        period-form         (merge period
                                   {:data (helpers/print-data (:data period))}
                                   sub-bucket-remap)]

    (assoc-in db [:forms :period-form] period-form)))

(defn update-period-form [db [_ period-form]]
  (let [period-form (if (contains? period-form :bucket-id)
                      (let [bucket (select-one
                                    (bucket-path {:bucket-id (:bucket-id period-form)})
                                    db)]
                        (merge period-form
                               {:bucket-label (:label bucket)
                                :bucket-color (:color bucket)}))
                      ;; ^ pulls out the label when selecting new parent
                      ;; because all that comes from the picker is id
                      period-form)]
    (transform [:forms :period-form] #(merge % period-form) db)))

(defn save-period-form [{:keys [db]} [_ date-time]]
  (let [period-form (get-in db [:forms :period-form])]
    (try
      (let [bucket-id         (:bucket-id period-form)
            period-id         (:id period-form)
            new-data          (read-string (:data period-form))
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
             old-period]      (->> db
                                   (select-one
                                    (period-path-no-bucket-id {:period-id period-id})))

            removed-period-db (->> db
                                   (setval
                                    (period-path-sub-bucket {:period-id period-id
                                                             :bucket-id (:id old-bucket)})
                                    sp/NONE))
            new-db            (->> removed-period-db
                                   (setval
                                    (period-path-insert {:bucket-id bucket-id
                                                         :period-id (:id new-period)})
                                    new-period))]

        {:db       new-db
         ;; load period form so that the data string gets re-formatted prettier
         :dispatch [:load-period-form {:period-id (:id new-period)
                                       :bucket-id bucket-id}]})
      (catch js/Error e
        {:db    db
         :alert (str "Failed data json validation " e)}))))

(defn load-template-form [db [_ template-id]]
  (let [[pattern template] (select-one
                            [:patterns sp/ALL
                             (sp/collect-one (sp/submap [:id]))
                             :templates sp/ALL #(= (:id %) template-id)] db)
        bucket             (select-one
                            (bucket-path {:bucket-id (:bucket-id template)})
                            db)
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
        bucket             (select-one
                            (bucket-path {:bucket-id (:bucket-id template)})
                            db)
        external-data      {:pattern-id   (:id pattern)
                            :bucket-id    (:id bucket)
                            :bucket-color (:color bucket)
                            :bucket-label (:label bucket)}
        template-form      (merge template
                                  external-data
                                  {:data (helpers/print-data (:data template))})]

    ;; (println (select [:forms :pattern-form :templates sp/ALL (sp/submap [:label :id])] db))
    (assoc-in db [:forms :template-form] template-form)))

(defn update-template-form [db [_ template-form]]
  (let [template-form-with-labels
        (->> template-form
             ((fn [template-form]
                ;; add bucket label + color if needed
                (if (contains? template-form :bucket-id)
                  (let [bucket (select-one
                                (bucket-path {:bucket-id (:bucket-id template-form)})
                                db)]
                    (merge
                     template-form
                     {:bucket-label (:label bucket)
                      :bucket-color (:color bucket)}))

                  template-form)))

             ;; add pattern-label if needed
             ((fn [template-form]
                (if (contains? template-form :pattern-id)
                  (merge
                   template-form
                   {:pattern-label (:label
                                    (select-one
                                     [:patterns
                                      sp/ALL
                                      #(= (:id %) (:pattern-id template-form))]
                                     db))})
                  template-form))))]

    (transform [:forms :template-form] #(merge % template-form-with-labels) db)))

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
            (setval [:buckets (sp/keypath id)]
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

(defn add-new-period
  "Difference between this and add-period is that this creates a blank period from just an id and a timestamp"
  [{:keys [db]} [_ {:keys [bucket-id id now]}]]

  (let [random-bucket-id (->> db
                              (select-one (buckets-path))
                              first
                              (:id))
        bucket-id (if (some? bucket-id)
                    bucket-id
                    random-bucket-id)]
    {:db (setval (period-path-insert {:bucket-id bucket-id
                                      :period-id id})
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
                              :params {:period-id id
                                       :bucket-id bucket-id}}]}))

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
                           planned
                           now
                           start]}]]
  {:db (setval [:forms :pattern-form
                :templates
                sp/NIL->VECTOR
                sp/AFTER-ELEM]
               {:id          id
                :bucket-id   bucket-id
                :created     now
                :last-edited now
                :planned     planned
                :label       ""
                :data        {}
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
            (setval (bucket-path {:bucket-id id}) sp/NONE)
            (setval [:forms :bucket-form] nil)
            (setval [:patterns sp/ALL :templates sp/ALL
                     #(= id (:bucket-id %))]  sp/NONE)) ;; TODO think about removing it from forms too?
   ;; TODO pop stack when possible
   :dispatch [:navigate-to {:current-screen :buckets}]})

(defn delete-period [{:keys [db]} [_ {:keys [period-id bucket-id]}]]
  {:db         (->> db
                    (setval (period-path {:bucket-id bucket-id
                                          :period-id period-id}) sp/NONE)
                    (setval [:forms :period-form] nil) ;; it must be deleted from the form
                    (#(if (= (:period-in-play-id db) period-id)  ;; it _may_ be in play when it is deleted
                        (setval [:period-in-play-id] nil %)
                        %))
                    (setval [:selection :period] {:movement {:bucket-id nil
                                                             :period-id nil}
                                                  :edit     {:bucket-id nil
                                                             :period-id nil}}))
   ;; TODO pop stack when possible
   :dispatch-n [[:navigate-to {:current-screen :day}]
                [:select-period-movement {:bucket-id nil
                                          :period-id nil}]
                [:select-period-edit {:bucket-id nil
                                      :period-id nil}]]})

(defn delete-template [{:keys [db]} [_ id]]
  (merge
   {:db (->> db
             (setval [:patterns sp/ALL :templates sp/ALL #(= id (:id %))] sp/NONE)
             (setval [:forms :template-form] nil)
             (setval [:forms :pattern-form] nil))}
   ;; TODO pop stack when implemented
   (when (= :template (-> db :navigation :current-screen))
     {:dispatch [:navigate-to {:current-screen :templates}]})))

(defn delete-pattern [{:keys [db]} [_ id]]
  (merge
   {:db (->> db
             (setval [:patterns sp/ALL #(= id (:id %))] sp/NONE)
             (setval [:forms :pattern-form] nil))}
   ;; TODO pop stack when implemented
   (when (= :pattern (-> db :navigation :current-screen))
     {:dispatch [:navigate-to {:current-screen :patterns}]})))

(defn delete-template-from-pattern-planning [{:keys [db]} [_ id]]
  (merge
   {:db (->> db
             (setval [:forms :pattern-form :templates
                      sp/ALL #(= id (:id %))] sp/NONE)
             (setval [:forms :template-form] nil))}

   ;; TODO pop stack when implemented
   (when (= :template (-> db :navigation :current-screen))
     {:dispatch [:navigate-to {:current-screen :pattern-planning
                               :params {:pattern-id (get-in db [:forms :pattern-form :id])}}]})))

(defn delete-filter [{:keys [db]} [_ id]]
  {:db (->> db
            (setval [:filters sp/ALL #(= id (:id %))] sp/NONE)
            (setval [:forms :filter-form] nil))
   ;; TODO pop stack when possible
   :dispatch [:navigate-to {:current-screen :filters}]})

(defn select-period-movement
  [{:keys [db]} [_ {:keys [bucket-id period-id]}]]
  (merge
   {:db (assoc-in db [:selection :period :movement] {:period-id period-id
                                                     :bucket-id bucket-id})}))

(defn select-period-edit
  [{:keys [db]} [_ {:keys [bucket-id period-id]}]]
  (merge
   {:db (assoc-in db [:selection :period :edit] {:period-id period-id
                                                 :bucket-id bucket-id})}
   (when (some? period-id)
     {:dispatch [:load-period-form {:bucket-id bucket-id
                                    :period-id period-id}]})))

(defn select-template-movement
  [{:keys [db]} [_ {:keys [bucket-id template-id]}]]
  (merge
   {:db (assoc-in db [:selection :template :movement] {:bucket-id bucket-id
                                                       :template-id template-id})}))

(defn select-template-edit
  [{:keys [db]} [_ {:keys [bucket-id template-id]}]]
  (merge
   {:db (assoc-in db [:selection :template :edit] {:template-id template-id
                                                   :bucket-id bucket-id})}
   (when (some? template-id)
     {:dispatch [:load-template-form-from-pattern-planning template-id]})))

(defn update-period [{:keys [db]} [_ {:keys [period-id bucket-id update-map]}]]
  ;; TODO add an interceptor? for last edited
  (merge
   {:db (->> db (transform
                 (period-path {:bucket-id bucket-id
                               :period-id period-id})
                 #(merge % update-map)))}
   (when (selected-any? (combine-paths (period-selections-path)
                                       [#(= % period-id)])
                       db)
     {:dispatch [:load-period-form {:period-id period-id
                                    :bucket-id bucket-id}]})))

(defn add-period
  "Difference between this and add-new-period is that this takes a full period (like from a template)"
  [db [_ {:keys [period bucket-id]}]]
  (let [random-bucket-id (->> db
                              (select (buckets-path))
                              (first)
                              (:id))
        bucket-id (if (some? bucket-id)
                    bucket-id
                    random-bucket-id)]
    (->> db
         (setval (period-path-insert {:bucket-id bucket-id
                                      :period-id (:id period)})
                 (clean-period period)))))

(defn update-day-time-navigator [db [_ new-date]]
  (assoc-in db [:time-navigators :day] new-date))

(defn tick [{:keys [db]} [_ date-time]]
  (let [period-in-play-id       (get-in db [:period-in-play-id])
        [bucket period-in-play] (->> db
                                     (select-one
                                      (period-path-no-bucket-id
                                       {:period-id period-in-play-id})))
        selected-period-id      (get-in db [:selection :period :edit :period-id])]
    ;; Update period in play if there is one
    (merge
     {:db
      (-> (if (some? period-in-play-id)
            (->> db
                 (transform (period-path {:bucket-id (:id bucket)
                                          :period-id period-in-play-id})
                            #(merge % {:stop date-time})))
            db)

          ;; update now regardless
          (assoc-in [:now] date-time))}

     ;; Decided not to do this since it would overwrite anything the user is editing
     ;; (when (and (some? period-in-play)
     ;;            (= selected-period-id period-in-play-id))
     ;;   {:dispatch [:load-period-form period-in-play-id]})
     )))

(defn play-from-period [{:keys [db]} [_ {:keys [id time-started new-id]}]]
  (let [[bucket
         period-to-play-from] (->> db
                                   (select-one
                                    (period-path-no-bucket-id {:period-id id})))
        new-period            (merge period-to-play-from
                                     {:id      new-id
                                      :planned false
                                      :start   time-started
                                      :stop    (->> time-started
                                                    (.valueOf)
                                                    (+ (helpers/minutes->ms 5))
                                                    (js/Date.))})]
    {:db
     (->> db
          ;; Add new period
          (setval
           (period-path-insert {:bucket-id (:id bucket)
                                :period-id new-id})
           new-period )
          ;; Set it as playing
          (setval [:period-in-play-id] new-id))
     :dispatch [:select-period-edit {:bucket-id (:id bucket)
                                     :period-id new-id}]}))

(defn stop-playing-period [db [_ _]]
  (assoc-in db [:period-in-play-id] nil))

(defn play-from-bucket [{:keys [db]} [_ {:keys [bucket-id id now]}]]
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

    {:db
     (->> db
          ;; Add new period
          (setval (period-path-insert {:bucket-id bucket-id
                                       :period-id id})
                  new-period )
          ;; Set it as playing ;; TODO make a handler for this and dispatch it
          (setval [:period-in-play-id] id))
     ;; Set it as selected edit
     :dispatch-n [[:select-period-edit {:bucket-id bucket-id
                                        :period-id id}]]}))

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


(defn share-app-db [{:keys [db]} [_ _]]
  {:db db
   :share db})

(defn import-app-db [_ [_ new-app-db]]
  (deep-merge app-db new-app-db))

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
         (transform
          [:buckets sp/MAP-VALS
           #(some? (some #{(:id %)} all-bucket-ids))
           (sp/collect-one (sp/submap [:id]))
           :periods]

          (fn [bucket old-period-map]
            (let [periods-to-add
                  (->> new-periods
                       ;; select periods for this bucket
                       (filter #(= (:bucket-id %) (:id bucket)))
                       ;; make the periods valid
                       (map #(pseudo-template->perfect-period
                              displayed-day %))
                       ;; turn list into a map indexed by id
                       ((fn [period-list]
                          (apply merge (->> period-list
                                            (map #(hash-map (:id %) %)))))))]
              ;; put the periods in the bucket
              (merge old-period-map periods-to-add)))))))

(defn update-template-on-pattern-planning-form
  ":id needs to be in update map"
  ;; TODO update pramams to take template id separately
  [{:keys [db]} [_ update-map]]
  (let [id (:id update-map)]
    (merge
     {:db  (->> db
                (transform
                 [:forms :pattern-form :templates sp/ALL #(= (:id %) id)]
                 (fn [template] (merge template update-map))))}

     (when (selected-any? (combine-paths (template-selections-path)
                                         [#(= % id)])
                          db)
       {:dispatch [:load-template-form-from-pattern-planning id]}))))

(defn make-pattern-from-day [db [_ {:keys [date now]}]]
  (let [periods       (select (combine-paths
                               (periods-path)
                               [#(and (or (same-day? date (:start %))
                                          (same-day? date (:stop %))))])
                              db)
        new-templates (->> periods
                           (map (fn [[bucket {:keys [label data start stop planned]}]]
                                  (let [start-rel (get-ms start)
                                        stop-rel  (get-ms stop)]

                                    {:id          (random-uuid) ;; TODO remove this to make function pure
                                     :bucket-id   (:id bucket)
                                     :label       label
                                     :created     now
                                     :planned     planned
                                     :last-edited now
                                     :data        data
                                     :start       (if (> stop-rel start-rel) ;; this will catch the chance that start is relatively later than stop (is on the day before)
                                                    start-rel
                                                    (min (- stop-rel 1000) 0))
                                     :stop        stop-rel}))))
        new-pattern {:id          (random-uuid) ;; TODO remove this to make function pure
                     :label       (str (format-date date) " generated pattern")
                     :created     now
                     :last-edited now
                     :data        {}
                     :templates   new-templates}]
    (->> db
         (setval [:patterns
                  sp/AFTER-ELEM]
                 new-pattern))))

(defn set-current-pixel-to-minute-ratio [db [_ ratio]]
  (setval [:config :pixel-to-minute-ratio :current] (max ratio 0.25) db))

(defn set-default-pixel-to-minute-ratio [db [_ ratio]]
  (setval [:config :pixel-to-minute-ratio :default] ratio db))

(defn select-element-movement [context [dispatch-key {:keys [element-type element-id bucket-id pattern-id]}]]
  (case element-type
    ;; :select-period-movement << for searching usage of that dispatch key
    :period   (select-period-movement context [dispatch-key {:period-id element-id
                                                             :bucket-id bucket-id}])
    ;; :select-template-movement << for searching usage of that dispatch key
    :template (select-template-movement context [dispatch-key {:template-id element-id
                                                               :bucket-id   bucket-id}])))

(defn select-element-edit [context [dispatch-key {:keys [element-type element-id bucket-id pattern-id]}]]
  (case element-type
    ;; :select-period-edit << for searching usage of that dispatch key
    :period   (select-period-edit context [dispatch-key {:period-id element-id
                                                         :bucket-id bucket-id}])
    ;; :select-template-edit << for searching usage of that dispatch key
    :template (select-template-edit context [dispatch-key {:template-id element-id
                                                           :bucket-id bucket-id}])))

(defn select-next-or-prev-period [{:keys [db]} [_ direction]]
  (if-let [selected-period-id (get-in db [:selection :period :edit :period-id])]
    (let [displayed-day            (get-in db [:time-navigators :day])
          [bucket selected-period] (->> db
                                        (select-one
                                         (period-path-no-bucket-id
                                          {:period-id selected-period-id})))
          sorted-periods           (->> db
                                        (select (periods-path))
                                        ;; List from this point looks like
                                        ;; [[{bucket} {period}]
                                        ;;  [{bucket} {period}] ... ]
                                        ;; Next period needs to be on this displayed day
                                        (filter #(and (some? (:start (second %)))
                                                      (some? (:stop (second %)))
                                                      (or (same-day? (:start (second %)) displayed-day)
                                                          (same-day? (:stop (second %)) displayed-day))))
                                        ;; Next period needs to be visible on this track
                                        (filter #(= (:planned selected-period) (:planned (second %))))
                                        (sort-by #(.valueOf (:start (second %))))
                                        (#(if (= direction :prev)
                                            (reverse %)
                                            %)))
          [next-bucket next-period]              (->> sorted-periods
                                        ;; Since they are sorted, drop them until you get to
                                        ;; the current selected period.
                                        ;; Then take the next one.
                                        (drop-while #(not (= (:id (second %)) selected-period-id)))
                                        (second))]
      (merge
       {:db db}
       (when (some? next-period)
         {:dispatch [:select-period-edit {:bucket-id (:id next-bucket)
                                          :period-id (:id next-period)}]})))
    {:db db}))

(defn select-next-or-prev-template-in-form [{:keys [db]} [_ direction]] ;; TODO add pattern form to docs or name
  (if-let [selected-template-id (get-in db [:selection :template :edit :template-id])]
    (let [[pattern selected-template]
          (select-one [:forms :pattern-form
                       (sp/collect-one (sp/submap [:id]))
                       :templates sp/ALL
                       #(= selected-template-id (:id %))] db)

          sorted-templates (->> db
                                (select [:forms :pattern-form
                                         :templates sp/ALL])
                                (filter
                                 #(= (:planned selected-template)
                                     (:planned %)))
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
      (merge {:db db}
             (when (some? next-template)
               {:dispatch [:select-template-edit
                           {:template-id (:id next-template)
                            :bucket-id   (:bucket-id next-template)}]})))
    {:db db})) ;; if nothings is selected then why is this handler called?

