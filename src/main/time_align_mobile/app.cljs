(ns time-align-mobile.app
  (:require
    ["react-native" :as rn]
    ["react" :as react]
    [reagent.core :as r :refer [atom]]
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [shadow.expo :as expo]
    [time-align-mobile.components.day :refer [bottom-sheet-ref snap-bottom-sheet]]
    [time-align-mobile.handlers]
    [time-align-mobile.helpers :refer [deep-merge]]
    [time-align-mobile.subs]
    [time-align-mobile.navigation :as nav]
    [cljs.reader :refer [read-string]]
    [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                       oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
    [time-align-mobile.styles :refer [theme]]
    [time-align-mobile.db :refer [app-db] :rename {app-db default-app-db}]
    [time-align-mobile.js-imports :refer [ReactNative
                                          ei
                                          en
                                          fa
                                          version
                                          back-handler
                                          app-state
                                          paper-provider
                                          ic
                                          text-paper
                                          card
                                          surface
                                          mi
                                          text
                                          view
                                          status-bar
                                          image
                                          subheading
                                          button-paper
                                          surface
                                          divider
                                          touchable-highlight
                                          touchable-ripple
                                          drawer-layout
                                          side-menu
                                          read-file-from-dd-async
                                          portal-host
                                          secure-store-get!]]))

;; must use defonce and must refresh full app so metro can fill these in
;; at live-reload time `require` does not exist and will cause errors
;; must use path relative to :output-dir

(defonce splash-img (js/require "../assets/shadow-cljs.png"))

(def styles
  ^js (-> {:container
           {:flex 1
            :backgroundColor "#fff"
            :alignItems "center"
            :justifyContent "center"}
           :title
           {:fontWeight "bold"
            :fontSize 24
            :color "blue"}}
          (clj->js)
          (rn/StyleSheet.create)))

(defn drawer-list []
  [surface {:style {:flex 1 :justify-content "center" :align-items "center"}}
   [subheading {:style {:font-weight   "bold"
                        :margin-bottom 16}}
    "Screens"]
   (->> nav/screens-map
        (filter #(:in-drawer %))
        (sort-by #(:position-drawer %))
        (map (fn [{:keys [icon label id]}]
               (let [{:keys [family name]} icon
                     params                {:name  name
                                            :style {:margin-right 25
                                                    :width        32}
                                            :color (->> theme :colors :placeholder)
                                            :size  24}
                     label-element         [text-paper label]
                     icon-element          (case family
                                             "EvilIcons"     [ei params]
                                             "FontAwesome"   [fa params]
                                             "IonIcons"      [ic params]
                                             "Entypo"        [en params]
                                             "MaterialIcons" [mi params])]

                 [touchable-ripple {:key      (str "icon-" name)
                                    :on-press (fn [_]
                                                ;; TODO remove bucket id params when done testing

                                                (dispatch
                                                 [:navigate-to
                                                  {:current-screen id
                                                   :params
                                                   (cond
                                                     (= id :bucket)
                                                     {:bucket-id (uuid "a7396f81-38d4-4d4f-ab19-a7cef18c4ea2")}

                                                     (= id :period)
                                                     {:period-id (uuid "a8404f81-38d4-4d4f-ab19-a7cef18c4531")}

                                                     (= id :template)
                                                     {:template-id (uuid "c52e4f81-38d4-4d4f-ab19-a7cef18c8882")}

                                                     (= id :filter)
                                                     {:filter-id (uuid "bbc34081-38d4-4d4f-ab19-a7cef18c1212")}
                                                     :else nil)}]))}
                  [view {:flex-direction  "row"
                         :justify-content "flex-start"
                         :align-items     "center"
                         :padding-left    20
                         :width           200
                         :height          32}
                   icon-element
                   label-element]]))))
   [text-paper {:style {:margin 8
                        :color  (->> theme :colors :placeholder)}} version]])

(defn root []
  (fn []
    (let [navigation (subscribe [:get-navigation])]
      (fn []
        [paper-provider {:theme (clj->js theme)}
         [side-menu
          {:menu      (r/as-element [drawer-list])
           :on-change #(do
                         (println bottom-sheet-ref)
                         (snap-bottom-sheet bottom-sheet-ref 0)
                         (dispatch [:select-element-edit {:element-type :period :bucket-id nil :period-id nil}])
                         (dispatch [:select-element-edit {:element-type :template :bucket-id nil :period-id nil}]))}

          [view {:style {:flex             1
                         :background-color (get-in theme [:colors :background])}}
           [status-bar {:hidden true}]
           (if-let [screen-comp (some #(if (= (:id %) (:current-screen @navigation))
                                         (:screen %))
                                      nav/screens-map)]
             [screen-comp (:params @navigation)]
             [view [text "That screen doesn't exist"]])]]]))))

(defn start
  {:dev/after-load true}
  []
  (expo/render-root (r/as-element [root]))
  (r/force-update-all))

(defn init []
  (dispatch-sync [:initialize-db])

  ;; set back handler
  (.addEventListener back-handler
                     "hardwareBackPress"
                     (fn [] (dispatch [:navigate-back])
                       true))

  ;; load previous state
  ;; TODO uncomment this
  ;; (read-file-from-dd-async
  ;;  "app-db"
  ;;  (fn [value]
  ;;    (let [app-db (read-string value)]
  ;;      (if (some? app-db)
  ;;        (dispatch-sync [:load-db (deep-merge default-app-db app-db)]))))
  ;;  (fn [error]
  ;;    (println "error reading file")))

  ;; start ticking
  ;; (js/setInterval #(dispatch [:tick (js/Date.)]) 1000)

  (start))

