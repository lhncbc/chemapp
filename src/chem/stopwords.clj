(ns chem.stopwords)

;; Initial stopwords list, should probably be in an external file.

(def stopwords
  #{
    "about"
    "acid"
    "acidic"
    "activation"
    "alkaloid"
    "all"
    "alone"
    "and"
    "are"
    "as"
    "but"
    "compound"
    "compounds"
    "concluded"
    "contains"
    "control"
    "deals"
    "demethylation"
    "determine"
    "disease"
    "distant"
    "enzyme"
    "enzymes"
    "for"
    "from"
    "hypermethylated"
    "hypomethylated"
    "illustrate"
    "induced"
    "intrinsic"
    "its"
    "liquid"
    "mediated"
    "met"
    "methylation"
    "one"
    "optimal"
    "other"
    "region"
    "target"
    "that"
    "the"
    "unmethylated"
    "were"
    "where"
    "with"
    })


(def component-stopwords
  #{
    "induced"
    "demethylation"
    })

;; Notes
;;
;; Proteins are not considered chemicals for the purposes of
;; BioCreative.
;;
;;  Terms ending in "rase", "ribose", and possibly others
;; should be removed from consideration.
;; (this should be evaluated, are there exceptions?)
;;
(def suffix-stopwords
  #{
    "rase"
    "ribose"
    })
