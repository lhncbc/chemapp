(ns chem.lookup
  (:require [chem.irutils-normchem :as normchem])
  (:require [chem.pubchem :as pubchem])
  (:gen-class))

;; first look up chemical in database, if not found try PUBCHEM.

(defn lookup [query]
  (let [local-result (normchem/lookup  query)]
    (if (nil? local-result)
      (pubchem/lookup query)
      local-result)))
