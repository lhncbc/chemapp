(ns user
  (:import (edu.stanford.nlp.ie.crf CRFClassifier))
  (:import (edu.stanford.nlp.ling CoreAnnotations))
  (:import (gov.nih.nlm.nls.ner.stanford ListProb))
  (:require [chem.core])
  (:require [chem.setup])
  (:require [chem.annotations])
  (:require [chem.mallet])
  (:require [chem.stanford-ner :as stanford-ner])
  (:require [chem.evaluation :as eval])
  (:require [chem.stacking-prep :as stacking-prep])
  (:require [chem.stacking :as stacking])
  (:use [chem.utils]))

(load-file "scripts/setupscript.clj")

;; load classifier trained using training records
(def classifier (CRFClassifier/getClassifier "ner-model.training.ser.gz"))


;; annotate development records using classifier
;; (def stanford-ner-records (map
;;                            #(chem.stanford-ner/annotate-record classifier %) 
;;                            development-records))

;; (def stanford-ner-chemdner-resultlist
;;   (apply concat
;;          (map #(eval/docid-termlist-to-chemdner-result
;;                 (:docid %)
;;                 (eval/get-annotation-termlist % :stanford-ner))
;;               stanford-ner-records)))

;; (eval/write-chemdner-resultlist-to-file "stanford-ner-chemdner-dev-resultlist.txt"
;;                                         stanford-ner-chemdner-resultlist)

;; Experiments
;;
;; (def sentence-list (.classify classifier (:abstract (nth training-records 10))))
;; (def prob-map (read-string (pr-str (ListProb/getProbsDocument classifier (nth sentence-list 0)))))
;; (print-table (map #(conj (second %) (hash-map :term (first %))) prob-map))
;;
;; (def prob-map (chem.stanford-ner/gen-prob-map classifier (nth sentence-list 0)))
;; (print-table (chem.stanford-ner/java-prob-map-to-table prob-map))

;; See function java-prob-map-to-table in chem/stanford-ner.clj
;; (dorun (map #(print-table (chem.stanford-ner/prob-map-to-table (chem.stanford-ner/gen-prob-map classifier %))) sentence-list))
;; end of Experiments


;; Filtering - keeping terms with chemical weights greater than theshold:  
;; 
;; (print-table (filter #(> (% "CHEMICAL") 0.7) (chem.stanford-ner/prob-map-to-table prob-map)))

;; (dorun (map 
;;         #(print-table (filter 
;;                        (fn [tuplemap] (> (tuplemap "CHEMICAL") 0.5)) 
;;                        (chem.stanford-ner/prob-map-to-table (chem.stanford-ner/gen-prob-map classifier %))))
;;         sentence-list))


(def dev-classifier (CRFClassifier/getClassifier "ner-model.dev.training.ser.gz"))
(def dev-stanford-ner-records (map
                           #(chem.stanford-ner/annotate-record dev-classifier %) 
                           training-records))
(def dev-stanford-ner-chemdner-training-resultlist
  (apply concat
         (map #(eval/docid-termlist-to-chemdner-result
                (:docid %)
                (eval/get-annotation-termlist % :stanford-ner))
              dev-stanford-ner-records)))
(eval/write-chemdner-resultlist-to-file "dev-stanford-ner-chemdner-training-resultlist.txt"
                                        dev-stanford-ner-chemdner-training-resultlist)

;; stacking 
(def stanford-ner-meta-data (chem.stacking-prep/gen-stanford-ner-meta-data dev-stanford-ner-records :stanford-ner))
(chem.stacking-prep/write-data "stanford-ner-meta-data.txt" stanford-ner-meta-data)


;; Filtering - keeping terms with chemical weights greater than theshold:  
;; 
;; (print-table (filter #(> (% "CHEMICAL") 0.7) (chem.stanford-ner/prob-map-to-table prob-map)))

;; (dorun (map 
;;         #(print-table (filter 
;;                        (fn [tuplemap] (> (tuplemap "CHEMICAL") 0.5)) 
;;                        (chem.stanford-ner/prob-map-to-table (chem.stanford-ner/gen-prob-map classifier %))))
;;         sentence-list))


;;
;; 
(def stanford-ner-meta-data (chem.stacking-prep/gen-stanford-ner-meta-data dev-stanford-ner-records :stanford-ner))
(def stanford-ner-document-meta-data (chem.stacking/collect-document-pairs stanford-ner-meta-data))

(def enchilada0-meta-data (chem.stacking-prep/convert-chemdner-result-file-to-meta-data-list
                           "enchilada0-chemdner-resultlist.txt"))
(def enchilada0-document-meta-data (chem.stacking/collect-document-pairs enchilada0-meta-data))

(def subsume-chemdner-resultlist (map #(clojure.string/split % #"\t")
                                      (chem.utils/line-seq-from-file "subsume-chemdner-resultlist.txt")))
(def subsume-meta-data (chem.stacking-prep/chemdner-resultlist-to-meta-data subsume-chemdner-resultlist))
(def subsume-document-meta-data (chem.stacking/collect-document-pairs subsume-meta-data))

(def trie-ner-meta-data (chem.stacking-prep/convert-chemdner-result-file-to-meta-data-list 
                         "trie-ner-chemdner-resultlist.txt"))
(def trie-ner-document-meta-data (chem.stacking/collect-document-pairs trie-ner-meta-data))

(def metadata-classifier-map (hash-map :enchilda0 enchilada0-document-meta-data 
                                       :subsume subsume-document-meta-data 
                                       :stanford-ner stanford-ner-document-meta-data))

(def ordered-classifier-key-list [:stanford-ner :enchilda0 :subsume])

;; these must be updated if meta-classifier is re-trained.
(def l-weights [0.6233059514921795 0.011315388338504123 0.5823637709594908]))

;; sample run of meta-classifier
(chem.stacking/classify
 (double-array l-weights) 
 (chem.stacking/meta-data-map-list-for-docid (:docid (first training-records))
                                             metadata-classifier-map
                                             ordered-classifier-key-list))

(def document-classification-map
  (chem.stacking/meta-classify training-records metadata-classifier-map 
                               ordered-classifier-key-list l-weights))

(def metaclassifier-chemdner-list (chem.stacking/document-classification-map-to-chemdner-list
                                   training-records document-classification-map))
(write-elements "metaclassifier-chemdner-list.txt" (map #(clojure.string/join "\t" %)
                                                        metaclassifier-chemdner-list))


(def stanford-ner-chemdner-list (chem.stacking/document-meta-data-map-to-chemdner-list training-records stanford-ner-document-meta-data))

(write-elements "stanford-ner-chemdner-list.txt" (map #(clojure.string/join "\t" %) stanford-ner-chemdner-list))



