(ns time-align-mobile.app
  (:require
    ["react-native" :as rn]
    ["react" :as react]
    [reagent.core :as r :refer [atom]]
    [re-frame.core :refer [subscribe dispatch dispatch-sync]]
    [shadow.expo :as expo]
    [time-align-mobile.components.day :refer [bottom-sheet-ref snap-bottom-sheet close-bottom-sheet]]
    [time-align-mobile.handlers]
    [time-align-mobile.helpers :refer [deep-merge]]
    [time-align-mobile.subs]
    [time-align-mobile.navigation :as nav]
    [cljs.reader :refer [read-string]]
    [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                       oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
    [time-align-mobile.styles :refer [theme]]
    [time-align-mobile.db :refer [app-db] :rename {app-db default-app-db}]
    [time-align-mobile.config :refer [amplitude-api-key]]
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
                                          amplitude-init
                                          surface
                                          mi
                                          mci
                                          text
                                          view
                                          status-bar
                                          image
                                          subheading
                                          button-paper
                                          surface
                                          divider
                                          alert
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
  (let [version-from-state @(subscribe [:get-version])]
    [view {:style {:flex 1 :justify-content "center" :align-items "center"
                   :background-color (->> theme :colors :background)}}
     (->> nav/screens-map
          (filter #(:in-drawer %))
          (sort-by #(:position-drawer %))
          (map (fn [{:keys [icon label id]}]
                 (let [{:keys [family name]} icon

                       params {:name  name
                               :style {:margin-right 8
                                       :width        32}
                               :color (->> theme :colors :primary)
                               :size  24}

                       label-element [subheading label]
                       icon-element  (case family
                                       "EvilIcons"              [ei params]
                                       "FontAwesome"            [fa params]
                                       "IonIcons"               [ic params]
                                       "Entypo"                 [en params]
                                       "MaterialCommunityIcons" [mci params]
                                       "MaterialIcons"          [mi params])]

                   [touchable-ripple {:key      (str "icon-" name)
                                      :on-press (fn [_]
                                                  (dispatch
                                                   [:navigate-to
                                                    {:current-screen id
                                                     :params         nil}])
                                                  (dispatch [:set-menu-open false]))}

                    [view {:flex-direction  "row"
                           :justify-content "flex-start"
                           :align-items     "center"
                           :padding-left    8
                           :width           200
                           :height          32}
                     icon-element
                     label-element]]))))

     [text-paper {:style {:margin 8
                          :color  (->> theme :colors :placeholder)}} version-from-state]]))

(defn root []
  (fn []
    (let [navigation (subscribe [:get-navigation])
          menu-open  (subscribe [:get-menu-open])]
      (fn []
        (let [current-screen (:current-screen @navigation)]
          [paper-provider {:theme (clj->js theme)}
           [side-menu
            {:menu             (r/as-element [drawer-list])
             :is-open          @menu-open
             :disable-gestures true
             :on-change        #(do
                                  (dispatch [:set-menu-open %])
                                  (dispatch [:set-day-fab-visible (not %)])
                                  (if (and (some? @bottom-sheet-ref)
                                           (or (= :day current-screen)
                                               (= :pattern-planning current-screen)))
                                    (do (close-bottom-sheet bottom-sheet-ref :period)
                                        (close-bottom-sheet bottom-sheet-ref :template))))}

            [view {:style {:flex             1
                           :background-color (get-in theme [:colors :background])}}
             [status-bar {:hidden true}]
             (if-let [screen-comp (some #(if (= (:id %) current-screen)
                                           (:screen %))
                                        nav/screens-map)]
               [screen-comp (:params @navigation)]
               [view [text "That screen doesn't exist"]])]]])))))

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
  (read-file-from-dd-async
   "app-db"
   (fn [value]
     (let [app-db (read-string value)]
       (if (some? app-db)
         (dispatch-sync [:load-db (deep-merge default-app-db app-db)])
         (dispatch-sync [:set-version version]))))
   (fn [error]
     (alert "error reading file")))

  (amplitude-init amplitude-api-key)

  (start))

