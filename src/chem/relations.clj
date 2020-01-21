(ns chem.relations
;;    (:require [somnium.congomongo :as m])
)

;; mostly isa relations 

;;     (defn collect-relations [subject-cui rela]
;;       "Get a list of relation with subject-cui."
;;       (m/fetch :mrrel
;;         :where {:cui1 subject-cui :rela rela}))

;;     (defn collect-relation-cuis [subject-cui rela]
;;       "Get a list of cuis related to subject-cui."
;;       (set (map #(:cui2 %)
;;                 (m/fetch :mrrel
;;                   :where {:cui1 subject-cui :rela rela}))))

;;     user> (def monosaccharide-isa-relations
;;              (chem.relations/collect-relations "C0026492" "isa"))
;;     #'user/monosaccharide-isa-relations
;;     user> (count monosaccharide-isa-relations)
;;     40
;;     user> (def monosaccharide-cuis
;;             (set (map #(:cui2 %) monosaccharide-isa-relations)))
;;     #'user/monosaccharide-cuis
;;     user> (count monosaccharide-cuis)
;;     28
;;     user> (def disaccharide-isa-relations
;;             (chem.relations/collect-relations "C0012611" "isa"))
;;     #'user/disaccharide-isa-relations
;;     user> (count disaccharide-isa-relations)
;;     19
;;     user> (def disaccharide-cuis  (set (map #(:cui2 %) disaccharide-isa-relations)))
;;     #'user/disaccharide-cuis
;;     user> (count disaccharide-cuis)
;;     10
;;     user> (def aminoglycoside-isa-relations
;;             (chem.relations/collect-relations "C0002556" "isa"))
;;     #'user/aminoglycoside-isa-relations
;;     user> (count aminoglycoside-isa-relations)
;;     75
;;     user> (def aminoglycoside-cuis
;;       (set (map #(:cui2 %) aminoglycoside-isa-relations)))
;;     #'user/aminoglycoside-cuis
;;     user> (count aminoglycoside-cuis)
;;     48
;;     user> (chem.utils/write-elements "monosaccharide_cuis.dat" monosaccharide-cuis)
;;     28
;;     user> (chem.utils/write-elements "disaccharide_cuis.dat" disaccharide-cuis)
;;     10
;;     user> (chem.utils/write-elements "aminoglycoside_cuis.dat" aminoglycoside-cuis)
;;     48
;;     user> ?
