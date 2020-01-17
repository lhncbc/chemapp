(ns chem.cdi-views
  (:require [chem.backend :as backend]
            [chem.cdi :refer [pr-str-cdi-result]])
  (:gen-class))

(defn pubmed-output [pmid]
  (pr-str-cdi-result pmid (backend/index-pubmed-document :irutils-normchem pmid)))

(defn pubmed-cdi-output [pmid]
  (pr-str-cdi-result pmid (backend/index-pubmed-document :irutils-normchem pmid)))
