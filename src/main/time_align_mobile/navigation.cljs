(ns time-align-mobile.navigation
  (:require [time-align-mobile.screens.calendar :as calendar-screen]
            [time-align-mobile.screens.day-planning :as day-planning-screen]
            [time-align-mobile.screens.period-form :as period-form-screen]
            [time-align-mobile.screens.period-list :as period-list-screen]
            [time-align-mobile.screens.queue :as queue-screen]
            [time-align-mobile.screens.report :as report-screen]
            [time-align-mobile.screens.bucket-form :as bucket-form-screen]
            [time-align-mobile.screens.bucket-list :as bucket-list-screen]
            [time-align-mobile.screens.pattern-list :as pattern-list-screen]
            [time-align-mobile.screens.pattern-form :as pattern-form-screen]
            [time-align-mobile.screens.pattern-planning :as pattern-planning-screen]
            [time-align-mobile.screens.template-form :as template-form-screen]
            [time-align-mobile.screens.template-list :as template-list-screen]
            [time-align-mobile.screens.filter-form :as filter-form-screen]
            [time-align-mobile.screens.filter-list :as filter-list-screen]
            [time-align-mobile.screens.port :as port-screen]))

;; https://expo.github.io/vector-icons/
(def bucket-screen {:id       :bucket
                    :label           "bucket"
                    :screen          bucket-form-screen/root
                    :in-drawer       false
                    :position-drawer 999
                    :icon            nil})

(def buckets-screen {:id       :buckets
                     :label           "buckets"
                     :screen          bucket-list-screen/root
                     :in-drawer       true
                     :position-drawer 3
                     :icon            {:family "FontAwesome"
                                       :name   "list"}})

(def pattern-screen {:id              :pattern
                     :label           "pattern"
                     :screen          pattern-form-screen/root
                     :in-drawer       false
                     :position-drawer nil
                     :icon            nil})

(def patterns-screen {:id              :patterns
                      :label           "patterns"
                      :screen          pattern-list-screen/root
                      :in-drawer       true
                      :position-drawer 3.5
                      :icon            {:family "MaterialIcons"
                                        :name   "palette"}})

(def pattern-planning-screen {:id              :pattern-planning
                              :label           "pattern planning"
                              :screen          pattern-planning-screen/root
                              :in-drawer       false
                              :position-drawer nil
                              :icon            nil})

(def period-screen {:id       :period
                    :label           "period"
                    :screen          period-form-screen/root
                    :in-drawer       false
                    :position-drawer nil
                    :icon            nil})

(def periods-screen {:id       :periods
                     :label           "periods"
                     :screen          period-list-screen/root
                     :in-drawer       true
                     :position-drawer 4
                     :icon            {:family "Entypo"
                                       :name   "time-slot"}})

(def template-screen {:id       :template
                      :label           "template"
                      :screen          template-form-screen/root
                      :in-drawer       false
                      :position-drawer nil
                      :icon            nil})

(def templates-screen {:id       :templates
                       :label           "templates"
                       :screen          template-list-screen/root
                       :in-drawer       true
                       :position-drawer 5
                       :icon            {:family "FontAwesome"
                                         :name   "wpforms"}})

(def filter-screen {:id       :filter
                    :label           "filter"
                    :screen          filter-form-screen/root
                    :in-drawer       false
                    :position-drawer nil
                    :icon            nil})

(def filters-screen {:id       :filters
                     :label           "filters"
                     :screen          filter-list-screen/root
                     :in-drawer       true
                     :position-drawer 6
                     :icon            {:family "FontAwesome"
                                       :name   "filter"}})

(def day-planning-screen {:id       :day
                 :label           "day"
                 :screen          day-planning-screen/root
                 :in-drawer       true
                 :position-drawer 1
                 :icon            {:family "FontAwesome"
                                   :name   "columns"}})

(def calendar-screen {:id       :calendar
                      :label           "calendar"
                      :screen          calendar-screen/root
                      :in-drawer       true
                      :position-drawer 2
                      :icon            {:family "Entypo"
                                        :name   "calendar"}})

(def report-screen {:id       :report
                    :label           "report"
                    :screen          report-screen/root
                    :in-drawer       true
                    :position-drawer 8
                    :icon            {:family "Entypo"
                                      :name   "bar-graph"}})

(def queue-screen {:id              :queue
                   :label           "queue"
                   :screen          queue-screen/root
                   :in-drawer       true
                   :position-drawer 7
                   :icon            {:family "MaterialIcons"
                                     :name   "queue"}})

(def port-screen {:id              :port
                  :label           "port"
                  :screen          port-screen/root
                  :in-drawer       true
                  :position-drawer 8
                  :icon            {:family "MaterialIcons"
                                    :name   "import-export"}})

(def screens-map [bucket-screen
                  buckets-screen
                  pattern-screen
                  patterns-screen
                  pattern-planning-screen
                  period-screen
                  periods-screen
                  template-screen
                  templates-screen
                  filter-screen
                  filters-screen
                  day-planning-screen
                  calendar-screen
                  report-screen
                  queue-screen
                  port-screen])
