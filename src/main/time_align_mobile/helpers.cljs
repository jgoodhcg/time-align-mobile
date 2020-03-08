(ns time-align-mobile.helpers
  (:require
   ;; [zprint.core :refer [zprint]]
   [time-align-mobile.js-imports :refer [gesture-states]]
   [goog.object :as obj]
   [re-frame.core :refer [dispatch]]
   [com.rpl.specter :as sp :refer-macros [select select-one setval transform]])
  (:import [goog.async Throttle Debouncer]))


(def day-hour 24)

(def day-ms
  ;; 24 hours in millis
  (* day-hour 60 60 1000))

(def day-min
  ;; 24 hours in minutes
  (* day-hour 60))

(defn same-day? [date-a date-b]
  (if (and (inst? date-a)
           (inst? date-b))
    (and (= (.getUTCFullYear date-a)
            (.getUTCFullYear date-b))
         (= (.getUTCMonth date-a)
            (.getUTCMonth date-b))
         (= (.getUTCDate date-a)
            (.getUTCDate date-b)))
    false))

(defn same-year? [date-a date-b]
  (if (and (inst? date-a)
           (inst? date-b))
    (and (= (.getUTCFullYear date-a)
            (.getUTCFullYear date-b)))
    false))

(defn print-data [data]
  (str data)
  ;; (with-out-str (zprint data 40))  ;; TODO doesn't like zprint :(
)

(defn hours->ms [hours]
  (-> hours
      (* 60)
      (* 60)
      (* 1000)))

(defn minutes->ms [minutes]
  (-> minutes
      (* 60)
      (* 1000)))

(defn ms->minutes [millis]
  (-> millis
      (/ 1000)
      (/ 60)))

(defn sec->ms [seconds]
  (* seconds 1000))

(defn get-ms [date]
  (if (inst? date)
    (let [hours   (.getHours date)
          minutes (.getMinutes date)
          seconds (.getSeconds date)
          millis  (.getMilliseconds date)]
      (+ (hours->ms hours)
         (minutes->ms minutes)
         (sec->ms seconds)
         millis))
    ;; TODO maybe throw an error if not a number?
    date))

(defn ms->hhmm [ms]
  (let [hours   (-> ms
                    (quot (hours->ms 1))
                    (#(if (< % 10) ;; add leading 0
                        (str "0" %)
                        (str %))))
        minutes (-> ms
                    (rem (hours->ms 1)) ;; minutes left over after hours
                    (/ 1000) ;; converted from ms to s
                    (/ 60)   ;; converted from s to m
                    (.toFixed 0)
                    (#(if (< % 10) ;; add leading 0
                        (str "0" %)
                        (str %))))]
    (str hours "-" minutes)))

(defn ms->h [ms] ;; TODO refactor this to "rounded"
  (-> ms (quot (hours->ms 1))))

(defn ms->h-float [ms]
  (-> ms
      (/ 1000)
      (/ 60)
      (/ 50)
      (.toFixed 2)
      (js/parseFloat)))

(defn rel-ms->y-pos [ms total-height]
  (-> ms
      (/ day-ms)
      (* total-height)))

(defn date->y-pos [date-time total-height]
  (-> date-time
      (get-ms)
      (rel-ms->y-pos total-height)))

(defn y-pos->ms [y-pos total-height]
  (-> y-pos
      (/ total-height)
      (* day-ms)))

(defn duration->height [duration-ms total-height]
  (-> duration-ms
      (/ day-ms)
      (* total-height)))

(defn bound-start [start day]
  (if (same-day? day start)
    start
    (js/Date. (.getFullYear day)
              (.getMonth day)
              (.getDate day)
              0
              0)))

(defn reset-relative-ms [ms date]
  (let [year           (.getFullYear date)
        month          (.getMonth date)
        day            (.getDate date)
        zero-day    (js/Date. year month day 0 0 0)
        zero-day-ms (.valueOf zero-day)]
    (js/Date. (+ zero-day-ms ms))))

(defn abstract-element-timestamp
  "Returns relative ms to the displayed day"
  [timestamp displayed-day]
  (if (number? timestamp)
    timestamp ;; already relative-ms (template)
    (if (inst? timestamp)
      ;; below will work no matter how far behind or ahead
      ;; the timestamp is to the displayed day
      (- (.valueOf timestamp) (->> displayed-day
                                   (reset-relative-ms 0)
                                   (.valueOf)))
      (throw (js/Error. "timestamp wasn't a number or an inst")))))

