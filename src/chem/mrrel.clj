(ns chem.mrrel
  (:require [somnium.congomongo :as m]))

(defn list-objects-for-subject [subject-cui]
  ^{:doc "list object cuis for supplied subject cui in MRREL" }
  (m/fetch :mrrel :where {:cui2 subject-cui, :rela "isa"}))

(defn display-objects-for-subject [subject-cui]
  ^{:doc "display object cuis for supplied subject cui in MRREL" }
  (dorun 
   (map #(prn (list (:cui1 %) (:rela %) (:cui2 %)))
        (list-objects-for-subject subject-cui))))


