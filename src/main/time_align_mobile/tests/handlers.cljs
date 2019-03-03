(ns time-align-mobile.tests.handlers
  (:require [cljs.test :as t :refer-macros [deftest is]]
            [time-align-mobile.handlers :as handlers]
            [time-align-mobile.db :as db :refer [app-db]]))

(deftest initialize-db
  (is (= app-db (handlers/initialize-db [] []))))

(deftest navigate-to-bucket-form
  (is (= {:db {:navigation {:current-screen :bucket
                            :params {:bucket-id 12345}}
               :active-filter nil}
          :dispatch [:load-bucket-form 12345]}
         (handlers/navigate-to {:db {}} [[] {:current-screen :bucket
                                             :params {:bucket-id 12345}}]))))

(deftest navigate-to-period-form
  (is (= {:db {:navigation {:current-screen :period
                            :params {:period-id 12345}}
               :active-filter nil}
          :dispatch [:load-period-form 12345]}
         (handlers/navigate-to {:db {}} [[] {:current-screen :period
                                             :params {:period-id 12345}}]))))

(deftest navigate-to-template-form
  (is (= {:db {:navigation {:current-screen :template
                            :params {:template-id 12345}}
               :active-filter nil}
          :dispatch [:load-template-form 12345]}
         (handlers/navigate-to {:db {}} [[] {:current-screen :template
                                             :params {:template-id 12345}}]))))

(deftest navigate-to-filter-form
  (is (= {:db {:navigation {:current-screen :filter
                            :params {:filter-id 12345}}
               :active-filter nil}
          :dispatch [:load-filter-form 12345]}
         (handlers/navigate-to {:db {}} [[] {:current-screen :filter
                                             :params {:filter-id 12345}}]))))

(deftest navigate-to-non-form
  (is (= {:db {:navigation {:current-screen :day
                            :params {}}
               :active-filter nil}}
         (handlers/navigate-to {:db {}} [[] {:current-screen :day
                                             :params {}}]))))

(deftest load-bucket-form
  (is (= {:buckets [{:id 12345
                     :data {}}]
          :forms {:bucket-form {:id 12345
                                :data "{}\n"}}}
         (handlers/load-bucket-form {:buckets [{:id 12345
                                                :data {}}]} [[] 12345]))))

(deftest load-period-form
  (let [db {:buckets [{:id 12345
                       :color "#112233"
                       :label "bucket label"
                       :periods [{:id 6789
                                  :data {}}]}]}]
    (is (= (merge db {:forms {:period-form {:id 6789
                                            :data "{}\n"
                                            :bucket-id 12345
                                            :bucket-color "#112233"
                                            :bucket-label "bucket label"}}})
           (handlers/load-period-form db [:whatever 6789])))))

(deftest load-filter-form
  (let [db {:filters [{:id 12345 :predicates {} :sort {}}]}]
    (is (= (merge db {:forms {:filter-form {:id 12345
                                            :predicates "{}\n"
                                            :sort "{}\n"}}})
           (handlers/load-filter-form db [:whatever 12345])))))

(deftest load-template-form
  (let [db {:buckets [{:id 12345
                       :color "blue"
                       :label "label"
                       :templates [{:id 67890
                                    :data {}}]}]}]
    (is = (merge db {:forms {:template-forms {:id 67890
                                              :bucket-id 12345
                                              :bucket-color "blue"
                                              :bucket-label "label"
                                              :data "{}\n"}}}))))

(deftest add-new-bucket
  (let [db  {:buckets []}
        id  12345
        now (js/Date. 2018 0 1)]
    (is (= {:db       {:buckets [{:id          12345
                                  :label       ""
                                  :created     now
                                  :last-edited now
                                  :data        {}
                                  :color       "#ff1122"
                                  :templates   nil
                                  :periods     nil}]}
            :dispatch [:navigate-to {:current-screen :bucket
                                     :params         {:bucket-id id}}]}
           (handlers/add-new-bucket {:db db} [:whatever {:id id :now now}])))))

