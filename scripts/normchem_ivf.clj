(ns normchem-ivf
  (:require [chem.irutils :refer [create-index lookup nmslookup] :as civf]
            [chem.irutils-normchem :as ir-normchem]))

;;
;; This module generates indices for Normalized chemical index in
;; data/ivf/normchem/tables
;;

(def ivf "data/ivf")
(def tablepath (str ivf "/normchem/tables"))
(def indexpath (str ivf "/normchem/indices"))
(def indexname "normchem2017")
(def normchem-index (create-index tablepath indexpath indexname))
