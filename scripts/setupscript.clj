(ns user
  (:require [chem.core]
            [chem.setup]
            [chem.process]
            [chem.chemdner-tools :as chemdner-tools]
            [chem.mti-filtering :as mti-filtering])
  (:use [chem.utils]))

;; load training and test documents along with gold file
(defonce chemdner-training-data            (chem.setup/load-chemdner-training-data))
(defonce chemdner-gold-map                 (chemdner-training-data :chemdner-training-cdi-gold))
(defonce chemdner-training-cdi-gold        (chemdner-training-data :chemdner-training-cdi-gold))
(defonce chemdner-development-cdi-gold     (chemdner-training-data :chemdner-development-cdi-gold))

(defonce chemdner-training-cdi-gold-map    
  (chemdner-tools/gen-training-annotations-map chemdner-training-cdi-gold))
(defonce chemdner-development-cdi-gold-map 
  (chemdner-tools/gen-training-annotations-map chemdner-development-cdi-gold))

(defn gen-record-map [records] 
  (into {} (map #(vec (list (:docid %) %)) records)))

(defonce training-records
   (map #(chemdner-tools/add-chemdner-gold-annotations chemdner-training-cdi-gold-map %)
        (chemdner-training-data :training-records)))

(defonce training-record-map (gen-record-map training-records))

(defonce test-records (chemdner-training-data :test-records))

(defonce test-record-map (gen-record-map test-records))

(defonce development-records
   (map #(chemdner-tools/add-chemdner-gold-annotations chemdner-development-cdi-gold-map %)
        (chemdner-training-data :development-records)))

(defonce development-record-map (gen-record-map development-records))
