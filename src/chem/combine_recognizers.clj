(ns chem.combine-recognizers
  (:require [chem.metamap-api :as mm-api])
  (:require [chem.annotations :as annot])
  (:require [chem.semtypes :as semtypes])
  (:require [chem.partial :as partial])
  (:require [chem.normchem :as normchem]))

(defn combination-1 [document]
  (let [result0 (partial/partial-match document)
        result1 (normchem/process-document document)]
    (hash-map
        :spans (concat (result0 :spans) (result1 :spans))
        :annotations (concat (result0 :annotations) (result1 :annotations))
        :matched-terms (concat (result0 :matched-terms) (result1 :matched-terms)))))

