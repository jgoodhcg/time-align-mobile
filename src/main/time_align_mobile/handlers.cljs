(ns time-align-mobile.handlers
  (:require
    [time-align-mobile.js-imports :refer [write-file-to-dd! alert share format-date]]
    [re-frame.core :refer [reg-event-db ->interceptor reg-event-fx reg-fx dispatch]]
    ;; [zprint.core :refer [zprint]]
    [cljs.reader :refer [read-string]]
    [clojure.spec.alpha :as s]
    [time-align-mobile.node-safe-handler-functions :as h]
    [time-align-mobile.db :as db :refer [app-db app-db-spec period-data-spec]]
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
  (if true ;;goog.DEBUG ;; TODO reinstate this after pre-alpha
    (->interceptor
        :id :validate-spec
        :after (fn [context]
                 context
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

;; -- fx ---------------------------------------------------------------
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

(reg-fx
 :share
 (fn [app-db]
   (share (str (format-date (js/Date.)) "-app-db.edn") (str app-db))))

;; -- Handlers --------------------------------------------------------------
(reg-event-fx :select-next-or-prev-period [validate-spec persist-secure-store] h/select-next-or-prev-period)
(reg-event-fx :select-next-or-prev-template-in-form [validate-spec persist-secure-store] h/select-next-or-prev-template-in-form)
(reg-event-db :initialize-db [validate-spec] h/initialize-db)
(reg-event-fx :navigate-to [validate-spec persist-secure-store] h/navigate-to)
(reg-event-db :load-bucket-form [validate-spec persist-secure-store] h/load-bucket-form)
(reg-event-db :update-bucket-form [validate-spec persist-secure-store] h/update-bucket-form)
(reg-event-fx :save-bucket-form [alert-message validate-spec persist-secure-store] h/save-bucket-form)
(reg-event-db :load-period-form [validate-spec persist-secure-store] h/load-period-form)
(reg-event-db :update-period-form [validate-spec persist-secure-store] h/update-period-form)
(reg-event-fx :save-period-form [alert-message validate-spec persist-secure-store] h/save-period-form)
(reg-event-db :load-template-form [validate-spec persist-secure-store] h/load-template-form)
(reg-event-db :load-template-form-from-pattern-planning [validate-spec persist-secure-store] h/load-template-form-from-pattern-planning)
(reg-event-db :update-template-form [validate-spec persist-secure-store] h/update-template-form)
(reg-event-fx :save-template-form [alert-message validate-spec persist-secure-store] h/save-template-form)
(reg-event-fx :save-template-form-from-pattern-planning [alert-message validate-spec persist-secure-store] h/save-template-form-from-pattern-planning)
(reg-event-db :load-filter-form [validate-spec persist-secure-store] h/load-filter-form)
(reg-event-db :update-filter-form [validate-spec persist-secure-store] h/update-filter-form)
(reg-event-fx :save-filter-form [alert-message validate-spec persist-secure-store] h/save-filter-form)
(reg-event-db :update-active-filter [validate-spec persist-secure-store] h/update-active-filter)
(reg-event-fx :add-new-bucket [validate-spec persist-secure-store] h/add-new-bucket)
(reg-event-fx :add-new-period [validate-spec persist-secure-store] h/add-new-period)
;; (reg-event-fx :add-template-period [validate-spec persist-secure-store] add-template-period)
(reg-event-fx :add-new-template [validate-spec persist-secure-store] h/add-new-template)
(reg-event-fx :add-new-template-to-planning-form [validate-spec persist-secure-store] h/add-new-template-to-planning-form)
(reg-event-fx :add-new-filter [validate-spec persist-secure-store] h/add-new-filter)
(reg-event-fx :delete-bucket [validate-spec persist-secure-store] h/delete-bucket)
(reg-event-fx :delete-period [validate-spec persist-secure-store] h/delete-period)
(reg-event-fx :delete-template [validate-spec persist-secure-store] h/delete-template)
(reg-event-fx :delete-template-from-pattern-planning [validate-spec persist-secure-store] h/delete-template-from-pattern-planning)
(reg-event-fx :delete-pattern [validate-spec persist-secure-store] h/delete-pattern)
(reg-event-fx :delete-filter [validate-spec persist-secure-store] h/delete-filter)
(reg-event-fx :update-period [validate-spec persist-secure-store] h/update-period)
(reg-event-db :add-period [validate-spec persist-secure-store] h/add-period)
(reg-event-db :update-day-time-navigator [validate-spec persist-secure-store] h/update-day-time-navigator)
(reg-event-fx :tick [validate-spec persist-secure-store] h/tick)
(reg-event-fx :play-from-period [validate-spec persist-secure-store] h/play-from-period)
(reg-event-db :stop-playing-period [validate-spec persist-secure-store] h/stop-playing-period)
(reg-event-fx :play-from-bucket [validate-spec persist-secure-store] h/play-from-bucket)
(reg-event-db :play-from-template [validate-spec persist-secure-store] h/play-from-template)
(reg-event-db :load-db [validate-spec] h/load-db)
(reg-event-fx :share-app-db [validate-spec] h/share-app-db)
(reg-event-db :add-auto-filter [validate-spec persist-secure-store] h/add-auto-filter)
(reg-event-db :load-pattern-form [validate-spec persist-secure-store] h/load-pattern-form)
(reg-event-fx :update-pattern-form [validate-spec persist-secure-store] h/update-pattern-form)
(reg-event-fx :save-pattern-form [validate-spec persist-secure-store] h/save-pattern-form)
(reg-event-fx :add-new-pattern [validate-spec persist-secure-store] h/add-new-pattern)
(reg-event-db :apply-pattern-to-displayed-day [validate-spec persist-secure-store] h/apply-pattern-to-displayed-day)
(reg-event-db :import-app-db [validate-spec persist-secure-store] h/import-app-db)
(reg-event-fx :navigate-back [validate-spec persist-secure-store] h/navigate-back)
(reg-event-fx :update-template-on-pattern-planning-form [validate-spec persist-secure-store] h/update-template-on-pattern-planning-form)
(reg-event-db :make-pattern-from-day [validate-spec persist-secure-store] h/make-pattern-from-day)
(reg-event-db :set-current-pixel-to-minute-ratio [validate-spec persist-secure-store] h/set-current-pixel-to-minute-ratio)
(reg-event-db :set-default-pixel-to-minute-ratio [validate-spec persist-secure-store] h/set-default-pixel-to-minute-ratio)
(reg-event-fx :select-period-movement [validate-spec persist-secure-store] h/select-period-movement)
(reg-event-fx :select-period-edit [validate-spec persist-secure-store] h/select-period-edit)
(reg-event-fx :select-template-movement [validate-spec persist-secure-store] h/select-template-movement)
(reg-event-fx :select-template-edit [validate-spec persist-secure-store] h/select-template-edit)
(reg-event-fx :select-element-movement [validate-spec persist-secure-store] h/select-element-movement)
(reg-event-fx :select-element-edit [validate-spec persist-secure-store] h/select-element-edit)
