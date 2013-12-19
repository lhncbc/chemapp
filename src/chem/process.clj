(ns chem.process
  (:require [chem.metamap-api :as mm-api])
  (:require [chem.annotations :as annot])
  (:require [chem.metamap-annotation :as mm-annot])
  (:require [chem.semtypes :as semtypes])
  (:require [chem.partial :as partial])
  (:require [chem.normchem :as normchem])
  (:require [chem.combine-recognizers :as combinelib]))

(defn process [engine document]
  "Generate annotations from document text."
  (case engine
    "metamap" (let [mmapi-inst (mm-api/api-instantiate)]
                (mm-annot/annotate-document mmapi-inst document))
    "partial" (partial/partial-match document)
    "fragment" (partial/fragment-match document)
    "normchem" (normchem/process-document document)
    "combine1" (combinelib/combination-1 document)
    ))

(defn process-document-seq [engine document-seq]
    "Generate annotations from raw document sequence text."
  (case engine
    "metamap" (let [mmapi-inst (mm-api/api-instantiate)]
                (map (fn [document]
                       (mm-annot/annotate-document mmapi-inst document))
                     document-seq))
    "partial"  (map partial/partial-match     document-seq)
    "fragment" (map partial/fragment-match    document-seq)
    "normchem" (map normchem/process-document document-seq)
    "combine1" (map combinelib/combination-1  document-seq)))

(defn annotate-chemdner-document [annotate-fn engine document]
  (hash-map :docid           (document :docid)
            :title-result    (annotate-fn  (:title document))
            :abstract-result (annotate-fn  (:abstract document))
            :method          engine))

(defn process-chemdner-document-seq [engine document-seq]
    "Generate annotations from structured document sequence text.
     Document elements contain fields mapped by the keywords :docid, :title, :abstract."
  (case engine
    "metamap" (let [mmapi-inst (mm-api/api-instantiate)]
                (doall
                 (map (fn [document]
                        (hash-map :docid           (document :docid)
                                  :title-result    (mm-annot/annotate-document mmapi-inst (:title document))
                                  :abstract-result (mm-annot/annotate-document mmapi-inst (:abstract document))
                                  :method          engine))
                     document-seq)))
    "partial"  (map #(annotate-chemdner-document partial/partial-match engine %)     document-seq)
    "fragment" (map #(annotate-chemdner-document partial/fragment-match engine %)    document-seq)
    "normchem" (doall (map #(annotate-chemdner-document normchem/process-document engine %) document-seq))
    "combine1" (doall (map #(annotate-chemdner-document combinelib/combination-1 engine %)  document-seq))))
                    
