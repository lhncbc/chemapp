(ns user
  (:require [chem.chemdner-data :as chemdner-data]
            [chem.chemdner-tools :as chemdner-tools]
            [chem.utils :refer :all]))

;; load training and test documents along with gold file
(defonce chemdner-data                     (chemdner-data/load-chemdner-data))
(defonce chemdner-gold-map                 (chemdner-data :chemdner-training-cdi-gold))
(defonce chemdner-training-cdi-gold        (chemdner-data :chemdner-training-cdi-gold))
(defonce chemdner-development-cdi-gold     (chemdner-data :chemdner-development-cdi-gold))
(defonce chemdner-training-cem-gold        (chemdner-data :chemdner-training-cem-gold))
(defonce chemdner-development-cem-gold     (chemdner-data :chemdner-development-cem-gold))

(defonce chemdner-training-cdi-gold-map    
  (chemdner-tools/gen-training-annotations-map chemdner-training-cdi-gold))
(defonce chemdner-development-cdi-gold-map
  (chemdner-tools/gen-training-annotations-map chemdner-development-cdi-gold))

(defonce chemdner-training-cem-gold-map    
  (chemdner-tools/gen-training-extents-map chemdner-training-cem-gold))
(defonce chemdner-development-cem-gold-map 
  (chemdner-tools/gen-training-extents-map chemdner-development-cem-gold))

(defn gen-record-map [records] 
  (into {} (map #(vec (list (:docid %) %)) records)))

(defonce training-records
   (map #(chemdner-tools/add-chemdner-gold-annotations chemdner-training-cdi-gold-map %)
        (chemdner-data :training-records)))

(defonce training-record-map (gen-record-map training-records))

(defonce test-records (chemdner-data :test-records))

(defonce test-record-map (gen-record-map test-records))

(defonce development-records
   (map #(chemdner-tools/add-chemdner-gold-annotations chemdner-development-cdi-gold-map %)
        (chemdner-data :development-records)))

(defonce development-record-map (gen-record-map development-records))

(defonce development-term-attribute-map
  (chem.chemdner-tools/gen-term-attribute-map (chemdner-data :development-annotations)))
(defonce training-term-attribute-map
  (chem.chemdner-tools/gen-term-attribute-map (chemdner-data :training-annotations)))

(defonce development-docid-term-attribute-map
  (chem.chemdner-tools/gen-docid-term-attribute-map (chemdner-data :development-annotations)))
(defonce training-docid-term-attribute-map
  (chem.chemdner-tools/gen-docid-term-attribute-map (chemdner-data :training-annotations)))


(defonce development-docid-span-start-attribute-map
  (chem.chemdner-tools/gen-docid-span-start-attribute-map (chemdner-data :development-annotations)))
(defonce training-docid-span-start-attribute-map
  (chem.chemdner-tools/gen-docid-span-start-attribute-map (chemdner-data :training-annotations)))

(defonce development-docid-annotation-list-map
  (chem.chemdner-tools/gen-docid-annotation-list-map (chemdner-data :development-annotations)))
(defonce training-docid-annotation-list-map
  (chem.chemdner-tools/gen-docid-annotation-list-map (chemdner-data :training-annotations)))
