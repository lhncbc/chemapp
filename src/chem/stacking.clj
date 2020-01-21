(ns chem.stacking
  (:use [clojure.pprint])
  (:require [clojure.edn :as edn] 
            [chem.partial :as partial]
            [chem.annotations :as annot]
            [chem.metamap-annotation :as mm-annot]
            [chem.metamap-tokenization :as mm-tokenization]
            [chem.stanford-ner :as stanford-ner]
            [chem.irutils-normchem :as normchem]
            [chem.opsin]
            [chem.process :as process]
            [chem.ctx-utils :as ctx-utils]
            [chem.utils])
  (:import (stacking RunTest RP MLRstackClassifier MLRstackTrainer)
           (java.util HashMap)))

;;
;; user> (def classifier-instances (chem.stacking/setup-classifiers-instances "ner-model.dev.training.ser.gz" "normchemdb.dat"))
;; #'user/classifier-instances
;; user> (keys classifier-instances)
;; (:ner-classifier :term-trie :mm-api-instance)
;; user> 
;;


(def meta-data-filelist ["data/train/stanford-ner-meta-data.txt"
                         "data/train/enchilada0-meta-data.txt"
                         "data/train/subsume-meta-data.txt"])
(def qrels-filename "data/qrels.txt")

(defn run-test [qrels-filename meta-data-filelist]
  "run trainer and then classifier of list of "
  (let [test-instance (new RunTest)
        mBaseClassifierMaps (map #(.fillMap test-instance % 1) 
                                 meta-data-filelist)
        mQrels (.fillMap test-instance qrels-filename 1)
        trainer (new MLRstackTrainer)
        lWeights (.train trainer mBaseClassifierMaps mQrels)
        mBaseClassifierPredict (map #(.fillMap test-instance % 1) 
                                    meta-data-filelist)
        mPredict (new HashMap)
        classifier (new MLRstackClassifier)]
    (.classify classifier lWeights mBaseClassifierPredict mPredict)
    (hash-map :l-weights (vec lWeights)
              :m-qrels (into {} mQrels)
              :m-predict-map (into {} mPredict)
              :RP (into [] (.getRP (new RP) mPredict mQrels 0.3)))))

