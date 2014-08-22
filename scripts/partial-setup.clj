(ns user
  (:require [chem.core]
            [chem.partial]
            [chem.evaluation :as eval]
            [chem.stacking]
            [chem.stacking-prep :as stacking-prep]
            [chem.mti-filtering :as mti-filtering])
  (:use [chem.utils]))


;; Load ChemDNER Training and Development sets
;;
(load-file "scripts/setupscript.clj")

;; annotate record using partial-match based annotation
(def partial-annotated-records (map chem.partial/partial-annotate-record user/training-records))

(def partial-chemdner-resultlist
  (apply concat
         (map #(eval/docid-termlist-to-chemdner-result
                (:docid %)
                (eval/get-annotation-termlist % :partial))
              partial-annotated-records)))

(eval/write-chemdner-resultlist-to-file "partial-chemdner-dev-resultlist.txt"
                                        partial-chemdner-resultlist)

(def partial-meta-data (stacking-prep/chemdner-resultlist-to-meta-data partial-chemdner-resultlist))
(chem.stacking-prep/write-data "partial-meta-data.txt" partial-meta-data)


