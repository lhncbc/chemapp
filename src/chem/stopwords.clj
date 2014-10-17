(ns chem.stopwords)

;; Initial stopwords list, should probably be in an external file.

(def stopwords
  #{
    "about"
    "acid"
    "acidic"
    "acidification"
    "acids"
    "activation"
    "alkaline"
    "alkaloid"
    "all"
    "alone"
    "and"
    "are"
    "as"
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
    "mediated"
    "met"
    "methylation"
    "monoclonal"
    "monolayers"
    "monotherapy"
    "one"
    "optimal"
    "other"
    "phosphorylation"
    "progress"
    "protocol"
    "rapid"
    "region"
    "stimulator"
    "target"
    "that"
    "the"
    "time"
    "tool"
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
