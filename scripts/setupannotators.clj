(ns users
  (:require [chem.mongodb])
  (:require [chem.metamap-api]))

;; initialize any resources for annotators
(chem.mongodb/init)
(def mmapi (chem.metamap-api/api-instantiate))
