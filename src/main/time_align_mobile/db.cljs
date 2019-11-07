(ns time-align-mobile.db
  (:require [clojure.spec.alpha :as s]
            [spec-tools.data-spec :as ds]
            [spec-tools.core :as st]
            [clojure.string :as string]
            [clojure.test.check.generators :as gen]
            ;; [time-align-mobile.navigation :as nav]
            [time-align-mobile.helpers :as helpers]
            [time-align-mobile.screens.filter-form :refer [filterable-types]] ;; TODO find a better spot to put this, think about nav too
            [time-align-mobile.js-imports :refer [make-date
                                                  get-default-timezone
                                                  start-of-today
                                                  format-date
                                                  format-time
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
            :label       (str (if type "planned " "actual ")
                              (if (some? (:start stamps))
                                (format-time start)
                                " queue item"))
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

(s/def ::periods (s/with-gen
                   (s/and map?
                          (s/every-kv uuid? period-spec))
                   #(gen/fmap
                     (fn [n]
                       (into {} (->> n
                                     range
                                     (map (fn [n] (gen/generate ::moment)))
                                     (map generate-period))))
                     10)))

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
                         :planned        boolean?
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
(defn make-hex-digit
  "non gen verison"
  []
  (str (rand-nth (seq "0123456789abcdef"))))

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

(defn generate-bucket [n]
  {:id          (random-uuid)
   :label       ""
   :created     (js/Date.)
   :last-edited (js/Date.)
   :data        {:nth-generated n}
   :color       (gen/generate ::color)
   :periods     (gen/generate ::periods)})

(defn make-periods [num]
  (apply merge (->> num
                    range
                    (map (fn [_] (let [id (random-uuid)]
                                   {id (-> time-range
                                           rand-nth
                                           js/Date.
                                           generate-period
                                           (merge {:id id}))}))))))
(defn make-bucket
  "non gen/generate version"
  [num-periods]
  {:id          (random-uuid)
   :label       "Generated bucket"
   :created     (js/Date.)
   :last-edited (js/Date.)
   :data        {}
   :color       (str "#" (->> 6 range (map make-hex-digit) (string/join "")))
   :periods     (make-periods num-periods)})

(defn make-buckets [number-buckets number-periods-per-bucket]
  (apply merge (->> number-buckets
                    range
                    (map (fn [_] (let [id (random-uuid)]
                                   {id (merge
                                        (make-bucket number-periods-per-bucket)
                                        {:id id})}))))))

(def bucket-data-spec {:id          uuid?
                       :label       string?
                       :created     ::moment
                       :last-edited ::moment
                       :data        map?
                       :color       ::color
                       :periods     (ds/maybe ::periods)})

(def bucket-spec
  (st/create-spec {:spec
                   (ds/spec {:spec bucket-data-spec
                             :name ::bucket})}))

(s/def ::buckets (s/with-gen
                   (s/and map?
                          (s/every-kv uuid? bucket-spec))
                   #(gen/fmap
                     (fn [n]
                       (into {} (->> n
                                     range
                                     (map generate-bucket))))
                     10)))

;; (def screen-id-set (set (->> nav/screens-map
;;                              (map (fn [{:keys [id]}] id)))))

(s/def ::screen keyword?) ;; importing nav/screens-map loads every screen ns which starts a tick side effect

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
                   :selection     {:period   {:movement {:period-id (ds/maybe uuid?)
                                                         :bucket-id (ds/maybe uuid?)}
                                              :edit     {:period-id (ds/maybe uuid?)
                                                         :bucket-id (ds/maybe uuid?)}}
                                   :template {:movement {:template-id (ds/maybe uuid?)
                                                         :bucket-id   (ds/maybe uuid?)}
                                              :edit     {:template-id (ds/maybe uuid?)
                                                         :bucket-id   (ds/maybe uuid?)}}}
                   :filters       [filter-data-spec]
                   :navigation    {:current-screen ::screen
                                   :params         (ds/maybe map?)}

                   :buckets           ::buckets
                   :patterns          [pattern-spec]
                   :time-navigators   {:day      ::moment
                                       :calendar ::moment
                                       :report   ::moment}
                   :config            {:auto-log-time-align   boolean?
                                       :pixel-to-minute-ratio {:default number?
                                                               :current number?}}
                   :period-in-play-id (ds/maybe uuid?)
                   :now               inst?
                   :day-fab           {:open    boolean?
                                       :visible boolean?}
                   :menu              {:open boolean?}}
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
   :patterns          []
   :active-filter     nil
   :filters           []
   :navigation        {:current-screen :day
                       :params         nil}
   :buckets           []
   :time-navigators   {:day      (js/Date.)
                       :calendar (js/Date.)
                       :report   (js/Date.)}
   :config            {:auto-log-time-align   true
                       :pixel-to-minute-ratio {:default 0.5
                                               :current 0.5}}
   :period-in-play-id nil
   :selection         {:period   {:movement {:bucket-id nil
                                             :period-id nil}
                                  :edit     {:bucket-id nil
                                             :period-id nil}}
                       :template {:movement {:bucket-id   nil
                                             :template-id nil}
                                  :edit     {:bucket-id   nil
                                             :template-id nil}}}
   :now               now
   :day-fab           {:open    false
                       :visible true}
   :menu              {:open false}})

;; TODO use https://facebook.github.io/react-native/docs/appstate.html to log all time in app
;; old initial state of app-db
