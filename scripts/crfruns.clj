(ns user
  (:require [chem.core])
  (:require [chem.setup])
  (:require [chem.annotations])
  (:require [chem.mallet])
  (:use [chem.utils]))

(load-file "scripts/setupscript.clj")

;; write training features to data/features/training (term-based [multiword])
;; (dorun (map #(chem.mallet/write-features-for-record "data/features/training-term-based" %)
;;            training-records))

;; write training features to data/features/training (token-based [multiword, one word per line])
(dorun (map #(chem.mallet/write-features-for-record
       "data/features/training"
       %
       chem.mallet/gen-token-based-feature-list-from-record)
            training-records))

;; write development features to data/features/development

(dorun (map #(chem.mallet/write-features-for-record
       "data/features/development"
       %
       chem.mallet/gen-token-based-feature-list-from-record)
            development-records))
