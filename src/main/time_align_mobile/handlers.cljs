(ns time-align-mobile.handlers
  (:require
    [time-align-mobile.js-imports :refer [write-file-to-dd! alert share format-date]]
    [re-frame.core :refer [reg-event-db ->interceptor reg-event-fx reg-fx dispatch]]
    ;; [zprint.core :refer [zprint]]
    [cljs.reader :refer [read-string]]
    [clojure.spec.alpha :as s]
    [time-align-mobile.db :as db :refer [app-db app-db-spec period-data-spec]]
    [time-align-mobile.helpers :as helpers :refer [same-day? get-ms deep-merge]]
    [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]))

(def navigation-history (atom []))

;; -- Interceptors ----------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/blob/develop/docs/Interceptors.md
;;
(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db event]
  (when-not (s/valid? spec db)
    (let [explaination (s/explain-str spec db)]
      ;; (zprint (::clojure.spec.alpha/problems explaination) {:map {:force-nl? true}})
      (println explaination)
      (println (str "Failed on event - " event))
      ;; (throw (ex-info (str "Spec check failed: " explain-data) explain-data))
      (alert "Failed spec validation" "Check the command line output.")
      true)))

(def validate-spec
  (if true ;;goog.DEBUG ;; TODO reinstate this after pre-alpha
    (->interceptor
        :id :validate-spec
        :after (fn [context]
                 (let [db (-> context :effects :db)
                       old-db (-> context :coeffects :db)
                       event (-> context :coeffects :event)]
                   (if (some? (check-and-throw app-db-spec db event))
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

(def persist-secure-store ;; TODO rename this to reflect file storage instead of secure store
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

(reg-fx
 :save-nav-screen
 (fn [new-screen]
   (swap! navigation-history conj new-screen)))

(reg-fx
 :go-back-nav-screen
 (fn [_]
   (let [[previous-screen _] (take-last 2 @navigation-history)]
     (if (some? previous-screen)
       (do
         (swap! navigation-history #(do (drop-last 2 %)))
         (dispatch [:navigate-to previous-screen]))))))

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
                 :period           [:load-period-form (:period-id params)]
                 :template         (if (contains? params :pattern-form-pattern-id)
                                     [:load-template-form-from-pattern-planning
                                      (:template-id params)]
                                     [:load-template-form (:template-id params)])
                 :filter           [:load-filter-form (:filter-id params)]
                 nil)]
           (when (some? dispatch)
             {:dispatch dispatch}))))

(defn load-bucket-form [db [_ bucket-id]]
  (let [bucket      (select-one [:buckets sp/ALL #(= (:id %) bucket-id)] db)
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
                      (let [bucket (select-one
                                    [:buckets
                                     sp/ALL
                                     #(= (:id %) (:bucket-id period-form))]
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

    (println (select [:forms :pattern-form :templates sp/ALL (sp/submap [:label :id])] db))
    (assoc-in db [:forms :template-form] template-form)))

(defn update-template-form [db [_ template-form]]
  (let [template-form-with-labels
        (->> template-form

             ((fn [template-form]
                ;; add bucket label + color if needed
                (if (contains? template-form :bucket-id)
                  (let [bucket (select-one
                                [:buckets
                                 sp/ALL
                                 #(= (:id %) (:bucket-id template-form))]
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
            (setval [:forms :bucket-form] nil)
            (setval [:patterns :templates sp/ALL
                     #(= id (:bucket-id %))]  sp/NONE)) ;; TODO think about removing it from forms too?
   ;; TODO pop stack when possible
   :dispatch [:navigate-to {:current-screen :buckets}]})

(defn delete-period [{:keys [db]} [_ id]]
  {:db (->> db
            (setval [:buckets sp/ALL :periods sp/ALL #(= id (:id %))] sp/NONE)
            (setval [:forms :period-form] nil) ;; it must be deleted from the form
            (setval [:selected-period] nil)    ;; it must be selected if it is deleted
            (#(if (= (:period-in-play-id db) id)  ;; it _may_ be in play when it is deleted
                (setval [:period-in-play-id] nil %)
                %)))
   ;; TODO pop stack when possible
   :dispatch [:navigate-to {:current-screen :day}]})

(defn delete-template [{:keys [db]} [_ id]]
  (merge
   {:db (->> db
             (setval [:patterns sp/ALL :templates sp/ALL #(= id (:id %))] sp/NONE)
             (setval [:forms :template-form] nil)
             (setval [:forms :pattern-form] nil))}
   ;; TODO pop stack when implemented
   (when (= :template (-> db :navigation :current-screen))
     {:dispatch [:navigate-to {:current-screen :templates}]})))

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
  [{:keys [db]} [_ id]]
  (merge
   {:db (assoc-in db [:selection :period :movement] id)}))

(defn select-period-edit
  [{:keys [db]} [_ id]]
  (merge
   {:db (assoc-in db [:selection :period :edit] id)}
   (when (some? id)
     {:dispatch [:load-period-form id]})))

(defn select-template-movement
  [{:keys [db]} [_ id]]
  (merge
   {:db (assoc-in db [:selection :template :movement] id)}))

(defn select-template-edit
  [{:keys [db]} [_ id]]
  (merge
   {:db (assoc-in db [:selection :template :edit] id)}
   (when (some? id)
     {:dispatch [:load-template-form id]})))

(defn update-period [{:keys [db]} [_ {:keys [id update-map]}]]
  ;; TODO add an interceptor? for last edited
  (merge
   {:db (transform [:buckets sp/ALL
                    :periods sp/ALL
                    #(= id (:id %))]
                   #(merge % update-map)
                   db)}
   (when (= (:selected-period db) id)
     {:dispatch [:load-period-form id]})))

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
   (share (str (format-date (js/Date.)) "-app-db.edn") (str app-db))))

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

(defn update-template-on-pattern-planning-form [db [_ update-map]]
  (let [id (:id update-map)]
    (->> db
         (transform
          [:forms :pattern-form :templates sp/ALL #(= (:id %) id)]
          (fn [template] (merge template update-map))))))

(defn make-pattern-from-day [db [_ {:keys [date planned now]}]]
  (let [periods       (select [:buckets sp/ALL
                               (sp/collect-one (sp/submap [:id :color :label]))
                               :periods sp/ALL
                               #(and (or (same-day? date (:start %))
                                         (same-day? date (:stop %)))
                                     (= (:planned %) planned))]
                              db)
        new-templates (->> periods
                           (map (fn [[bucket {:keys [label data start stop]}]]
                                  (let [start-rel (get-ms start)
                                        stop-rel (get-ms stop)]

                                    {:id          (random-uuid) ;; TODO this needs to not be here
                                     :bucket-id   (:id bucket)
                                     :label       label
                                     :created     now
                                     :last-edited now
                                     :data        data
                                     :start       (if (> stop-rel start-rel) ;; this will catch the chance that start is relatively later than stop (is on the day before)
                                                    start-rel
                                                    (min (- stop-rel 1000) 0))
                                     :stop        stop-rel}))))
        new-pattern {:id          (random-uuid) ;; TODO this needs to not be here
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
  (setval [:config :pixel-to-minute-ratio :current] ratio db))

(defn set-default-pixel-to-minute-ratio [db [_ ratio]]
  (setval [:config :pixel-to-minute-ratio :default] ratio db))

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
(reg-event-fx :update-period [validate-spec persist-secure-store] update-period)
(reg-event-db :add-period [validate-spec persist-secure-store] add-period)
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
(reg-event-fx :update-pattern-form [validate-spec persist-secure-store] update-pattern-form)
(reg-event-fx :save-pattern-form [validate-spec persist-secure-store] save-pattern-form)
(reg-event-fx :add-new-pattern [validate-spec persist-secure-store] add-new-pattern)
(reg-event-db :apply-pattern-to-displayed-day [validate-spec persist-secure-store] apply-pattern-to-displayed-day)
(reg-event-db :import-app-db [validate-spec persist-secure-store] import-app-db)
(reg-event-fx :navigate-back [validate-spec persist-secure-store] navigate-back)
(reg-event-db :update-template-on-pattern-planning-form [validate-spec persist-secure-store]
              update-template-on-pattern-planning-form)
(reg-event-db :make-pattern-from-day [validate-spec persist-secure-store] make-pattern-from-day)
(reg-event-db :set-current-pixel-to-minute-ratio [validate-spec persist-secure-store] set-current-pixel-to-minute-ratio)
(reg-event-db :set-default-pixel-to-minute-ratio [validate-spec persist-secure-store] set-default-pixel-to-minute-ratio)
(reg-event-fx :select-period-movement [validate-spec persist-secure-store] select-period-movement)
(reg-event-fx :select-period-edit [validate-spec persist-secure-store] select-period-edit)
(reg-event-fx :select-template-movement [validate-spec persist-secure-store] select-template-movement)
(reg-event-fx :select-template-edit [validate-spec persist-secure-store] select-template-edit)