(defn bound-stop [stop day]
  (if (same-day? day stop)
    stop
    ;; use the end of the day otherwise
    (js/Date. (.getFullYear day)
              (.getMonth day)
              (.getDate day)
              23
              59)))

(defn back-n-days [date n]
  (let [days (.getDate date)
        month (.getMonth date)
        year (.getFullYear date)]
    (js/Date. year month (- days n))))

(defn forward-n-days [date n]
  (let [days (.getDate date)
        month (.getMonth date)
        year (.getFullYear date)]
    (js/Date. year month (+ days n))))

(defn overlapping-timestamps? [{start-a :start stop-a :stop}
                               {start-b :start stop-b :stop}]
  (and (<= (.valueOf start-b) (.valueOf stop-a))
       (<= (.valueOf start-a) (.valueOf stop-b))))

(defn overlapping-relative-timestamps? [{start-a :start stop-a :stop}
                                        {start-b :start stop-b :stop}]
  (and (<= start-b stop-a)
       (<= start-a stop-b)))

(defn overlapping-relative-time-or-date-obj? [a b]
  (if (int? (:start a))
    ;; If it is an integer then it is the relative time for a template
    (overlapping-relative-timestamps? a b)
    ;; Otherwise it is a date object
    (overlapping-timestamps? a b)))

