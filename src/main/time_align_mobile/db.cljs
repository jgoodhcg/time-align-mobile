(ns time-align-mobile.db
  (:require [clojure.spec.alpha :as s]
            [spec-tools.data-spec :as ds]
            [spec-tools.core :as st]
            [clojure.string :as string]
            [clojure.test.check.generators :as gen]
            [time-align-mobile.navigation :as nav]
            [time-align-mobile.screens.filter-form :refer [filterable-types]] ;; TODO find a better spot to put this, think about nav too
            [time-align-mobile.js-imports :refer [make-date
                                                  get-default-timezone
                                                  start-of-today
                                                  end-of-today]]))

(def hour-ms
  (->> 1
       (* 60)
       (* 60)
       (* 1000)))
(def time-range
  (range (.valueOf (start-of-today (make-date) (get-default-timezone)))
         (.valueOf (end-of-today (make-date) (get-default-timezone)))
         hour-ms))
(def time-set
  (set (->> time-range
            (map #(new js/Date %)))))
(s/def ::moment (s/with-gen inst? #(s/gen time-set)))

;; period
(defn start-before-stop [period]
  (if

      ;; Check that it has time stamps
      (and
       (contains? period :start)
       (contains? period :stop)
       (some? (:start period))
       (some? (:stop period)))

    ;; If it has time stamps they need to be valid
    (> (.valueOf (:stop period))
       (.valueOf (:start period)))

    ;; Passes if it has no time stamps
    true))
(defn generate-period [moment]
  (let [desc-chance   (> 0.5 (rand))
        queue-chance  (> 0.5 (rand))
        actual-chance (> 0.5 (rand))
        start         (.valueOf moment)
        stop          (->> start
                           (+ (rand-int (* 2 hour-ms))))
        stamps        (if queue-chance
                        {:start nil
                         :stop  nil}
                        {:start (new js/Date start)
                         :stop  (new js/Date stop)})
        type          (if (nil? (:start stamps))
                        true
                        actual-chance)]

    (merge stamps
           {:id          (random-uuid)
            :created     moment
            :last-edited moment
            :label       ""
            :planned     type
            :data        {}})))

(def period-data-spec {:id          uuid?
                       :created     ::moment
                       :last-edited ::moment
                       :label       string?
                       :planned     boolean?
                       :start       (ds/maybe ::moment)
                       :stop        (ds/maybe ::moment)
                       :data        map?})

(def period-spec
  (st/create-spec {:spec (s/and
                          (ds/spec {:spec period-data-spec
                                    :name ::period})
                          start-before-stop)
                   :gen  #(gen/fmap generate-period
                                    (s/gen ::moment))}))

;; template
(defn start-before-stop-template [{:keys [start stop]}]
  (if
      (and ;; Check that it has time stamps
       (some? start)
       (some? stop))

    ;; If it has time stamps they need to be valid
    (> stop start)
    ;; Passes if it has no time stamps
    true))

(def template-data-spec {:id             uuid?
                         :bucket-id      uuid?
                         :label          string?
                         :created        ::moment
                         :last-edited    ::moment
                         :data           map?
                         (ds/opt :start) integer? ;; relative ms of day
                         (ds/opt :stop)  integer?})
(def template-spec
  (st/create-spec {:spec (s/and
                          (ds/spec {:spec template-data-spec
                                    :name ::template})
                          start-before-stop-template)}))

;; bucket
(s/def ::hex-digit (s/with-gen
                     (s/and string? #(contains? (set "0123456789abcdef") %))
                     #(s/gen (set "0123456789abcdef"))))
(s/def ::hex-str (s/with-gen
                   (s/and string? (fn [s] (every? #(s/valid? ::hex-digit %) (seq s))))
                   #(gen/fmap string/join (gen/vector (s/gen ::hex-digit) 6))))
(s/def ::color (s/with-gen
                 (s/and #(= "#" (first %))
                        #(s/valid? ::hex-str (string/join (rest %)))
                        #(= 7 (count %)))
                 #(gen/fmap
                   (fn [hex-str] (string/join (cons "#" hex-str)))
                   (s/gen ::hex-str))))
(def bucket-data-spec {:id          uuid?
                       :label       string?
                       :created     ::moment
                       :last-edited ::moment
                       :data        map?
                       :color       ::color
                       :periods     (ds/maybe [period-spec])})
(def bucket-spec
  (st/create-spec {:spec
                   (ds/spec {:spec bucket-data-spec
                             :name ::bucket})}))

(def screen-id-set (set (->> nav/screens-map
                             (map (fn [{:keys [id]}] id)))))
(s/def ::screen screen-id-set)


;; filter
(def filter-data-spec
  {:id          uuid?
   :label       string?
   :created     ::moment
   :last-edited ::moment
   :compatible  [(s/spec filterable-types)]
   :sort        (ds/maybe {:path      [keyword?]
                           :ascending boolean?})
   :predicates  [{:path   [keyword?]
                  :value  string? ;; TODO the form uses read and that coerces all values to strings
                  :negate boolean?}]})

;; pattern
(def pattern-data-spec
  {:id          uuid?
   :label       string?
   :created     ::moment
   :last-edited ::moment
   :data        map?
   :templates   (ds/maybe [template-spec])})
(def pattern-spec
  (st/create-spec {:spec
                   (ds/spec {:spec pattern-data-spec
                             :name ::pattern})}))
;; app-db
(def app-db-spec
  (ds/spec {:spec {:forms         {:bucket-form
                                   (ds/maybe (merge bucket-data-spec
                                                    {:data string?}))
                                   :period-form
                                   (ds/maybe (merge period-data-spec
                                                    {:data         string?
                                                     :bucket-id    uuid?
                                                     :bucket-label string?
                                                     :bucket-color ::color}))
                                   :pattern-form
                                   (ds/maybe (merge pattern-data-spec
                                                    {:data string?}))
                                   :template-form
                                   (ds/maybe (merge template-data-spec
                                                    {:data         string?
                                                     :bucket-id    uuid?
                                                     :bucket-label string?
                                                     :bucket-color ::color}))
                                   :filter-form
                                   (ds/maybe (merge filter-data-spec
                                                    {:predicates string?}
                                                    {:sort string?}))}
                   :active-filter (ds/maybe uuid?)
                   :selection     {:period   {:movement (ds/maybe uuid?)
                                              :edit     (ds/maybe uuid?)}
                                   :template {:movement (ds/maybe uuid?)
                                              :edit     (ds/maybe uuid?)}}
                   :filters       [filter-data-spec]
                   :navigation    {:current-screen ::screen
                                   :params         (ds/maybe map?)}

                   :buckets           [bucket-spec]
                   :patterns          [pattern-spec]
                   :time-navigators   {:day      ::moment
                                       :calendar ::moment
                                       :report   ::moment}
                   :config            {:auto-log-time-align   boolean?
                                       :pixel-to-minute-ratio {:default number?
                                                               :current number?}}
                   :period-in-play-id (ds/maybe uuid?)
                   :now               inst?}
            :name ::app-db}))
(def now (js/Date.))
(def default-bucket-id (uuid "a7396f81-38d4-4d4f-ab19-a7cef18c4ea2"))
(def default-period-id (uuid "a8404f81-38d4-4d4f-ab19-a7cef18c4531"))
(def app-db
  {:forms             {:bucket-form   nil
                       :period-form   nil
                       :pattern-form  nil
                       :template-form nil
                       :filter-form   nil}
   :patterns          [{:id          (uuid "ffa49881-38d4-4d4f-ab19-a7cef18c4465")
                        :label       "an example pattern"
                        :created     now
                        :last-edited now
                        :data        {}
                        :templates   [{:id                                      (uuid "bb9b9881-38d4-4d4f-ab19-a7cef18c6647")
                                       :bucket-id                               default-bucket-id
                                       :label                                   "do something in time align"
                                       :created                                 now
                                       :last-edited                             now
                                       :data                                    {}
                                       :start                                   (-> 12.5 ;; hours from start of day
                                                                                    (* 60) ;; minutes
                                                                                    (* 60) ;; seconds
                                                                                    (* 1000)) ;; millis
                                       :stop                                    (-> 14
                                                                                    (* 60)
                                                                                    (* 60)
                                                                                    (* 1000))}]}]
   :active-filter     nil
   :filters           [{:id          (uuid "bbc34081-38d4-4d4f-ab19-a7cef18c1212")
                        :label       "sort by bucket label"
                        :created     now
                        :compatible  [:period :template]
                        :last-edited now
                        :sort        {:path      [:bucket-label]
                                      :ascending true}
                        :predicates  []}
                       {:id          (uuid "aad94081-38d4-4d4f-ab19-a7cef18c1299")
                        :label       "sort by label"
                        :created     now
                        :compatible  [:bucket :filter :period :template]
                        :last-edited now
                        :sort        {:path      [:label]
                                      :ascending true}
                        :predicates  []}
                       {:id          (uuid "cccc4081-38d4-4d4f-ab19-a7cef18c4444")
                        :label       "time align bucket filter"
                        :created     now
                        :compatible  [:period :template]
                        :last-edited now
                        :sort        nil
                        :predicates  [{:path   [:bucket-id]
                                       :value  (str default-bucket-id)
                                       :negate false}]}]
   :navigation        {:current-screen :day
                       :params         nil}
   :buckets           [{:id          default-bucket-id
                        :label       "time align"
                        :created     now
                        :last-edited now
                        :data        {}
                        :color       "#11aa11"
                        :periods     [{:id          default-period-id
                                       :created     now
                                       :last-edited now
                                       :label       "start using"
                                       :planned     false
                                       :start       now
                                       :stop        (-> now (.valueOf) (+ (* 1 60 1000)) (js/Date.))
                                       :data        {}}]}]
   :time-navigators   {:day      (js/Date.)
                       :calendar (js/Date.)
                       :report   (js/Date.)}
   :config            {:auto-log-time-align   true
                       :pixel-to-minute-ratio {:default 0.5
                                               :current 0.5}}
   :period-in-play-id default-period-id
   :selection         {:period   {:movement nil
                                  :edit     nil}
                       :template {:movement nil
                                  :edit     nil}}
   :now               now})

;; TODO use https://facebook.github.io/react-native/docs/appstate.html to log all time in app
;; old initial state of app-db
