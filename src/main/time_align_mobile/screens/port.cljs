(ns time-align-mobile.screens.port
  (:require [time-align-mobile.js-imports :refer [view
                                                  text
                                                  touchable-highlight
                                                  load-file-async
                                                  button-paper
                                                  read-file-async
                                                  icon-button
                                                  subheading]]
            [cljs.reader :refer [read-string]]
            [time-align-mobile.components.top-bar :refer [top-bar]]
            ["react" :as react]
            [re-frame.core :refer [dispatch]]))

(defn root [params]
  [view {:style {:flex 1}}
   [top-bar {:center-content [subheading "Data"]
             :right-content  [icon-button]}]

   [view {:style {:flex 1 :justify-content "center" :align-items "center"}}

    [button-paper {:mode     "contained"
                   :style    {:margin-bottom 64}
                   :icon     "export"
                   :on-press #(dispatch [:share-app-db])}
     "export"]

    [button-paper {:mode     "contained"
                   :icon     "import"
                   :on-press (fn [ ] (load-file-async
                                      (fn [load-result]
                                        (if (= "success" (.-type load-result))
                                          (read-file-async (.-uri load-result)
                                                           (fn [contents]
                                                             (dispatch
                                                              [:import-app-db
                                                               (read-string contents)])) ;; TODO security concern
                                                           (fn [error]
                                                             (println
                                                              "Failed to read file")))))))}
     "import"]]])
