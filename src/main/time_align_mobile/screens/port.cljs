(ns time-align-mobile.screens.port
  (:require [time-align-mobile.js-imports :refer [view text touchable-highlight]]
            ["react" :as react]
            [re-frame.core :refer [dispatch]]))

(defn root [params]
  [view {:style {:flex 1 :justify-content "center" :align-items "center"}}
   [touchable-highlight {:on-press #(dispatch [:share-app-db])}
    [text {:style {:background-color "cyan"
                   :padding          10
                   :border-radius    4}}
     "export"]]])
