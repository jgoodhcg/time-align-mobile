(ns time-align-mobile.helpers
  (:require
   [zprint.core :refer [zprint]]))


(defn same-day? [date-a date-b]
  (and (= (.getFullYear date-a)
          (.getFullYear date-b))
       (= (.getMonth date-a)
          (.getMonth date-b))
       (= (.getDate date-a)
          (.getDate date-b))))

(defn print-data [data]
  (str data)
  ;; (with-out-str (zprint data 40))  ;; TODO doesn't like zprint :(
)
