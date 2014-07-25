(ns user
  (:require [chem.core]
            [chem.setup]
            [chem.process]
            [chem.mti-filtering :as mti-filtering])
  (:use [chem.utils]))

;; load training and test documents along with gold file
(def chemdner-training-data            (chem.setup/load-chemdner-training-data))
(def chemdner-gold-map                 (chemdner-training-data :chemdner-training-cdi-gold))
(def chemdner-training-cdi-gold        (chemdner-training-data :chemdner-training-cdi-gold))
(def chemdner-development-cdi-gold     (chemdner-training-data :chemdner-development-cdi-gold))

(def chemdner-training-cdi-gold-map    
  (mti-filtering/gen-training-annotations-map chemdner-training-cdi-gold))
(def chemdner-development-cdi-gold-map 
  (mti-filtering/gen-training-annotations-map chemdner-development-cdi-gold))

(defn gen-record-map [records] 
  (into {} (map #(vec (list (:docid %) %)) records)))

(def training-records
   (map #(chem.process/add-chemdner-gold-annotations chemdner-training-cdi-gold-map %)
        (chemdner-training-data :training-records)))

(def training-record-map (gen-record-map training-records))

(def test-records (chemdner-training-data :test-records))

(def test-record-map (gen-record-map test-records))

(def development-records
   (map #(chem.process/add-chemdner-gold-annotations chemdner-development-cdi-gold-map %)
        (chemdner-training-data :development-records)))

(def development-record-map (gen-record-map development-records))
