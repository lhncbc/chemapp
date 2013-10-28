(ns chem.semtypes)

;; chemical UMLS semantic types 
(def chemical-semantic-type-list 
  [
   "inch"                               ;inorganic chemical
   "orch"                               ;organic chemical
   "chem"                               ;chemical
   "chvf"                               ;chemical viewed functionally
   "chvs"                               ;chemical viewed structurally
   "elii"                               ;element, ion, or isotope
   ])

;; possible chemicals
;; "bacs" biologically active substance
;; "phsu"  pharmalogic substance
;; "sbst" substance
;; "hops" hazardous or poisonous substance

(def chemical-semantic-type-set (set chemical-semantic-type-list))