(deftest add-new-period
  (let [id 12345
        bucket-id 45678
        db {:buckets [{:id bucket-id
                       :periods nil}]}
        now (js/Date. 2018 0 1)]
    (is (=  {:db {:buckets [{:id bucket-id
                             :periods [{:id id
                                        :created now
                                        :last-edited now
                                        :label ""
                                        :data {}
                                        :planned true
                                        :start now
                                        :stop (js/Date.
                                               (+ (.valueOf now)
                                                  (* 1000 60)))}]}]}
             :dispatch [:navigate-to {:current-screen :period
                                      :params {:period-id id}}]}
           (handlers/add-new-period {:db db} [:whatever {:bucket-id bucket-id
                                                         :id id
                                                         :now now}])))))

(deftest add-new-filter
  (let [db  {:filters nil}
        id  12345
        now (js/Date. 2018 0 1)]
    (is (= {:db       {:filters [{:id          id
                                  :label       ""
                                  :created     now
                                  :last-edited now
                                  :compatible  []
                                  :sort        nil
                                  :predicates  []}]}
            :dispatch [:navigate-to {:current-screen :filter
                                     :params         {:filter-id id}}]}
           (handlers/add-new-filter {:db db} [:whatever {:id  id
                                                         :now now}])))))

(deftest add-new-template
  (let [id        12345
        bucket-id 45678
        now       (js/Date. 2018 0 1)
        db        {:buckets [{:id        bucket-id
                              :templates nil}]}]
    (is (= {:db       {:buckets [{:id        bucket-id
                                  :templates [{:id          id
                                               :created     now
                                               :last-edited now
                                               :label       ""
                                               :data        {}
                                               :planned     true
                                               :start       {:hour   (.getHours now)
                                                             :minute (.getMinutes now)}
                                               :stop        {:hour   (.getHours now)
                                                             :minute (+
                                                                      5
                                                                      (.getMinutes now))}
                                               :duration    nil}]}]}
            :dispatch [:navigate-to {:current-screen :template
                                     :params         {:template-id id}}]}
           (handlers/add-new-template {:db db} [:whatever {:id        id
                                                           :bucket-id bucket-id
                                                           :now       now}])))))

(deftest add-template-period
  (let [id          12345
        bucket-id   45678
        template-id 87654
        now         (js/Date. 2018 0 1)
        duration    (* 1000 60 60)
        data        {}
        template    {:id        template-id
                     :data      data
                     :start     {:hour   0
                                 :minute 0}
                     :duration  duration
                     :bucket-id bucket-id}
        db          {:buckets [{:id        bucket-id
                                :templates [template]
                                :periods   nil}]}]
    (is (= {:db       {:buckets [{:id        bucket-id
                                  :templates [template]
                                  :periods   [{:id          id
                                               :data        {:template-id template-id}
                                               :created     now
                                               :last-edited now
                                               :start       now
                                               :stop        (js/Date.
                                                             (+ (.valueOf now)
                                                                duration))}]}]}
            :dispatch [:navigate-to {:current-screen :period
                                     :params         {:period-id id}}]}
           (handlers/add-template-period {:db db} [:whatever {:template template
                                                              :id id
                                                              :now now}])))))

(deftest update-bucket-form
  (is (= {:forms {:bucket-form {:id 12345 :data "{}\n"}}}
         (handlers/update-bucket-form {:forms {:bucket-form {}}}
                                      [:whatever {:id 12345 :data "{}\n"}]))))

(deftest update-period-form
  (let [db {:buckets [{:id 45678 :label "bucket-label"}]}]
    (is (= (merge db {:forms {:period-form {:id           12345
                                            :data         "{}\n"
                                            :bucket-label "bucket-label"
                                            :bucket-id    45678}}})
           (handlers/update-period-form db [:whatever {:id        12345
                                                       :data      "{}\n"
                                                       :bucket-id 45678}])))))

(deftest update-filter-form
  (is (= {:forms {:filter-form {:id 12345}}}
         (handlers/update-filter-form {} [:whatever {:id 12345}]))))

(deftest update-template-form
  (let [db {:buckets [{:id 45678 :label "bucket-label"}]}]
    (is (= (merge db {:forms {:template-form {:id           12345
                                              :data         "{}\n"
                                              :bucket-label "bucket-label"
                                              :bucket-id    45678}}})
           (handlers/update-template-form db [:whatever {:id      12345
                                                         :data      "{}\n"
                                                         :bucket-id 45678}])))))

(deftest update-active-filter
  (is (= {:active-filter 12345}
         (handlers/update-active-filter {} [:whatever 12345]))))

