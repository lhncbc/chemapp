(ns chem.lookup
  (:require [chem.mongodb :as mongodb])
  (:require [chem.pubchem :as pubchem])
  (:gen-class))

;; first look up chemical in database, if not found try PUBCHEM.

(defn lookup [query]
  (let [local-result (mongodb/lookup  :normchem query)]
    (if (nil? local-result)
      (pubchem/lookup query)
      local-result)))