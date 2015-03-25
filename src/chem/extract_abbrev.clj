(ns chem.extract-abbrev
  (:import (bioc.tool ExtractAbbrev)))

(defonce ^:dynamic *extract-abbrev-instance* (new ExtractAbbrev))

(defn extract-abbr-pairs-string 
  [original-string]
  (map (fn [abbr-info]
         (let [short-form (.shortForm abbr-info)
               short-form-start (.shortFormIndex abbr-info)
               long-form (.longForm abbr-info)
               long-form-start (.longFormIndex abbr-info)]
           (hash-map :short-form {:text short-form
                                  :span {:start short-form-start
                                         :end (+ short-form-start (count short-form))}}
                     :long-form {:text long-form
                                 :span {:start long-form-start
                                        :end (+ long-form-start (count long-form))}})))
       (.extractAbbrPairsString *extract-abbrev-instance* original-string)))
