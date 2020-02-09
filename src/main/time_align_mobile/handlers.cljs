(ns time-align-mobile.handlers
  (:require
   [time-align-mobile.js-imports :refer [write-file-to-dd!
                                         alert
                                         version
                                         share
                                         format-date
                                         email-export
                                         share-file!
                                         amplitude-log-event-with-properties]]
    [re-frame.core :refer [reg-event-db ->interceptor reg-event-fx reg-fx dispatch]]
    ;; [zprint.core :refer [zprint]]
    [cljs.reader :refer [read-string]]
    [clojure.spec.alpha :as s]
    [time-align-mobile.db :as db :refer [app-db app-db-spec period-data-spec]]
    [time-align-mobile.components.day :refer [snap-bottom-sheet bottom-sheet-ref]]
    [time-align-mobile.subs :as subs]
    [time-align-mobile.components.day :refer [snap-bottom-sheet]]
    [time-align-mobile.helpers :as helpers :refer [same-day?
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
    [com.rpl.specter :as sp :refer-macros [select select-one setval transform selected-any?]]))

(def navigation-history (atom []))

(def app-db-persisted-file-name "app-db")
;; -- Interceptors ----------------------------------------------------------
;;
;; See https://github.com/Day8/re-frame/blob/develop/docs/Interceptors.md
;;
(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db event]
  (when-not (s/valid? spec db)
    (let [explanation (s/explain-str spec db)]
      ;; (zprint (::clojure.spec.alpha/problems explaination) {:map {:force-nl? true}})
      (println explanation)
      (println (str "Failed on event - " event))
      ;; (throw (ex-info (str "Spec check failed: " explain-data) explain-data))
      (alert "Failed spec validation" "Check the command line output.")
      true)))

(def validate-spec
  (if goog.DEBUG
    (->interceptor
        :id :validate-spec
        :after (fn [context]
                 context ;; TODO what does this do?
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
            (write-file-to-dd!
             app-db-persisted-file-name
             (-> context :effects :db str))
            context)))

(def amplitude-logging
  (if (not goog.DEBUG)
      ;; Only log to amplitude in non dev env
    (->interceptor
     :id :amplitude-logging
     :after (fn [context]
              (let [event-name     (-> context :coeffects :event first str)
                    next-screen    (if (= event-name ":navigate-to")
                                     (-> context :coeffects :event second :current-screen str)
                                     "na")
                    current-screen (-> context :coeffects :db :navigation :current-screen str)]

                (amplitude-log-event-with-properties event-name {:version        version
                                                                 :next-screen    next-screen
                                                                 :current-screen current-screen})
                context)))
    (->interceptor)))

(def generate-tests-flag false)

(def print-once-flag false)

(def generated-test-keys (atom #{}))

(defn print-once [event test-str]
  (swap! generated-test-keys
         (fn [test-keys-set]
           (let [new-test-keys-set (conj test-keys-set event)
                 old-count         (count test-keys-set)
                 new-count         (count new-test-keys-set)]

             (when (and print-once-flag
                        (-> new-count (> old-count)))
               (println test-str))

             (when (not print-once-flag)
               (println test-str))

             new-test-keys-set))))

(def generate-handler-test-fx
  (->interceptor
   :id :generate-handler-test-fx
   :after (fn [context]
            (if generate-tests-flag
              (let [event     (get-in context [:coeffects :event])
                    db-before (get-in context [:coeffects :db])
                    effects   (get-in context [:effects])]

                (let [handler-name (name (first event))
                      test-name    (str "handler " handler-name " generated test")]

                  (print-once handler-name
                              (str
                               "(js/test " "\"" test-name "\" "
                               "#(-> (handlers/" handler-name " "
                               "{:db " db-before "}"
                               event ")"
                               "(str)"
                               "(js/expect) "
                               "(.toBe (->> " effects
                               "(str)"
                               "))))"))

                  context))
              context))))

(def generate-handler-test-db
  (->interceptor
   :id :generate-handler-test-db
   :after (fn [context]
            (if generate-tests-flag
              (let [event     (get-in context [:coeffects :event])
                    db-before (get-in context [:coeffects :db])
                    effects   (get-in context [:effects])]

                (let [handler-name (name (first event))
                      test-name    (str "handler " handler-name " generated test")]

                  (print-once handler-name
                              (str
                               "(js/test " "\"" test-name "\" "
                               "#(-> (handlers/" handler-name " "
                               db-before
                               event ")"
                               "(str)"
                               "(js/expect) "
                               "(.toBe (->> " (:db effects)
                               "(str)"
                               "))))"))

                  context))
              context))))

;; -- Helpers ---------------------------------------------------------------
(defn clean-period [period]
  (select-keys period (keys period-data-spec)))

;; -- Handlers --------------------------------------------------------------

(defn initialize-db [_ _]
  app-db)

(defn load-db [old-db [_ db]] db)

(reg-fx
 :save-nav-screen
 (fn [new-screen]
   (swap! navigation-history conj new-screen)))

(reg-fx
 :go-back-nav-screen
 ;; TODO One obvious flaw with this is that when the app opens with no history the the current screen cannot be "back buttoned" to.
 ;; A quick fix would be on load dispatch the navigating to the current screen so it gets put in the navigation atom.
 ;; A long term fix could be to move history into state, then it could persist between sessions too.
 (fn [_]
   (let [[previous-screen _] (take-last 2 @navigation-history)]
     (if (some? previous-screen)
       (do
         (swap! navigation-history #(do (drop-last 1 %))) ;; used to drop 2 with a regular navigation but for some reason it didn't seem to work the same
         (dispatch [:navigate-to-no-history previous-screen]))))))

(defn navigate-back [{:keys [db]}]
  {:go-back-nav-screen true
   :db db})

(defn navigate-to [{:keys [db]} [dispatch-key {:keys [current-screen params]}]]
  (merge {:db (-> db
                  (assoc-in [:navigation] {:current-screen current-screen
                                           :params         params})
                  ;; prevents using incompatible filters
                  (assoc-in [:active-filter] nil))}

         (when (not= :navigate-to-no-history dispatch-key)
           {:save-nav-screen {:current-screen current-screen
                              :params         params}})
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
  {:db       (setval [:forms :pattern-form
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
                     db)
   ;; TODO make a separate function that wraps for selection (like with periods)
   :dispatch [:select-element-edit {:element-type :template
                                    :bucket-id    bucket-id
                                    :element-id   id}]})

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

(reg-fx
 :open-bottom-sheet
 (fn [position]
   (snap-bottom-sheet position)))

(defn select-period-edit
  [{:keys [db]} [_ {:keys [bucket-id period-id]}]]
  (merge
   {:db (assoc-in db [:selection :period :edit] {:period-id period-id
                                                 :bucket-id bucket-id})}
   (when (some? period-id)
     {:dispatch          [:load-period-form {:bucket-id bucket-id
                                             :period-id period-id}]
      :open-bottom-sheet 1})))

(defn select-template-movement
  [{:keys [db]} [_ {:keys [bucket-id template-id]}]]
  (merge
   {:db (assoc-in db [:selection :template :movement] {:bucket-id bucket-id
                                                       :template-id template-id})}))

(defn select-template-edit
  [{:keys [db]} [_ {:keys [bucket-id template-id]}]]
  (merge
   {:db (assoc-in db [:selection :template :edit] {:bucket-id   bucket-id
                                                   :template-id template-id})}
   (when (some? template-id)
     {:dispatch          [:load-template-form-from-pattern-planning template-id]
      :open-bottom-sheet 1})))

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
  "Difference between this and add-new-period is that this takes a full period (like from a template). Period-id must be unique!"
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

(defn add-period-with-selection
  [{:keys [db]} [_ {:keys [period bucket-id]}]]
  (merge
   {:db (add-period db [:no-op {:period period
                                :bucket-id bucket-id}])}
   {:dispatch [:select-element-edit {:bucket-id bucket-id
                                     :element-type :period
                                     :element-id (:id period)}]}))

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
                                                    (+ (helpers/minutes->ms 0.1))
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

(reg-fx
 :share
 (fn [app-db]
   (share-file! app-db-persisted-file-name)))

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

(defn zoom-in [db [_ _]]
  (transform [:config :pixel-to-minute-ratio :current] #(* 1.25 %) db))

(defn zoom-out [db [_ _]]
  (transform [:config :pixel-to-minute-ratio :current] #(* 0.75 %) db))

(defn zoom-default [db [_ _]]
  (setval [:config :pixel-to-minute-ratio :current]
          (select-one [:config :pixel-to-minute-ratio :default] db) db))

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

(defn set-day-fab-open [db [_ state]]
  (assoc-in db [:day-fab :open] state))

(defn set-day-fab-visible [db [_ state]]
  (assoc-in db [:day-fab :visible] state))

(defn set-menu-open [db [_ state]]
  (assoc-in db [:menu :open] state))

(defn set-report-contribution-bucket [db [_ bucket-id]]
  (assoc-in db [:selection :report :bucket-contribution] bucket-id))

(defn set-report-data [db [_ _]]
  (let [days-ago 200
        periods-last-n-days (filter
                             (fn [p] (-> (:stop p)
                                         (.valueOf)
                                         (> (.valueOf
                                             (helpers/back-n-days
                                              (helpers/reset-relative-ms 0 (js/Date.)) days-ago)))))
                             (subs/get-periods db :no-op))
        scores
        (->> (range)
             (take days-ago)
             ;; map over the days
             (map (fn [days-ago]
                    (let [day (helpers/reset-relative-ms
                               0
                               (helpers/back-n-days (js/Date.) days-ago))

                          score
                          (->> (range)
                               (take helpers/day-min)
                               ;; map over every minute in the day
                               (map (fn [min-of-day]
                                      (let [exact-date          (helpers/reset-relative-ms
                                                                 (helpers/minutes->ms min-of-day)
                                                                 day)
                                            ed-ms               (.valueOf exact-date)
                                            overlapping-periods (->> periods-last-n-days
                                                                     (filter (fn [period]
                                                                               (and
                                                                                (-> (.valueOf
                                                                                     (:stop period))
                                                                                    (>= ed-ms))
                                                                                (-> (.valueOf
                                                                                     (:start period))
                                                                                    (<= ed-ms))))))
                                            planned             (->> overlapping-periods
                                                                     (filter :planned))
                                            p-count             (count planned)
                                            p-ids               (->> planned
                                                                     (map :bucket-id)
                                                                     set)
                                            actual              (->> overlapping-periods
                                                                     (remove :planned))
                                            a-count             (count actual)
                                            a-ids               (->> actual
                                                                     (map :bucket-id)
                                                                     set)
                                            intersection        (clojure.set/intersection
                                                                 p-ids a-ids)]

                                        ;; figure out the score for the minute
                                        (cond
                                          ;; didn't plan didn't do
                                          (and
                                           (= 0 p-count)
                                           (= 0 a-count))            0
                                          ;; planned xor did
                                          (or
                                           (= 0 p-count)
                                           (= 0 a-count))            1
                                          ;; planned and did but do not match at all
                                          (= 0 (count intersection)) 2
                                          ;; planned and did and match perfectly
                                          (= p-ids a-ids)            4
                                          ;; planned and did but only kinda match
                                          (> 0 (count intersection)) 3

                                          :else 0))))
                               ;; add all the minute scores together
                               (reduce +))]
                      ;; put it all together
                      {:score (/ score 1000) :day day}))))]
    (println (map :score scores))
    (assoc-in db [:reports :score-data] scores)))

(defn set-version [db [_ version]]
  (assoc-in db [:version] version))

(reg-event-fx :select-next-or-prev-period [validate-spec amplitude-logging persist-secure-store] select-next-or-prev-period)
(reg-event-fx :select-next-or-prev-template-in-form [validate-spec amplitude-logging persist-secure-store] select-next-or-prev-template-in-form)
(reg-event-db :initialize-db [validate-spec amplitude-logging] initialize-db)
(reg-event-fx :navigate-to [validate-spec amplitude-logging persist-secure-store] navigate-to)
(reg-event-fx :navigate-to-no-history [validate-spec amplitude-logging persist-secure-store] navigate-to)
(reg-event-db :load-bucket-form [validate-spec amplitude-logging persist-secure-store] load-bucket-form)
(reg-event-db :update-bucket-form [validate-spec amplitude-logging persist-secure-store] update-bucket-form)
(reg-event-fx :save-bucket-form [alert-message validate-spec amplitude-logging persist-secure-store] save-bucket-form)
(reg-event-db :load-period-form [validate-spec amplitude-logging persist-secure-store] load-period-form)
(reg-event-db :update-period-form [validate-spec amplitude-logging persist-secure-store] update-period-form)
(reg-event-fx :save-period-form [alert-message validate-spec amplitude-logging persist-secure-store] save-period-form)
(reg-event-db :load-template-form [validate-spec amplitude-logging persist-secure-store] load-template-form)
(reg-event-db :load-template-form-from-pattern-planning [validate-spec amplitude-logging persist-secure-store] load-template-form-from-pattern-planning)
(reg-event-db :update-template-form [validate-spec amplitude-logging persist-secure-store generate-handler-test-db ] update-template-form)
(reg-event-fx :save-template-form [alert-message validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] save-template-form)
(reg-event-fx :save-template-form-from-pattern-planning [alert-message validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] save-template-form-from-pattern-planning)
(reg-event-db :load-filter-form [validate-spec amplitude-logging persist-secure-store generate-handler-test-db ] load-filter-form)
(reg-event-db :update-filter-form [validate-spec amplitude-logging persist-secure-store generate-handler-test-db ] update-filter-form)
(reg-event-fx :save-filter-form [alert-message validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] save-filter-form)
(reg-event-db :update-active-filter [validate-spec amplitude-logging persist-secure-store generate-handler-test-db ] update-active-filter)
(reg-event-fx :add-new-bucket [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] add-new-bucket)
(reg-event-fx :add-new-period [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] add-new-period)
;; (reg-event-fx :add-template-period [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] add-template-period)
(reg-event-fx :add-new-template [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] add-new-template)
(reg-event-fx :add-new-template-to-planning-form [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] add-new-template-to-planning-form)
(reg-event-fx :add-new-filter [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] add-new-filter)
(reg-event-fx :delete-bucket [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] delete-bucket)
(reg-event-fx :delete-period [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] delete-period)
(reg-event-fx :delete-template [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] delete-template)
(reg-event-fx :delete-template-from-pattern-planning [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] delete-template-from-pattern-planning)
(reg-event-fx :delete-pattern [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] delete-pattern)
(reg-event-fx :delete-filter [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] delete-filter)
(reg-event-fx :update-period [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] update-period)
(reg-event-db :add-period [validate-spec amplitude-logging persist-secure-store generate-handler-test-db ] add-period)
(reg-event-db :update-day-time-navigator [validate-spec amplitude-logging persist-secure-store generate-handler-test-db ] update-day-time-navigator)
(reg-event-fx :tick [validate-spec persist-secure-store generate-handler-test-fx ] tick)
(reg-event-fx :play-from-period [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] play-from-period)
(reg-event-db :stop-playing-period [validate-spec amplitude-logging persist-secure-store generate-handler-test-db ] stop-playing-period)
(reg-event-fx :play-from-bucket [validate-spec amplitude-logging persist-secure-store generate-handler-test-fx ] play-from-bucket)
(reg-event-db :play-from-template [validate-spec amplitude-logging persist-secure-store generate-handler-test-db ] play-from-template)
(reg-event-db :load-db [validate-spec amplitude-logging] load-db)
(reg-event-fx :share-app-db [validate-spec amplitude-logging] share-app-db)
(reg-event-db :add-auto-filter [validate-spec amplitude-logging persist-secure-store  ] add-auto-filter)
(reg-event-db :load-pattern-form [validate-spec amplitude-logging persist-secure-store  ] load-pattern-form)
(reg-event-fx :update-pattern-form [validate-spec amplitude-logging persist-secure-store  ] update-pattern-form)
(reg-event-fx :save-pattern-form [validate-spec amplitude-logging persist-secure-store  ] save-pattern-form)
(reg-event-fx :add-new-pattern [validate-spec amplitude-logging persist-secure-store  ] add-new-pattern)
(reg-event-db :apply-pattern-to-displayed-day [validate-spec amplitude-logging persist-secure-store  ] apply-pattern-to-displayed-day)
(reg-event-db :import-app-db [validate-spec amplitude-logging persist-secure-store  ] import-app-db)
(reg-event-fx :navigate-back [validate-spec amplitude-logging persist-secure-store  ] navigate-back)
(reg-event-fx :update-template-on-pattern-planning-form [validate-spec amplitude-logging persist-secure-store  ] update-template-on-pattern-planning-form)
(reg-event-db :make-pattern-from-day [validate-spec amplitude-logging persist-secure-store  ] make-pattern-from-day)
(reg-event-db :set-current-pixel-to-minute-ratio [validate-spec amplitude-logging persist-secure-store  ] set-current-pixel-to-minute-ratio)
(reg-event-db :set-default-pixel-to-minute-ratio [validate-spec amplitude-logging persist-secure-store  ] set-default-pixel-to-minute-ratio)
(reg-event-fx :select-period-movement [validate-spec amplitude-logging persist-secure-store  ] select-period-movement)
(reg-event-fx :select-period-edit [validate-spec amplitude-logging persist-secure-store  ] select-period-edit)
(reg-event-fx :select-template-movement [validate-spec amplitude-logging persist-secure-store  ] select-template-movement)
(reg-event-fx :select-template-edit [validate-spec amplitude-logging persist-secure-store  ] select-template-edit)
(reg-event-fx :select-element-movement [validate-spec amplitude-logging persist-secure-store  ] select-element-movement)
(reg-event-fx :select-element-edit [validate-spec amplitude-logging persist-secure-store  ] select-element-edit)
(reg-event-db :set-day-fab-open [validate-spec amplitude-logging persist-secure-store] set-day-fab-open)
(reg-event-db :set-day-fab-visible [validate-spec amplitude-logging persist-secure-store] set-day-fab-visible)
(reg-event-db :set-menu-open [validate-spec amplitude-logging persist-secure-store] set-menu-open)
(reg-event-db :zoom-in [validate-spec amplitude-logging persist-secure-store] zoom-in)
(reg-event-db :zoom-out [validate-spec amplitude-logging persist-secure-store] zoom-out)
(reg-event-db :zoom-default [validate-spec amplitude-logging persist-secure-store] zoom-default)
(reg-event-fx :add-period-with-selection [validate-spec amplitude-logging persist-secure-store] add-period-with-selection)
(reg-event-db :set-report-data [validate-spec amplitude-logging persist-secure-store] set-report-data)
(reg-event-db :set-version [validate-spec amplitude-logging persist-secure-store] set-version)
