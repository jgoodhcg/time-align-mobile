(ns time-align-mobile.db
  (:require [clojure.spec.alpha :as s]
            [spec-tools.data-spec :as ds]
            [spec-tools.core :as st]
            [clojure.string :as string]
            [clojure.test.check.generators :as gen]
            [time-align-mobile.navigation :as nav]
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
(defn start-before-stop-template [template]
  (let [start-hour   (:hour (:start template))
        start-minute (:minute (:start template))
        stop-hour    (:hour (:stop template))
        stop-minute  (:minute (:stop template))]
    (if
        ;; Check that it has time stamps
        (and
         (contains? template :start)
         (contains? template :stop)
         (some? (:start template))
         (some? (:stop template)))

      ;; If it has time stamps they need to be valid
      ;; Compare hours then minutes
      (or (> stop-hour start-hour)
          (if (= stop-hour start-hour)
            (> stop-minute start-minute)))

      ;; Passes if it has no time stamps
      true)))

(def template-data-spec {:id          uuid?
                         :label       string?
                         :created     ::moment
                         :last-edited ::moment
                         :data        map?
                         :planned     boolean?
                         :start       (ds/maybe {:hour   integer?
                                                 :minute integer?})
                         :duration    (ds/maybe integer?)})
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
                       :templates   (ds/maybe [template-spec])
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
   :compatible  [(s/spec #{:bucket :period :template :filter})]
   :sort        (ds/maybe {:path [keyword?]
                           :ascending boolean?})
   :predicates  [{:path [keyword?]
                  :value string? ;; TODO the form uses read and that coerces all values to strings
                  :negate boolean?}]})

;; app-db
(def app-db-spec
  (ds/spec {:spec {:forms           {:bucket-form
                                     (ds/maybe (merge bucket-data-spec
                                                      {:data string?}))
                                     :period-form
                                     (ds/maybe (merge period-data-spec
                                                      {:data         string?
                                                       :bucket-id    uuid?
                                                       :bucket-label string?
                                                       :bucket-color ::color}))
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
                   :active-filter   (ds/maybe uuid?)
                   :selected-period (ds/maybe uuid?)
                   :filters         [filter-data-spec]
                   :navigation      {:current-screen ::screen
                                     :params         (ds/maybe map?)}

                   :buckets         [bucket-spec]
                   :time-navigators {:day      ::moment
                                     :calendar ::moment
                                     :report   ::moment}
                   :config          {:auto-log-time-align boolean?}
                   :period-in-play-id (ds/maybe uuid?)
                   :now             inst?}
            :name ::app-db}))
(def app-db
  {:forms             {:bucket-form   nil
                       :period-form   nil
                       :template-form nil
                       :filter-form   nil}
   :selected-period   nil
   :active-filter     nil
   :filters           [{:id          (uuid "bbc34081-38d4-4d4f-ab19-a7cef18c1212")
                        :label       "bucket label sort"
                        :created     (new js/Date 2018 4 28 15 57)
                        :compatible  [:period :template]
                        :last-edited (new js/Date 2018 4 28 15 57)
                        :sort        {:path      [:bucket-label]
                                      :ascending false}
                        :predicates  []}
                       {:id          (uuid "cda44081-38d4-4d4f-ab19-a7cef18c1718")
                        :label       "Home workouts"
                        :created     (new js/Date 2018 4 28 15 57)
                        :compatible  [:period :template :filter :bucket]
                        :last-edited (new js/Date 2018 4 28 15 57)
                        :sort        {:path      [:label]
                                      :ascending false}
                        :predicates  [{:path   [:data :location]
                                       :value  "home"
                                       :negate false}]}
                       {:id          (uuid "defaaa81-38d4-4d4f-ab19-a7cef18c1300")
                        :label       "Night life"
                        :created     (new js/Date 2018 4 28 15 57)
                        :last-edited (new js/Date 2018 4 28 15 57)
                        :compatible  [:period :template]
                        :sort        {:path      [:created]
                                      :ascending true}
                        :predicates  [{:path   [:data :category]
                                       :value  "night life"
                                       :negate false}]}]
   :navigation        {:current-screen :day
                       :params         nil}
   :buckets           [{:id          (uuid "a7396f81-38d4-4d4f-ab19-a7cef18c4ea2")
                        :label       "Exercise"
                        :created     (new js/Date 2018 4 28 15 57)
                        :last-edited (new js/Date 2018 4 28 15 57)
                        :data        {}
                        :color       "#11aa11"
                        :templates   nil
                        :periods     (concat
                                      (->> (range 2)
                                           (map #(gen/generate (s/gen period-spec))))
                                      [{:id          (uuid "a8404f81-38d4-4d4f-ab19-a7cef18c4531")
                                        :created     (new js/Date 2018 9 0 15 25)
                                        :last-edited (new js/Date 2018 9 0 15 25)
                                        :label       ""
                                        :planned     false
                                        :start       (new js/Date 2018 9 0 15 25)
                                        :stop        (new js/Date 2018 9 0 16 17)
                                        :data        {:location "gym"
                                                      :session  {:plank                   [{:s 1 :time {:min 1 :sec 0}}]
                                                                 :arch-hold               [{:s 1 :time {:min 1 :sec 0}}]
                                                                 :side-plank              [{:s 1 :time {:min 1 :sec 0}}]
                                                                 :lumbar-extension        [{:s 1 :r 5}]
                                                                 :hip-tilts-wall          [{:s 1 :r 10}]
                                                                 :thoracic-extension-wall [{:s 1 :r 10}]
                                                                 :dislocates              [{:s 1 :r 10}]
                                                                 :pushup                  [{:s 1 :r 20}]
                                                                 :incline-pushup          [{:s 1 :r 20}]
                                                                 :squat                   [{:s 3 :r 5}]
                                                                 :dip-forward             [{:s 1 :r 8}]
                                                                 :dip-neutral             [{:s 2 :r 5}]
                                                                 :chinup                  [{:s 2 :r 7}]}}}
                                       {:id          (uuid "e3314f81-38d4-4d4f-ab19-a7cef17c4182")
                                        :created     (new js/Date 2018 9 1 9 30)
                                        :last-edited (new js/Date 2018 9 1 9 30)
                                        :label       ""
                                        :planned     false
                                        :start       (new js/Date 2018 9 1 9 30)
                                        :stop        (new js/Date 2018 9 1 9 40)
                                        :data        {:location "home"
                                                      :session  {:plank            [{:s 1 :time {:min 1 :sec 0}}]
                                                                 :arch-hold        [{:s 1 :time {:min 1 :sec 0}}]
                                                                 :side-plank       [{:s 1 :time {:min 1 :sec 0}}]
                                                                 :lumbar-extension [{:s 1 :r 5}]}}}]
                                      )}
                       {:id          (uuid "8c3907da-5222-408c-aba4-777f0a1204de")
                        :label       "Social"
                        :created     (new js/Date 2018 4 28 15 57)
                        :last-edited (new js/Date 2018 4 28 15 57)
                        :data        {}
                        :color       "#1111aa"
                        :templates   [{:id          (uuid "c52e4f81-38d4-4d4f-ab19-a7cef18c8882")
                                       :created     (new js/Date 2018 4 28 15 57)
                                       :last-edited (new js/Date 2018 4 28 15 57)
                                       :label       "Drinks with friends"
                                       :planned     true
                                       :start       {:hour 19 :minute 0}
                                       :duration    (* 2 ;; 2 hours converted to ms
                                                       60 60 1000)
                                       :data        {:category "night life"}}
                                      {:id          (uuid "b89e4f81-38d4-4d4f-ab19-a7cef18c6647")
                                       :created     (new js/Date 2018 4 28 15 57)
                                       :last-edited (new js/Date 2018 4 28 15 57)
                                       :label       "Breakfast with Girlfriend"
                                       :planned     true
                                       :start       {:hour 7 :minute 0}
                                       :duration    (* 2 ;; 2 hours converted to ms
                                                       60 60 1000)
                                       :data        {:category "night life"}}
                                      {:id          (uuid "da3e4f81-38d4-4d4f-ab19-a7cef18c6641")
                                       :created     (new js/Date 2018 4 28 15 57)
                                       :last-edited (new js/Date 2018 4 28 15 57)
                                       :label       "Date with Girlfriend"
                                       :planned     true
                                       :start       nil
                                       :duration    nil
                                       :data        {}}
                                      {:id          (uuid "ef111f81-38d4-4d4f-ab19-a7cef18caa31")
                                       :created     (new js/Date 2018 4 28 15 57)
                                       :last-edited (new js/Date 2018 4 28 15 57)
                                       :label       "Lunch with Brother"
                                       :planned     true
                                       :start       nil
                                       :duration    (* 1.5 60 60 1000)
                                       :data        {}}]
                        :periods     (->> (range 1)
                                          (map #(gen/generate (s/gen period-spec))))}
                       {:id          (uuid "4b9b07da-5222-408c-aba4-777f0a1203af")
                        :label       "Work"
                        :created     (new js/Date 2018 4 28 15 57)
                        :last-edited (new js/Date 2018 4 28 15 57)
                        :data        {}
                        :color       "#aa1111"
                        :templates   nil
                        :periods     (->> (range 5)
                                          (map #(gen/generate (s/gen period-spec))))}]
   :time-navigators   {:day      (js/Date.)
                       :calendar (js/Date.)
                       :report   (js/Date.)}
   :config            {:auto-log-time-align true}
   :period-in-play-id nil
   :now               (js/Date.)})

;; TODO use https://facebook.github.io/react-native/docs/appstate.html to log all time in app
;; old initial state of app-db
