(ns chem.chemdner-data
  (:require [chem.chemdner-paths :as paths]
            [chem.chemdner-tools :as chemdner-tools]))

;; Load CHEMDNER training, testing, and development corpora.

(defn load-chemdner-training-data []
  ^{:doc "load chemdner training data into map."}
  (let [training-records (chemdner-tools/load-chemdner-abstracts paths/training-text)
        chemdner-training-cdi-gold (chem.chemdner-tools/load-training-document-entity-gold-standard 
                                    paths/training-entities)
        chemdner-development-cdi-gold (chem.chemdner-tools/load-training-document-entity-gold-standard
                                       paths/development-entities)
        development-records (chemdner-tools/load-chemdner-abstracts paths/development-text)
        test-records (chemdner-tools/load-chemdner-abstracts paths/test-text)
        
        development-annotations (chemdner-tools/load-chemdner-annotations paths/development-annotations)
        training-annotations (chemdner-tools/load-chemdner-annotations paths/training-annotations)]
    (hash-map
     :training-filename paths/training-text
     :training-records training-records
     :chemdner-training-cdi-gold chemdner-training-cdi-gold
     :development-filename paths/development-text
     :development-records development-records
     :chemdner-development-cdi-gold chemdner-development-cdi-gold
     :training-record-map (into {} (map #(vec (list (:docid %) %)) training-records))
     :test-records test-records
     :training-annotations training-annotations
     :development-annotations development-annotations
     )))
