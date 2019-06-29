(ns time-align-mobile.screens.port
  (:require [time-align-mobile.js-imports :refer [view text touchable-highlight load-file-async read-file-async]]
            ["react" :as react]
            [re-frame.core :refer [dispatch]]))

(defn root [params]
  [view {:style {:flex 1 :justify-content "center" :align-items "center"}}
   [touchable-highlight {:on-press #(dispatch [:share-app-db])}
    [text {:style {:background-color "cyan"
                   :padding          10
                   :border-radius    4}}
     "export"]]

   [touchable-highlight
    {:on-press (fn [ ] (load-file-async
                        (fn [load-result]
                          (if (= "success" (.-type load-result))
                            (read-file-async (.-uri load-result)
                                             (fn [contents]
                                               #(dispatch
                                                 [:import-app-db
                                                  (read-string contents)])) ;; TODO security concern
                                             (fn [error]
                                               (println
                                                "Failed to read file")))))))}
    [text {:style {:background-color "cyan"
                   :padding          10
                   :border-radius    4}}
     "import"]]])
