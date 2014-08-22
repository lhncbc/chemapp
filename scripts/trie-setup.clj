(ns user
  (:require [chem.core]
            [chem.setup]
            [chem.process]
            [chem.ctx-utils]
            [chem.evaluation :as eval]
            [chem.stacking]
            [chem.stacking-prep :as stacking-prep]
            [chem.meta-classify :as meta-classify]

)
  (:use [chem.utils]))

;; Load ChemDNER Training and Development sets
;;
(load-file "scripts/setupscript.clj")

(meta-classify/reinit-trie)

;; annotate record using trie-based annotation
(def trie-ner-annotated-records (map meta-classify/ctx-utils-annotate-record user/training-records))

(def trie-ner-chemdner-resultlist
  (apply concat
         (map #(eval/docid-termlist-to-chemdner-result
                (:docid %)
                (eval/get-annotation-termlist % :trie-ner))
              trie-ner-annotated-records)))

(eval/write-chemdner-resultlist-to-file "trie-ner-chemdner-dev-resultlist.txt"
                                        trie-ner-chemdner-resultlist)

(def trie-ner-meta-data (chem.stacking-prep/convert-chemdner-result-file-to-meta-data-list
                          "trie-ner-chemdner-dev-resultlist.txt" ))
(chem.stacking-prep/write-data "trie-ner-meta-data.txt" trie-ner-meta-data)
