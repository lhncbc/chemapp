(ns user
  (:require [chem.core]
            [chem.setup]
            [chem.process]
            [chem.ctx-utils]
            [chem.evaluation :as eval]
            [chem.stacking]
            [chem.stacking-prep :as stacking-prep]
            [chem.mti-filtering :as mti-filtering])
  (:use [chem.utils]))

;; Load ChemDNER Training and Development sets
;;
(load-file "scripts/setupscript.clj")

;; annotate record using combination of annotation methods
(def enchilada0-annotated-records (map chem.stacking/enchilada0-annotate-record user/training-records))
(def enchilada0-chemdner-resultlist
  (apply concat
         (map #(eval/docid-termlist-to-chemdner-result
                (:docid %)
                (eval/get-annotation-termlist % :enchilada0))
              enchilada0-annotated-records)))

(eval/write-chemdner-resultlist-to-file "enchilada0-chemdner-resultlist.txt"
                                        enchilada0-chemdner-resultlist)

(def enchilada0-meta-data (chemdner-resultlist-to-meta-data enchilada0-chemdner-resultlist))
(stacking-prep/write-data "enchilada0-meta-data.txt" enchilada0-meta-data)
