(ns chem.semtypes)

;; chemical UMLS semantic types 
(def chemical-semantic-type-list 
  [
   "aapp"                               ;amino acid, peptide, or protein
   "carb"                               ;carbohydrate
;; "chem"                               ;chemical
;; "chvf"                               ;chemical viewed functionally
   "chvs"                               ;chemical viewed structurally
   "elii"                               ;element, ion, or isotope
   "inch"                               ;inorganic chemical
   "opco"                               ;organophosphorus compound
   "orch"                               ;organic chemical
   ])

;; possible chemicals
;; "bacs" biologically active substance
;; "phsu" pharmalogic substance
;; "sbst" substance
;; "hops" hazardous or poisonous substance
;; "enzy" enzyme

(def chemical-semantic-type-set (set chemical-semantic-type-list))