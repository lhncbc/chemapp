(ns chem.train-meta-classifier
  (:import (edu.stanford.nlp.ie.crf CRFClassifier))
  (:import (edu.stanford.nlp.ling CoreAnnotations))
  (:import (gov.nih.nlm.nls.ner.stanford ListProb))
  (:require [chem.setup]
            [chem.stacking-prep :as stacking-prep]
            [chem.stacking :as stacking]
            [chem.utils :as utils])
  (:import (stacking RunTest RP MLRstackClassifier MLRstackTrainer)
           (java.util HashMap)))
;;
;; Load ChemDNER Training and Development sets
;;
;;    (load-file "scripts/setupscript.clj")
;;    (require '[chem.evaluation :as eval])
;;
;;    (def partial-annotated-records (map chem.partial/partial-annotate-record user/training-records))
;;
;;    (def partial-chemdner-resultlist
;;      (apply concat
;;             (map #(eval/docid-termlist-to-chemdner-result
;;                    (:docid %)
;;                    (eval/get-annotation-termlist % :partial))
;;                     partial-annotated-records)))
;; 
;;    (eval/write-chemdner-resultlist-to-file "partial-chemdner-dev-resultlist.txt"
;;                                            partial-chemdner-resultlist)
;;
;;    (require '[chem.stacking-prep :as stacking-prep])
;;    (def partial-meta-data (stacking-prep/chemdner-resultlist-to-meta-data partial-chemdner-resultlist))
;;    (chem.stacking-prep/write-data "partial-meta-data.txt" partial-meta-data)
;;

;;    (def stanford-ner-meta-data (chem.stacking-prep/read-data "stanford-ner-meta-data.txt"))
;;    (def enchilada0-meta-data (chem.stacking-prep/read-data "enchilada0-meta-data.txt"))
;;    (def trie-ner-meta-data (chem.stacking-prep/read-data "trie-ner-meta-data.txt"))

;;    (def base-metadata-map
;;      (hash-map :enchilada0 (stacking/meta-data-pairs-to-map enchilada0-meta-data)
;;                :trie-ner (stacking/meta-data-pairs-to-map trie-ner-meta-data)
;;                :stanford-ner (stacking/meta-data-pairs-to-map stanford-ner-meta-data)))

(defn gen-base-metadata-map
  "Generate base metadata map for training"
 [classifier-meta-data-file-map]
  (reduce (fn [base-metadata-map entry]
            (let [[classifier-name meta-data-filename] entry]
              (assoc base-metadata-map classifier-name
                     (stacking/meta-data-pairs-to-map
                      (chem.stacking-prep/read-data meta-data-filename)))))
          {} classifier-meta-data-file-map))

;; (def ordered-classifier-key-list (vec (keys base-metadata-map)))

(defn gen-ordered-classifier-key-list
  "Generate ordered classifier key list from base metadata map"
  [base-metadata-map]
  (vec (keys base-metadata-map)))

;; (def test-instance (new RunTest))

(defn new-test-instance []
  "create new test instance for meta classifier."
  (new RunTest))

;; (def metadata-maplist (vals base-metadata-map))

(defn gen-metadata-maplist
  "generate metadata maplist from base metadata map"
  [base-metadata-map]
  (vals base-metadata-map))

;;
;; qrels was generated from CHEMDNER gold standard from TRAINING SET 
;;
(defonce default-qrels-filename "data/qrels.txt")

;; (defonce qrels-map (.fillMap test-instance qrels-filename 1))

;; Train the meta-classifier
;; (def meta-classifier-result-info-map (stacking/train qrels-map metadata-maplist))

(defn train-meta-classifier
  "Train meta classifier using metadata maplist from base classifiers
  and qrels-map, information structure on the meta classifier is
  returned.  A map is returned containing vector of classifier voting
  weights accessible using the keyword :l-weights.  "
  [qrels-map metadata-maplist]
  (stacking/train qrels-map metadata-maplist))

;; these must be updated if meta-classifier is re-trained.
;; (def l-weights (meta-classifier-result-info-map :l-weights))

(defn get-l-weights [meta-classifier-result-info-map]
  (meta-classifier-result-info-map :l-weights))

(defn save-pertinent-meta-classifier-info [l-weights ordered-classifier-key-list]
  "Save l-weights to l-weights.edn, and ordered-classifier-key-list to
   ordered-classifier-key-list.edn."
  (utils/pr-object-to-file "l-weights.edn" l-weights)
  (utils/pr-object-to-file "ordered-classifier-key-list.edn" ordered-classifier-key-list))

(defn train-meta-classifier-from-meta-data-files
  "Load base classifier meta-data and qrels from files and train meta-classifier.
   Save l-weights to l-weights.edn, and ordered-classifier-key-list to
   ordered-classifier-key-list.edn."
  [classifier-meta-data-file-map qrels-filename]
  (let [base-metadata-map (gen-base-metadata-map classifier-meta-data-file-map)
        ordered-classifier-key-list (gen-ordered-classifier-key-list base-metadata-map)
        test-instance (new RunTest)
        metadata-maplist  (vals base-metadata-map)
        qrels-map (.fillMap test-instance qrels-filename 1)
        meta-classifier-result-info-map (stacking/train qrels-map metadata-maplist)
        l-weights (meta-classifier-result-info-map :l-weights)]
  (save-pertinent-meta-classifier-info l-weights ordered-classifier-key-list)
  {:l-weights l-weights
   :ordered-classifier-key-list ordered-classifier-key-list 
   :meta-classifier-result-info-map meta-classifier-result-info-map}
  ))
        
;; Example of training:
;;
;; (def result (train/train-meta-classifier-from-meta-data-files
;;              {:partial      "partial-meta-data.txt"
;;               :enchilada0   "enchilada0-meta-data.txt"
;;               :stanford-ner "stanford-ner-meta-data.txt"
;;               :trie-ner     "trie-ner-meta-data.txt"}
;;              "data/qrels.txt"))