(deftest delete-bucket
  (is (= {:db {:buckets []
               :forms {:bucket-form nil}}
          :dispatch [:navigate-to {:current-screen :buckets}]}
         (handlers/delete-bucket {:db {:buckets [{:id 12345}]
                                       :forms {:bucket-form {}}}}
                                 [:whatever 12345]))))

(deftest delete-period
  (let [db      {:buckets [{:id 12345 :periods [{:id 6789}]}]
                 :forms   {:period-form {:id 6789}}}
        context {:db       {:buckets [{:id 12345 :periods []}]
                            :forms {:period-form nil}}
                 :dispatch [:navigate-to {:current-screen :periods}]}]
    (is (= context
           (handlers/delete-period {:db db} [:whatever 6789])))))

(deftest delete-filter
  (is (= {:db       {:filters []
                     :forms   {:filter-form nil}}
          :dispatch [:navigate-to {:current-screen :filters}]}
         (handlers/delete-filter {:db {:filters [{:id 12345}]
                                       :forms   {:filter-form {}}}}
                                 [:whatever 12345]))))

(deftest delete-template
  (let [db      {:buckets [{:id 12345 :templates [{:id 6789}]}]
                 :forms   {:template-form {:id 6789}}}
        context {:db       {:buckets [{:id 12345 :templates []}]
                            :forms {:template-form nil}}
                 :dispatch [:navigate-to {:current-screen :templates}]}]
    (is (= context
           (handlers/delete-template {:db db} [:whatever 6789])))))

(deftest save-bucket-form
  (let [now           (js/Date. 2018 0 1)
        forms         {:forms {:bucket-form {:id 12345 :data "{}\n"}}}
        bucket-before {:id 12345}
        bucket-after  (merge bucket-before {:data        {}
                                            :last-edited now})]
    (is (= {:db       (merge forms {:buckets [bucket-after]})
            :dispatch [:load-bucket-form 12345]}
           (handlers/save-bucket-form {:db (merge forms {:buckets [bucket-before]})}
                                      [:whatever now])))))

(deftest save-filter-form
  (let [now           (js/Date. 2018 0 1)
        forms         {:forms {:filter-form {:id 12345
                                             :predicates "[{}]\n"
                                             :sort "{}\n"}}}
        filter-before {:id 12345}
        filter-after  (merge filter-before {:predicates  [{}]
                                            :sort {}
                                            :last-edited now})]
    (is (= {:db       (merge forms {:filters [filter-after]})
            :dispatch [:load-filter-form 12345]}
           (handlers/save-filter-form {:db (merge forms {:filters [filter-before]})}
                                      [:whatever now])))))

(deftest save-period-form
  (is (= {:db       {:forms   {:period-form {:id   12345
                                             :data "{}\n"
                                             :bucket-id 67890
                                             :last-edited (js/Date. 2017 11 0)}}
                     :buckets [{:id 67890 :periods [{:id   12345
                                                     :data {}
                                                     :last-edited (js/Date. 2018 0 1)}]}]}
          :dispatch [:load-period-form 12345]}
         (handlers/save-period-form {:db {:forms {:period-form {:id   12345
                                                                :data "{}\n"
                                                                :bucket-id 67890
                                                                :last-edited (js/Date. 2017 11 0)}}
                                          :buckets [{:id 67890
                                                     :periods [{:id 12345}]}]}}
                                    [:whatever (js/Date. 2018 0 1)]))))

(deftest save-template-form
  (is (= {:db       {:forms   {:template-form {:id   12345
                                             :data "{}\n"
                                             :bucket-id 67890
                                             :last-edited (js/Date. 2017 11 0)}}
                     :buckets [{:id 67890 :templates [{:id   12345
                                                     :data {}
                                                     :last-edited (js/Date. 2018 0 1)}]}]}
          :dispatch [:load-template-form 12345]}
         (handlers/save-template-form {:db {:forms {:template-form {:id   12345
                                                                :data "{}\n"
                                                                :bucket-id 67890
                                                                :last-edited (js/Date. 2017 11 0)}}
                                          :buckets [{:id 67890
                                                     :templates [{:id 12345}]}]}}
                                    [:whatever (js/Date. 2018 0 1)]))))

(t/run-tests)
