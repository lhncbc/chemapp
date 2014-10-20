(ns chem.stopwords)

;; Initial stopwords list, should probably be in an external file.

(def stopwords
  #{
    "about"
    "access"
    "acid"
    "acidic"
    "acidification"
    "acids"
    "activation"
    "alkaline"
    "alkaloid"
    "all"
    "alleviate"
    "alone"
    "and"
    "are"
    "as"
    "attack"
    "box"
    "but"
    "catechol"
    "compound"
    "compounds"
    "concluded"
    "contains"
    "control"
    "costimulatory"
    "crystalline"
    "deals"
    "demethylation"
    "determine"
    "different"
    "difficult"
    "diffusion"
    "diminish"
    "diode"
    "directional"
    "disease"
    "disorders"
    "displays"
    "disrupts"
    "distant"
    "distinct"
    "distributions"
    "environmental"
    "enzyme"
    "enzymes"
    "estimation"
    "fine"
    "for"
    "from"
    "gene"
    "haze"
    "hormone"
    "hypermethylated"
    "hypomethylated"
    "illustrate"
    "induced"
    "intrinsic"
    "its"
    "label"
    "line"
    "liquid"
    "max"
    "mediated"
    "met"
    "methylation"
    "monoclonal"
    "monolayers"
    "monotherapy"
    "nasal"
    "one"
    "optimal"
    "other"
    "phosphorylation"
    "progress"
    "protocol"
    "rapid"
    "region"
    "result"
    "stimulator"
    "target"
    "that"
    "the"
    "time"
    "tool"
    "unknown"
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
