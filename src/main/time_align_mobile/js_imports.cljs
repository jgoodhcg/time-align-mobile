(ns time-align-mobile.js-imports
  (:require
   ["react-native" :as react-native]
   ["expo" :as e]
   ["@expo/vector-icons" :as expo-icons]
   ["react-native-color-picker" :as react-native-color-picker]
   ["react-native-elements" :as react-native-elements]
   ["react-native-paper" :as paper]
   ["react-native-modal-datetime-picker" :as react-native-date-picker]
   ["moment-timezone" :as moment-timezone]
   ["react-native-keyboard-aware-scroll-view" :as kasv]
   ["expo-secure-store" :as SecureStore]
   ["expo-document-picker" :as DocumentPicker]
   ["expo-file-system" :as fs
    :refer []]
   ;; ["expo-doucument-picker" :as dp] ;; TODO in sdk-33
   ["expo-constants" :as expo-constants]
   ["color" :as color]
   ["reanimated-bottom-sheet" :as rbs]
   ["react-native-gesture-handler"
    :refer [PanGestureHandler
            PinchGestureHandler
            TapGestureHandler
            ScrollView
            LongPressGestureHandler
            DrawerLayout
            RectButton
            State]]
   [oops.core :refer [oget oset! ocall oapply ocall! oapply!
                      oget+ oset!+ ocall+ oapply+ ocall!+ oapply!+]]
   ["react-native-side-menu" :default SideMenu]
   [reagent.core :as r :refer [atom]]))

(def ReactNative react-native)
(def expo e)

(def evil-icons (oget expo-icons "EvilIcons"))
(def font-awesome (oget expo-icons "FontAwesome"))
(def ionicons (oget expo-icons "Ionicons"))
(def entypo (oget expo-icons "Entypo"))
(def material-icons (oget expo-icons "MaterialIcons"))
(def material-community-icons (oget expo-icons "MaterialCommunityIcons"))

(def ei (r/adapt-react-class evil-icons))
(def fa (r/adapt-react-class font-awesome))
(def ic (r/adapt-react-class ionicons))
(def en (r/adapt-react-class entypo))
(def mi (r/adapt-react-class material-icons))
(def mci (r/adapt-react-class material-community-icons))

;; TODO is this stuff used?
(def app-state (oget ReactNative "AppState"))
(def pan-responder (oget ReactNative "PanResponder"))
(def animated (oget ReactNative "Animated"))
(def animated-xy (oget animated "ValueXY"))
(def animated-value (.-Value animated))
(def animated-view (r/adapt-react-class (oget animated "View")))
;; </>

(def status-bar (r/adapt-react-class (oget ReactNative "StatusBar")))
(def dimensions (oget ReactNative "Dimensions"))
(defn get-device-width [] (-> dimensions
                              (ocall "get" "window")
                              (oget "width")))
(def text (r/adapt-react-class (oget ReactNative "Text")))
(def view (r/adapt-react-class (oget ReactNative "View")))
(def scroll-view (r/adapt-react-class (oget ReactNative "ScrollView")))
(def image (r/adapt-react-class (oget ReactNative "Image")))
(def flat-list (r/adapt-react-class (oget ReactNative "FlatList")))
(def touchable-highlight (r/adapt-react-class (oget ReactNative "TouchableHighlight")))
(def touchable-without-feedback (r/adapt-react-class (oget ReactNative "TouchableWithoutFeedback")))
(def modal (r/adapt-react-class (oget ReactNative "Modal")))
(def picker (r/adapt-react-class (oget ReactNative "Picker")))
(def picker-item (r/adapt-react-class (oget ReactNative "Picker" "Item")))
(def Alert (oget ReactNative "Alert"))
(defn alert
  ([title]
   (ocall Alert "alert" title))
  ([title subtitle]
   (ocall Alert "alert" title subtitle))
  ([title subtitle options]
   ;; TODO wrap options in a clj->js and camel->kebab thread
   (ocall Alert "alert" title subtitle options)))
(def switch (r/adapt-react-class (oget ReactNative "Switch")))

(def text-input (r/adapt-react-class (oget ReactNative "TextInput")))

(def keyboard-aware-scroll-view (r/adapt-react-class (oget kasv "KeyboardAwareScrollView")))
(def platform (oget ReactNative "Platform"))

(def data-font-family (if (= (.-OS platform) "ios")
                        "Courier"
                        "monospace"))

(def ColorPicker (oget react-native-color-picker "ColorPicker"))
(def color-picker (r/adapt-react-class ColorPicker))
(def DatePicker (oget react-native-date-picker "default"))
(def date-time-picker (r/adapt-react-class DatePicker))

(def moment-tz (oget moment-timezone "tz"))

(def secure-store SecureStore)
(def share-api (oget ReactNative "Share"))

(defn share [title message]
  (ocall share-api "share"
         (clj->js {:title   title
                   :message message})))
(defn secure-store-set! [key value ]
  ;; TODO include options and camel->kebab
  (ocall secure-store "setItemAsync" key value))
(defn secure-store-get! [key then-fn]
  ;; TODO include options and camel->kebab
  (-> (ocall secure-store "getItemAsync" key)
      (ocall "then" then-fn)))

(defn get-default-timezone []
  (ocall moment-tz "guess"))
