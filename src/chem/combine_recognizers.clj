(ns chem.combine-recognizers
  (:require [chem.metamap-api :as mm-api])
  (:require [chem.annotations :as annot])
  (:require [chem.semtypes :as semtypes])
  (:require [chem.partial :as partial])
  (:require [chem.normchem :as normchem]))

(defn combine [annotator1 annotator2 document]
  (let [result0 (annotator1 document)
        result1 (annotator2 document)]
    (hash-map
        :annotations (concat (result0 :annotations) (result1 :annotations)))))

(defn combination-1 [document]
  (let [result0 (partial/match document)
        result1 (normchem/process-document document)]
    (hash-map
        :annotations (concat (result0 :annotations) (result1 :annotations)))))


