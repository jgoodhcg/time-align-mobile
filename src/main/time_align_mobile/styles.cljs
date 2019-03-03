(ns time-align-mobile.styles)

(defn field-label-changeable-style [changes field]
  {:color         (if (contains? @changes field)
                    "blue"
                    "grey")
   :width 80
   :padding-right 5})


(def field-label-style {:color         "grey"
                        :padding-right 5
                        :width 75})
