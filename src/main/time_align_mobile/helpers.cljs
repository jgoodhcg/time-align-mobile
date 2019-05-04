(ns time-align-mobile.helpers
  (:require
   ;; [zprint.core :refer [zprint]]

   [re-frame.core :refer [reg-sub]]
            [com.rpl.specter :as sp :refer-macros [select select-one setval transform]]))

(def day-ms
  ;; 24 hours in millis
  (* 24 60 60 1000))

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

(defn get-ms
  "takes a js/date and returns milliseconds since 00:00 that day. Essentially relative ms for the day."
  [date]
  (let [h  (.getHours date)
        m  (.getMinutes date)
        s  (.getSeconds date)
        ms (.getMilliseconds date)]
    (+
     (-> h
         (* 60)
         (* 60)
         (* 1000))
     (-> m
         (* 60)
         (* 1000))
     (-> s (* 1000))
     ms)))

(defn hours->ms [hours]
  (-> hours
      (* 60)
      (* 60)
      (* 1000)))

(defn minutes->ms [minutes]
  (-> minutes
      (* 60)
      (* 1000)))

(defn sec->ms [seconds]
  (* seconds 1000))

(defn date->y-pos [date-time total-height]
  (-> date-time
      (get-ms)
      (/ day-ms)
      (* total-height)))

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

(defn reset-relative-ms [ms date]
  (let [year           (.getFullYear date)
        month          (.getMonth date)
        day            (.getDate date)
        zero-day    (js/Date. year month day 0 0 0)
        zero-day-ms (.valueOf zero-day)]
    (js/Date. (+ zero-day-ms ms))))

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

