(ns chem.stacking-prep
  (:require [chem.metamap-annotation :as mm-annot]))

(defn average-probabilities [table]
  (map #(list (first %) (/ (apply + (second %)) (count (second %))))
       (reduce (fn [newmap row]
                 (let [text (row :text)]
                   (if (contains? newmap text)
                     (assoc newmap text (conj (newmap text) (row :probability)))
                     (assoc newmap text [(row :probability)]))))
               {} table)))


;;    |                 :text |             :score |
;;    |-----------------------+--------------------|
;;    |            Prilocaine | 0.9893952049531791 |
;;    |             lidocaine | 0.9943492305955366 |
;;    |           o-toluidine | 0.6041724373525079 |
;;    |          2,6-xylidine | 0.7216370759707509 |
;;    |            prilocaine | 0.9825976688239773 |
;;    |             lidocaine | 0.9888295719056682 |
;;    |                     ( | 0.3768532754815091 |
;;    |            prilocaine | 0.8582749355783896 |
;;    |            prilocaine | 0.9880186600065767 |
;;    |             lidocaine | 0.9848379716580961 |
;;    |            prilocaine | 0.9761222779398154 |
;;    |             lidocaine | 0.9865878991449606 |
;;    |           o-toluidine | 0.7932370063683195 |
;;    |          2,6-xylidine | 0.8370963224598511 |
;;    |            prilocaine |  0.983757845634979 |
;;    |             lidocaine |  0.988064270971522 |
;;    |       fluorophosphate | 0.9843804326851202 |
;;    |                   bis | 0.7365517864383962 |
;;    |                     - | 0.7360852859963697 |
;;    |         4-nitrophenyl | 0.8712066870670204 |
;;    |             diisoprop | 0.9666176337629392 |
;;    |                     ) | 0.4062887714685398 |
;;    |            prilocaine | 0.9881519519414016 |
;;    |             lidocaine | 0.9871179601018523 |
;;    |             phosphate | 0.8605096730076071 |
;;    |                     ( | 0.7476580770568985 |
;;    |           o-toluidine |  0.838467715990138 |
;;    |          2,6-xylidine | 0.8746670464805117 |
;;    |           o-Toluidine | 0.9303948061587595 |
;;    |          2,6-xylidine | 0.9580453018501668 |
;;    | 6-hydroxy-o-toluidine |  0.934296451040447 |
;;    |              xylidine | 0.5456446468203113 |
;;    |            prilocaine | 0.9702697954953339 |

(defn gen-labels-with-probability-with-extents [docid stanford-ner-table]
  "Convert Stanford NER tables to Stacking format with labels
   containing term$docid$start$end and score."
  (map (fn [row]
         (let [label (format "%s$%s$%s$%s" (row :text) docid (row :start) (row :end))]
           (list label (row :probability))))
       stanford-ner-table))

(defn gen-labels-with-probability-with-docid [docid stanford-ner-table]
  "Convert Stanford NER tables to Stacking format with labels
   containing term$docid$start$end and score."
  (map (fn [row]
         (let [label (format "%s$%s" (row :text) docid)]
           (list label (row :probability))))
       stanford-ner-table))

(defn gen-labels-with-probability [docid stanford-ner-table]
  "Convert Stanford NER tables to Stacking format with labels
   containing term$docid$start$end and score."
  (map (fn [row]
         (list docid (row :text) (row :probability)))
       stanford-ner-table))

(defn gen-labels-with-probability-for-record [record enginekywd]
  (concat
   (gen-labels-with-probability (:docid record) (-> record enginekywd :title-result))
   (gen-labels-with-probability (:docid record) (-> record enginekywd :abstract-result))))

(defn gen-stanford-ner-meta-data [annotated-recordlist enginekywd]
  "convert annotations into format that can be used by Dina's Meta Stacking program"
  (vec 
   (apply concat
          (vec 
           (map (fn [record]
                  (concat
                   (gen-labels-with-probability (:docid record) (-> record enginekywd :title-result))
                   (gen-labels-with-probability (:docid record) (-> record enginekywd :abstract-result))))
                annotated-recordlist)))))

(defn write-data [outfilename meta-data]
  ^{:doc "write meta data for stacking program to file."}
   (with-open [w (java.io.FileWriter. outfilename)]   
     (dorun
      (map #(.write w (format "%s\t%f\n" (first %) (second %))) meta-data))))

(defn read-data [infilename]
  (map #(clojure.string/split % #"\t")
       (chem.utils/line-seq-from-file infilename)))

(defn mm-evlist-to-labels-with-scores [docid resultlist input-text]
  "Convert metamap evlist to labels scores"
  (map (fn [el]
         (let [start (:start (first (el :position)))
               end  (+ start (:length (first (el :position))))
               label (format "%s$%s$%s$%s" (subs input-text start end) docid start end)]
           (list label (* (Math/abs (el :score)) 1.0))))
       (mm-annot/keep-ev-elements resultlist)))

(defn chemdner-resultlist-to-meta-data-with-docid [chemdner-resultlist]
  "Convert CHEMDNER resultlist file into the format needed by the Meta
   Stacking program."
  (map (fn [el]
         (let [docid (nth el 0)
               term (nth el 1)
               score (nth el 3)]
           (list docid term score)))
       chemdner-resultlist))

(defn chemdner-resultlist-to-meta-data [chemdner-resultlist]
  "Convert CHEMDNER resultlist file into the format needed by the Meta
   Stacking program."
  (map (fn [el]
         (let [docid (if (integer? (nth el 0))
                       (nth el 0)
                       (read-string (nth el 0)))
               term (nth el 1)
               score (if (number? (nth el 3))
                       (nth el 3)
                       (read-string (nth el 3)))]
           (vec (list docid term score))))
       chemdner-resultlist))

(defn gen-qrels-with-docid [gold-standard-pair-list]
  "Given gold standard for NER, generate qrels table for Meta Stacking
   program."
  (map (fn [pair]
         (list (format "%s$%s" (:term pair) (:docid pair)) 1.0))
       gold-standard-pair-list))

(defn gen-qrels [gold-standard-set]
  "Given gold standard for NER, generate qrels table for Meta Stacking
   program."
  (map (fn [term]
         (list (format "%s" term) 1.0))
       gold-standard-set))

(defn gen-gold-standard-set [cdi-gold-list]
  (set (map #(:term %) cdi-gold-list)))
  
(defn convert-chemdner-result-file-to-meta-data-file [result-filename meta-data-filename]
  (let [resultlist (map #(clojure.string/split % #"\t")
                        (chem.utils/line-seq-from-file result-filename))
        meta-data (map #(format "%s\t%s" (first %) (second %)) 
                               (chem.stacking-prep/chemdner-resultlist-to-meta-data resultlist))]
    (chem.utils/write-elements meta-data-filename meta-data)))
        
(defn convert-chemdner-result-file-to-meta-data-list [result-filename]
  (let [resultlist (map #(clojure.string/split % #"\t")
                        (chem.utils/line-seq-from-file result-filename))]
    (chem.stacking-prep/chemdner-resultlist-to-meta-data resultlist)))
        
