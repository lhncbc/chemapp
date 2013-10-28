(ns chem.stopwords)

;; Initial stopwords list, should probably be in an external file.

(def stopwords
  #{
    "activation"
    "compound"
    "compounds"
    "demethylation"
    "enzyme"
    "enzymes"
    "hypermethylated"
    "hypomethylated"
    "induced"
    "methylation"
    "one"
    "optimal"
    "unmethylated"
    })

(def component-stopwords
  #{
    "induced"
    })