(defn set-hour-for-date [date hour zone]
  (-> (moment-tz date zone)
      (ocall "hour" hour)
      (ocall "startOf" "hours")
      js/Date.))
(defn start-of-today [date zone]
  (set-hour-for-date date 0 zone))
(defn end-of-today [date zone]
  (set-hour-for-date date 20 zone)) ;;Set to 20 to avoid straddling the date line
(defn make-date
  ([] (ocall (moment-tz (js/Date.) "UTC") "toDate"))
  ( [year month day]
   (make-date year month day 0))
  ( [year month day hour]
   (make-date year month day hour 0))
  ( [year month day hour minute]
   (make-date year month day hour minute 0))
  ( [year month day hour minute second]
   (make-date year month day hour minute second 0))
  ( [year month day hour minute second millisecond]
   (-> (js/Date. (ocall js/Date "UTC" year (- 1 month) day hour minute second millisecond))
       (moment-tz "UTC"))))
(defn format-date [date]
  (ocall (moment-tz date (get-default-timezone))
         "format"
         "YYYY-MM-DD-HH-mm-ss")) ;; TODO rename this format-datetime
(defn format-date-day [date] ;; TODO rename this format-date
  (ocall (moment-tz date (get-default-timezone))
         "format"
         "YYYY-MM-DD"))
(defn format-time [date]
  (ocall (moment-tz date (get-default-timezone))
          "format"
          "HH-mm"))

(def document-directory (-> fs
                            (.-documentDirectory)))
(defn write-file-to-dd! [file-name contents-as-string]
  (-> fs
      (.writeAsStringAsync (str document-directory file-name)
                           contents-as-string)))
(defn read-file-async [file-uri success-callback error-callback]
  (-> fs
      (.readAsStringAsync file-uri)
      (.then success-callback error-callback)))
(defn read-file-from-dd-async [file-name success-callback error-callback]
  (read-file-async
   (str document-directory file-name)
   success-callback
   error-callback))

(def version (-> expo-constants
                 (.-default)
                 (.-manifest)
                 (.-version)))

(defn load-file-async [callback]
  (-> DocumentPicker
      (.getDocumentAsync)
      (.then callback)))

(def back-handler (oget ReactNative "BackHandler"))

(def Provider (oget paper "Provider"))
(def paper-provider (r/adapt-react-class Provider))
(def Card (oget paper "Card"))
(def card (r/adapt-react-class Card))
(def default-theme (oget paper "DefaultTheme"))
(def Modal (oget paper "Modal"))
(def modal-paper (r/adapt-react-class Modal))
(def portal (r/adapt-react-class (oget paper "Portal")))
(def portal-host (r/adapt-react-class (-> paper (oget "Portal") (oget "Host"))))
(def surface (r/adapt-react-class (oget paper "Surface")))
(def text-input-paper (r/adapt-react-class (oget paper "TextInput")))
(def subheading (r/adapt-react-class (oget paper "Subheading")))
(def chip (r/adapt-react-class (oget paper "Chip")))
(def button-paper (r/adapt-react-class (oget paper "Button")))
(def switch-paper (r/adapt-react-class (oget paper "Switch")))
(def touchable-ripple (r/adapt-react-class (oget paper "TouchableRipple")))
(def toggle-button (r/adapt-react-class (oget paper "ToggleButton")))
(def text-paper (r/adapt-react-class (oget paper "Text")))
(def icon-button (r/adapt-react-class (oget paper "IconButton")))
(def divider (r/adapt-react-class (oget paper "Divider")))

(def notifications (oget e "Notifications"))

(defn schedule-notification [{:keys [title body date callback]}]
  (-> notifications
      (ocall "scheduleLocalNotificationAsync"
             (-> {:title title
                  :body  body}
                 clj->js)
             (-> {:time date}
                 clj->js))
      (ocall "then"
             (fn [id]
               (callback id)))))

(def drawer-layout (r/adapt-react-class DrawerLayout))
(def pan-gesture-handler (r/adapt-react-class PanGestureHandler))
(def pinch-gesture-handler (r/adapt-react-class PinchGestureHandler))
(def tap-gesture-handler (r/adapt-react-class TapGestureHandler))
(def long-press-gesture-handler (r/adapt-react-class LongPressGestureHandler))
(def rect-button (r/adapt-react-class RectButton))
(def scroll-view-gesture-handler (r/adapt-react-class ScrollView))
(def gesture-handler-states State)
;; TODO fully namespace these keys
(def gesture-states {:active       (.-ACTIVE gesture-handler-states)
                     :undetermined (.-UNDETERMINED gesture-handler-states)
                     :failed       (.-FAILED gesture-handler-states)
                     :began        (.-BEGAN gesture-handler-states)
                     :cancelled     (.-CANCELLED gesture-handler-states)
                     :end          (.-END gesture-handler-states)})

(def side-menu (r/adapt-react-class SideMenu))

(defn color->hex-string [color]
  (-> color
      (.hex)
      (subs 2)
      (#(str "#" %))))

(defn color-light? [color-string]
  (-> color-string
      (color)
      (.isLight)))

(defn lighten-color [color-string factor]
  (-> color-string
      (color)
      (.lighten factor)
      color->hex-string))

(def bottom-sheet (r/adapt-react-class (.-default rbs)))
