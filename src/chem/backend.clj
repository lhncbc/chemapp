(ns chem.backend
  (:require [chem.mongodb :as chemdb])
  (:require [monger.core :as mg])
  (:require [monger.collection :as mc])
  (:require [chem.partial :as partial])
  (:require [chem.normchem :as normchem])
  (:require [chem.process :as process])
  (:gen-class))

(defn init []
  (chemdb/init "chem"))

(defn list-docids 
  ([len] (list-docids "chem" len))
  ([dbname len]
     (map #(:docid %)
          (mc/find "training.abstracts" :limit len))))

(defn list-documents
  ([len] (list-documents "chem" len))
  ([dbname len]
     (mc/find :training.abstracts :limit len)))

(defn get-document [docid]
  (first (mc/find-one "training.abstracts" {:docid docid})))

(defn process-chemdner-document [document engine]
  (case engine
    "partial"  (process/annotate-chemdner-document partial/match engine document)
    "normchem" (process/annotate-chemdner-document normchem/process-document engine document)
    ))