(defn train-filelist [qrels-filename meta-data-filelist]
  ^{:doc "train meta classifier, returning map containing training classifier
instance (:trainer), weight of base classifiers (:l-weights), and meta
classifier qrels (:m-qrels)" }
 (let [test-instance (new RunTest)
        mBaseClassifierMaps (map #(.fillMap test-instance % 1) 
                                 meta-data-filelist)
        mQrels (.fillMap test-instance qrels-filename 1)
        trainer (new MLRstackTrainer)
        lWeights (.train trainer mBaseClassifierMaps mQrels)]
   (hash-map :trainer trainer
             :l-weights (vec lWeights)
             :m-qrels (into {} mQrels))))
             
(defn classify-filelist [lWeights meta-data-filelist]
  (let [test-instance (new RunTest)
        mBaseClassifierPredict (map #(.fillMap test-instance % 1) 
                                    meta-data-filelist)
        mPredict (new HashMap)
        classifier (new MLRstackClassifier)]
    (.classify classifier lWeights mBaseClassifierPredict mPredict)
    (hash-map :l-weights (vec lWeights)
              :m-predict-map (into {} mPredict))))

(defn get-recall-precision [m-predict m-qrels]
  (hash-map :RP (vec (.getRP (new RP) m-predict m-qrels 0.3))))

(defn run-test-2 [qrels-filename meta-data-filelist]
  (let [train-result-map (train-filelist qrels-filename meta-data-filelist)
        classify-result-map (classify-filelist (:l-weights train-result-map)
                                               (:m-qrels train-result-map)
                                               meta-data-filelist)
        recall-precision (get-recall-precision (:m-predict-map classify-result-map)
                                               (:m-qrels classify-result-map))]
    (pprint (:l-weights train-result-map))
    (pprint (:m-predict-map train-result-map))
    (pprint (recall-precision))))

(defn fill-map [pairlist]
  (reduce (fn [newmap elem]
            (let [key   (first elem)
                  value (second elem)]
            (if (contains? newmap key)
              (assoc newmap key (/ (+ (newmap key) value) 2)) ; average confidence
              (assoc newmap key value))))
          {} pairlist))


(defn convert-meta-data-to-pairlist
  [meta-data]
  (map 
   (fn [feature]
     (vec (list (nth feature 1) (nth feature 2))))
   meta-data))

(defn meta-data-to-map
  [meta-data]
  (fill-map (convert-meta-data-to-pairlist meta-data)))

(defn gen-meta-data-maplist
 [list-of-pairlists]
  (map (fn [pairlist]
         (fill-map pairlist)
          list-of-pairlists)))

(defn train
  "train meta classifier"
  [qrels-map meta-data-maplist]
   (let [test-instance (new RunTest)
         mBaseClassifierMaps meta-data-maplist
         trainer (new MLRstackTrainer)
         l-weights (.train trainer mBaseClassifierMaps qrels-map)]
        (hash-map :trainer trainer
                  :l-weights (vec l-weights)
                  :m-qrels (into {} qrels-map))))

(defn classify [l-weights meta-data-maplist]
  (let [test-instance (new RunTest)
        mBaseClassifierPredict meta-data-maplist
        mPredict (new HashMap)
        classifier (new MLRstackClassifier)]
    (.classify classifier l-weights mBaseClassifierPredict mPredict)
    (hash-map :l-weights (vec l-weights)
              :m-predict-map (into {} mPredict))))

(defn collect-document-pairs [meta-data] 
  "Collect term/confidence pairs by document"
  (reduce (fn [newmap el]
            (let [key (first el)
                  pair (rest el)]         ;term-confidence pair
              (if (contains? newmap key)
                (assoc newmap key (conj (newmap key) pair))
                (assoc newmap key (vec (list pair))))))
          {} meta-data))


`(defn meta-data-pairs-to-map [meta-data-pairs]
  "From meta data term confidence pairs generate base classifier prediction map."
  (into {} (map (fn [el]
                  (let [key (first el)
                        value (if (string? (second el)) (edn/read-string (second el)) (second el))]
                  (vec (list key value)))) meta-data-pairs)))

(defn meta-data-map-list-for-docid [docid metadata-classifier-map ordered-classifier-key-list]
  "Generate list of base classifier predictions maps from meta data term confidences from base classifiers."
  (map (fn [classifier-key] 
         (let [metadata (metadata-classifier-map classifier-key)]
           (if (contains? metadata docid)
             (meta-data-pairs-to-map (metadata docid))
             {})))
           ordered-classifier-key-list))

(defn meta-data-map-list-for-record [metadata-classifier-map ordered-classifier-key-list]
  "Generate list of base classifier predictions maps from meta data term confidences from base classifiers."
  (map (fn [classifier-key] 
         (metadata-classifier-map classifier-key))
         ordered-classifier-key-list))

(defn meta-classify [recordlist metadata-classifier-map ordered-classifier-key-list l-weights]
  "Get result from each classifier for each document and run through meta-classifier."
  (reduce (fn [newmap record]
            (let [docid (:docid record)]
              (assoc newmap docid (classify (double-array l-weights)
                                            (meta-data-map-list-for-docid docid metadata-classifier-map 
                                                                          ordered-classifier-key-list)))))
          {} recordlist))

(defn roof-weight [weight]
  "if weight is greater than 1.0 then return 1.0 else return weight."
  (if (> weight 1.0) 
    1.0
    weight))

(defn document-classification-map-to-chemdner-list [recordlist document-classification-map]
  "Using term classification by document, generate chemdner result list"
  (apply concat
         (map (fn [record]
                (let [docid (:docid record)]
                  (filter #(and (> (nth % 3) 0.40) (> (count (nth % 1)) 1))
                          (map-indexed (fn [idx pair]
                                 (list docid (first pair) (inc idx) (roof-weight (second pair))))
                               (:m-predict-map (document-classification-map docid))))))
              recordlist)))

(defn document-meta-data-map-to-chemdner-list [recordlist document-meta-data-map]
  (apply concat
         (map (fn [record]
                (let [docid (:docid record)]
                  (filter #(and (> (nth % 3) 0.40) (> (count (nth % 1)) 1))
                          (map-indexed (fn [idx pair]
                                         (list docid (first pair) (inc idx) (second pair)))
                                       (into {} (map #(vec %) (document-meta-data-map docid)))))))
              recordlist)))

(defn filter-mm-annotations [record]
  (conj record 
        (hash-map
         :metamap (hash-map
                   :title-result (hash-map
                                  :annotations (mm-annot/filter-document-annotations
                                                (-> record :metamap :title-result :annotations)
                                                chem.semtypes/chemical-semantic-type-list))
                   :abstract-result (hash-map
                                     :annotations (mm-annot/filter-document-annotations
                                                   (-> record :metamap :abstract-result :annotations)
                                                   chem.semtypes/chemical-semantic-type-list))))))

(defn setup-classifiers-instances
  "load instances for NER classifier, normchem term-trie, and MetaMap server."
  [ner-classifier-serialized-file normchem-file]
  (let [term-trie (ctx-utils/new-trie-hash-table)
        normchem-line-list (chem.utils/line-seq-from-file normchem-file)]
    (ctx-utils/add-mesh-terms-to-trie term-trie normchem-line-list)
    ;; (normchem/init)
    (hash-map :ner-classifier (stanford-ner/load-classifier ner-classifier-serialized-file)
              :term-trie term-trie)))

;; (defn subsume-classify-record [mm-api-instance record]
;;   (into {}
;;         (map #(vec (list % 1.0))
;;              (:flow1-matched-terms
;;               (chem.process/subsume-flow
;;                (chem.process/flow1
;;                 (annot/annotate-record 
;;                  :subsume
;;                  normchem/process-document 
;;                  (chem.process/add-partial-match-annotations 
;;                   (filter-mm-annotations 
;;                    (chem.process/add-metamap-annotations mm-api-instance record))))))))))

(defn concat-enchilada0-title-annotations [annotated-record]
  (concat (-> annotated-record :token-opsin :title-result :annotations)
          (-> annotated-record :partial-opsin :title-result :annotations)
          (-> annotated-record :partial-normchem :title-result :annotations)
          (-> annotated-record :partial :title-result :annotations)))

(defn concat-enchilada0-abstract-annotations [annotated-record]
  (concat (-> annotated-record :token-opsin :abstract-result :annotations)
          (-> annotated-record :partial-opsin :abstract-result :annotations)
          (-> annotated-record :partial-normchem :title-result :annotations)
          (-> annotated-record :partial :abstract-result :annotations)))

(defn concat-enchilada0-annotations [annotated-record]
  (concat 
   (concat-enchilada0-title-annotations annotated-record)
   (concat-enchilada0-abstract-annotations annotated-record)))

(defn enchilada0-annotate-record
  [record]
  (let [partial-record (annot/annotate-record :partial partial/match record)
        token-record   (annot/annotate-record :token mm-tokenization/gen-token-annotations record)
        aggregate-record (process/subsume-flow
                          (conj record
                                (chem.opsin/filter-using-engine-keyword token-record :token)
                                (chem.opsin/filter-using-engine-keyword partial-record :partial)
                                ;; (normchem/filter-partial-match-using-normchem partial-record)
                                token-record
                                partial-record )
                          [:token-opsin :partial-normchem :partial-opsin :partial])]
    (conj aggregate-record
          (hash-map :enchilada0 
                    (hash-map :title-result 
                              (hash-map :annotations (concat-enchilada0-title-annotations aggregate-record))
                              :abstract-result
                              (hash-map :annotations (concat-enchilada0-abstract-annotations aggregate-record)))))))

(defn make-pairs [annotated-record]
  "Make annotations from enchilada0 NER into pairs usable by meta-classifier."
  (vec
   (set
    (pmap #(vec (list (:text %) 1.0))
         (concat
          (-> annotated-record :enchilada0 :title-result :annotations)
          (-> annotated-record :enchilada0 :abstract-result :annotations))))))

(defn enchilada0-classify-record [record]
  "Use token and partial match matchers with Opsin and Normchem
  databases to recognize chemicals."
  (make-pairs (enchilada0-annotate-record record)))

(defn enchilada0-classify-record-v1
  "Annotate record and return terms in dictionary map for meta-classifier"
  [record]
  (into {} 
        (map #(vec (list % 1.0))
             (annot/list-matched-terms :partial-normchem (enchilada0-annotate-record record)))))
                                                                                     
(defn stanford-ner-classify-record [ner-classifier record]
  "Annotate record using NER CRF classifier and return terms in dictionary map for meta-classifier"
  (meta-data-pairs-to-map
   (chem.stanford-ner/make-pairs
    (chem.stanford-ner/annotate-record :stanford-ner ner-classifier record))))

(defn classify-record [ner-classifier l-weights ordered-classifier-key-list record]
  (classify (double-array l-weights)
            (meta-data-map-list-for-record 
             (hash-map :trie-ner ()
                       :stanford-ner (stanford-ner-classify-record ner-classifier record)
                       :enchilada0 (enchilada0-classify-record record))
             ordered-classifier-key-list)))
