(ns user
  (:require [chem.mongodb])
  (:require [chem.metamap-api]))

;; initialize any resources for annotators
;; (chem.mongodb/init)
(def mmapi-inst (chem.metamap-api/api-instantiate))
