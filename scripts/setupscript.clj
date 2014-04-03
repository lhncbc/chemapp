(ns user
  (:require [chem.core])
  (:require [chem.setup])
  (:require [chem.process])
  (:use [chem.utils]))

;; load training and test documents along with gold file
(def chemdner-training-data (chem.setup/load-chemdner-training-data))
(def chemdner-gold-map (chemdner-training-data :chemdner-gold-map))
(def chemdner-training-cdi-gold-map (chemdner-training-data :chemdner-training-cdi-gold-map))
(def chemdner-development-cdi-gold-map (chemdner-training-data :chemdner-development-cdi-gold-map))

(def training-records
  (doall
   (map #(chem.process/add-chemdner-gold-annotations chemdner-gold-map %)
        (chemdner-training-data :training-records))))

(def test-records (chemdner-training-data :test-records))

(def development-record
  (doall
   (map #(chem.process/add-chemdner-gold-annotations chemdner-gold-map %)
        (chemdner-training-data :development-records))))