(defn period-overlaps-collision-group? [period c-group]
  (some? (->> c-group
              (some #(overlapping-relative-time-or-date-obj? period %)))))

(defn insert-into-collision-group [collision-groups period]
  (let [collision-groups-with-trailing-empty
        (if (empty? (last collision-groups))
          collision-groups
          (conj collision-groups []))]

    (setval

     (sp/cond-path
      [sp/ALL (partial period-overlaps-collision-group? period)]
      [sp/ALL (partial period-overlaps-collision-group? period) sp/AFTER-ELEM]

      [sp/ALL empty?]
      [sp/ALL empty? sp/AFTER-ELEM])

     period
     collision-groups-with-trailing-empty)))

(defn get-collision-groups [periods]
  (->> periods
       (reduce insert-into-collision-group [[]])
       (remove empty?)))

(defn period-time-on-day [{:keys [start stop]} date]
  (let [beginning (reset-relative-ms 0 date)
        end       (reset-relative-ms day-ms date)]

    (if ;; test if the interval overlaps
        (or
         ;; start or stop are on the day
         (or (same-day? start date)
             (same-day? stop date))
         ;; start and stop stretch past the day
         (and (-> (.valueOf start)
                  (<= (.valueOf beginning)))
              (-> (.valueOf stop)
                  (>= (.valueOf end)))))

      ;; it does so figure out the value of the portion that overlaps
      (let [adjusted-start (if (same-day? start date)
                             start
                             beginning)
            adjusted-stop  (if (same-day? stop date)
                             stop
                             end)]
        ;; use the adjusted start/stop to figure out the overlapping amount in ms
        (-> (.valueOf adjusted-stop)
            (- (.valueOf adjusted-start))))

      ;; it doesn't overlap so return 0
      0)))

(defn xor [a b]
  (let [a-num (if a 1 0) b-num (if b 1 0)]
    (if (= 1 (bit-xor a-num b-num))
      true
      false)))

;; https://gist.github.com/danielpcox/c70a8aa2c36766200a95#gistcomment-2711849
(defn deep-merge [& maps]
  (apply merge-with (fn [& args]
                      (if (every? map? args)
                        (apply deep-merge args)
                        (last args)))
         maps))

(defn get-gesture-handler-state [native-event]
  (let [state (obj/getValueByKeys native-event #js["nativeEvent" "state"])]
    (get (clojure.set/map-invert gesture-states)
         state
         :invalid-state)))

(defn get-gesture-handler-ys [native-event]
  {:y
   (obj/getValueByKeys
    native-event #js["nativeEvent" "y"])

   :absolute
   (obj/getValueByKeys
    native-event #js["nativeEvent" "absoluteY"])

   :translation
   (obj/getValueByKeys
    native-event #js["nativeEvent" "translationY"])

   :velocity
   (obj/getValueByKeys
    native-event #js["nativeEvent" "velocityY"])})

(defn get-gesture-handler-xs [native-event]
  {:x
   (obj/getValueByKeys
    native-event #js["nativeEvent" "x"])

   :absolute
   (obj/getValueByKeys
    native-event #js["nativeEvent" "absoluteX"])

   :translation
   (obj/getValueByKeys
    native-event #js["nativeEvent" "translationX"])

   :velocity
   (obj/getValueByKeys
    native-event #js["nativeEvent" "velocityX"])})

(defn get-gesture-handler-scale [native-event]
  (obj/getValueByKeys native-event #js["nativeEvent" "scale"]))

;; https://medium.com/@alehatsman/clojurescript-throttle-debounce-a651dfb66ac
(defn disposable->function [disposable listener interval]
  (let [disposable-instance (disposable. listener interval)]
    (fn [& args]
      (.apply (.-fire disposable-instance) disposable-instance (to-array args)))))

(defn throttle [listener interval]
  (disposable->function Throttle listener interval))

(defn debounce [listener interval]
  (disposable->function Debouncer listener interval))

(def dispatch-debounced
  (debounce dispatch 25))

(def dispatch-throttled
  (throttle dispatch (minutes->ms 0.5)))

(defn combine-paths [& paths]
  (into [] (apply concat paths)))

(defn bucket-path [{:keys [bucket-id]}]
  [:buckets (sp/must bucket-id)])

(defn period-path-sub-bucket [{:keys [period-id bucket-id buckets]}]
  (combine-paths (bucket-path {:bucket-id bucket-id})
                 [(sp/collect-one (sp/submap [:id :color :label]))
                  :periods (sp/must period-id)]))

(defn period-path [{:keys [period-id bucket-id]}]
  (combine-paths (bucket-path {:bucket-id bucket-id})
                 [:periods (sp/must period-id)]))

(defn period-path-insert [{:keys [bucket-id period-id]}]
  (combine-paths (bucket-path {:bucket-id bucket-id})
                 [:periods (sp/keypath period-id)]))

(defn buckets-path []
  [:buckets sp/MAP-VALS])

(defn period-path-no-bucket-id [{:keys [period-id]}]
  (combine-paths (buckets-path)
                 [(sp/collect-one (sp/submap [:id :color :label]))
                  :periods (sp/must period-id)]))

(defn periods-path []
  (combine-paths
   (buckets-path)
   [(sp/collect-one (sp/submap [:id :color :label])) :periods sp/MAP-VALS]))

(defn template-path-no-pattern-id [{:keys [template-id]}]
  [:patterns sp/ALL
   (sp/collect-one (sp/submap [:id]))
   :templates sp/ALL
   #(= (:id %) template-id)])

(defn template-path-pattern-form [{:keys [template-id]}]
  [:forms :pattern-form (sp/collect-one (sp/submap [:id]))
   :templates sp/ALL
   #(= (:id %) template-id)])

(defn period-selections-path []
  [:selection :period sp/MAP-VALS :period-id])

(defn template-selections-path []
  [:selection :template sp/MAP-VALS :template-id])

(def long-time (* 1 60 60 1000))

(def short-time (* 1 60 1000))

(defn element-time-stamp-info [time-stamp pixel-to-minute-ratio displayed-day]
  (let [time-stamp-ms      (abstract-element-timestamp
                            time-stamp
                            displayed-day)
        time-stamp-min     (ms->minutes time-stamp-ms)
        dst-adjustment-min (- (->> displayed-day
                                   (reset-relative-ms (hours->ms 2))
                                   (.getTimeZoneOffset))
                              (->> displayed-day
                                   (reset-relative-ms 0)
                                   (.getTimeZoneOffset)))
        time-stamp-y-pos   (* pixel-to-minute-ratio
                              (+ time-stamp-min
                                 dst-adjustment-min))]

    {:ms    time-stamp-ms
     :min   time-stamp-min
     :y-pos time-stamp-y-pos}))

(defn get-duration [{:keys [start stop]}]
  (if (and (inst? start)
           (inst? stop))
    (->> (.valueOf start)
         (- (.valueOf stop)))
    nil))

(defn day-of-week [index]
  (get {0 "Sun" 1 "Mon" 2 "Tue" 3 "Wed" 4 "Thu" 5 "Fri" 6 "Sat"} index ))
