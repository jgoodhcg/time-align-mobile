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
    (and (= (.getFullYear date-a)
            (.getFullYear date-b))
         (= (.getMonth date-a)
            (.getMonth date-b))
         (= (.getDate date-a)
            (.getDate date-b)))
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
  (let [hours   (.getHours date)
        minutes (.getMinutes date)
        seconds (.getSeconds date)
        millis  (.getMilliseconds date)]
    (+ (hours->ms hours)
       (minutes->ms minutes)
       (sec->ms seconds)
       millis)))

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
    (str hours ":" minutes)))

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
  (debounce dispatch 100))

(def dispatch-throttled
  (throttle dispatch 25))
