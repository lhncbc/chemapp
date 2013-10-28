(ns chem.process
  (:require [chem.metamap-api :as mm-api])
  (:require [chem.annotations :as annot])
  (:require [chem.semtypes :as semtypes])
  (:require [chem.partial :as partial])
  (:require [chem.normchem :as normchem])
  (:require [chem.combine-recognizers :as combinelib]))

(defn process [engine document]
  "Generate annotations from document text."
  (case engine
    "metamap" (let [mmapi-inst (mm-api/api-instantiate)
                    mm-annotations (mm-api/handle-result-list 
                                    (mm-api/process-string 
                                     mmapi-inst
                                     document))
                    spans (annot/get-mm-spans
                           mm-annotations
                           semtypes/chemical-semantic-type-list)
                    matched-terms (annot/build-matchedwordset-chemicals
                                   mm-annotations)]
                (hash-map :spans spans
                            :annotations mm-annotations
                            :matched-terms matched-terms))
    "partial" (partial/partial-match document)
    "fragment" (partial/fragment-match document)
    "normchem" (normchem/process-document document)
    "combine1" (combinelib/combination-1 document)
    ))

