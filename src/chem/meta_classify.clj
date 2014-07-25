(ns chem.meta-classify
  (:import (edu.stanford.nlp.ie.crf CRFClassifier))
  (:import (edu.stanford.nlp.ling CoreAnnotations))
  (:import (gov.nih.nlm.nls.ner.stanford ListProb))
  (:require [clojure.set]
            [chem.core]
            [chem.setup]
            [chem.annotations]
            [chem.mallet]
            [chem.stanford-ner :as stanford-ner]
            [chem.evaluation :as eval]
            [chem.stacking-prep :as stacking-prep]
            [chem.stacking :as stacking]
            [chem.ctx-utils :as ctx-utils]
            [chem.mti-filtering :as mti-filtering])
  (:use [chem.utils])
  (:import (stacking RunTest RP MLRstackClassifier MLRstackTrainer)
           (java.util HashMap)))

;; Initialization

(defn gen-record-map [records] 
  (into {} (map #(vec (list (:docid %) %)) records)))

(defonce ^:dynamic *crf-ner-classifier* 
  (CRFClassifier/getClassifier "ner-model.dev.training.ser.gz"))


(defonce chemdner-training-data        (chem.setup/load-chemdner-training-data))
(defonce chemdner-gold-map             (chemdner-training-data :chemdner-training-cdi-gold))
(defonce chemdner-training-cdi-gold    (chemdner-training-data :chemdner-training-cdi-gold))
(defonce chemdner-development-cdi-gold (chemdner-training-data :chemdner-development-cdi-gold))

(defonce chemdner-training-cdi-gold-map
  (mti-filtering/gen-training-annotations-map chemdner-training-cdi-gold))
(defonce chemdner-development-cdi-gold-map
  (mti-filtering/gen-training-annotations-map chemdner-development-cdi-gold))

(defonce training-records
   (map #(chem.process/add-chemdner-gold-annotations chemdner-training-cdi-gold-map %)
        (chemdner-training-data :training-records)))

(defonce training-record-map (gen-record-map training-records))

(defonce test-records (chemdner-training-data :test-records))

(defonce test-record-map (gen-record-map test-records))

(defonce development-records
   (map #(chem.process/add-chemdner-gold-annotations chemdner-development-cdi-gold-map %)
        (chemdner-training-data :development-records)))


;; load chemdner gold standard terms from all training documents
(defonce chemdner-gold-term-list
  (clojure.set/union
   (ctx-utils/gen-termset-from-chemdner-gold-standard training-records)
   (ctx-utils/gen-termset-from-chemdner-gold-standard development-records)))

;; load normalized chemical record list
(defonce normchem-record-list (line-seq-from-file "normchemdb.dat"))
(defonce normchem-term-list (map #(first (clojure.string/split % #"\t"))
                                   normchem-record-list))
(defonce chemdner-terms-not-in-mesh
  (clojure.set/difference (set chemdner-gold-term-list) (set normchem-term-list)))

(defonce ^:dynamic *chem-trie*
  (ctx-utils/new-term-trie normchem-record-list chemdner-terms-not-in-mesh))

(defn reinit-trie []
  (def ^:dynamic *chem-trie*
    (ctx-utils/new-term-trie normchem-record-list chemdner-terms-not-in-mesh)))

(defn init []
  (chem.mongodb/init))

(chem.mongodb/init)

;; base classifier functions

(defn crf-ner-classify-record
  "Classify record using Stanford NER classifier trained for chemicals."
 [record] 
  (stanford-ner/classify-record *crf-ner-classifier* record))

(defn tag-record
  "tag chemicals using Dina's hash-trie matcher"
  [record]
  (vec
   (set
    (map #(vec (list (:text %) 1.0))
         (let [trie-ner-result 
               (:trie-ner
                (ctx-utils/annotate-record :trie-ner *chem-trie* record))]
           (concat (-> trie-ner-result :title-result :annotations)
                   (-> trie-ner-result :abstract-result :annotations)))))))


(defn gen-base-metadata-classifier-map
  [record]
  (hash-map :enchilada0   (into {} (stacking/enchilada0-classify-record record))
            :trie-ner     (into {} (tag-record record))
            :stanford-ner (into {} (crf-ner-classify-record record))))

(defn meta-classify
  "Send record-map containing elements :title and :abstract to base
  classifiers and then send results of base classifiers to
  meta-classifier."
 [l-weights ordered-classifier-key-list record]
  (stacking/classify
   (double-array l-weights) 
   (stacking/meta-data-map-list-for-record (gen-base-metadata-classifier-map record)
                                                ordered-classifier-key-list)))

(defn get-base-classifier-annotated-records
  [record]
  (hash-map :enchilada0   (stacking/enchilada0-annotate-record record)
            :trie-ner     (ctx-utils/annotate-record :trie-ner *chem-trie* record)
            :stanford-ner (stanford-ner/annotate-record *crf-ner-classifier* record)))

(defn gen-metadata-classifier-map-from-annotated-records
  [annotated-record-map]
  (hash-map :enchilada0   (into {} (stacking/make-pairs (annotated-record-map :enchilada0)))
            :trie-ner     (into {} (ctx-utils/make-pairs (annotated-record-map :trie-ner)))
            :stanford-ner (into {} (stanford-ner/make-pairs (annotated-record-map :stanford-ner)))))

(defn keep-valid-annotations
  [m-predict-map annotated-record-map]
  (let [termset (set (map first m-predict-map))
        engines [:enchilada0 :trie-ner :stanford-ner]
        annotations (concat 
                     (-> annotated-record-map :enchilada0 :enchilada0 :abstract-result :annotations)
                     (-> annotated-record-map :trie-ner :trie-ner :abstract-result :annotations) 
                     (-> annotated-record-map :stanford-ner :stanford-ner :abstract-result :annotations))]
    ;; traverse annotations keeping valid ones
    (map #(dissoc % :span)              ; remove span elements from annotations (keep :spans elements)
         (vals
          (reduce (fn [newmap annotation]
                    (let [term (annotation :text)]
                      (if (termset term)
                        (if (contains? newmap term)
                          (assoc newmap term 
                                 (conj (newmap term)
                                       annotation 
                                       (hash-map :meta-classifier-predict
                                                 (m-predict-map (annotation :text))
                                                 :spans 
                                                 (clojure.set/union (:spans (newmap term))
                                                                    (if (not (nil? (annotation :span)))
                                                                      (set (list (annotation :span)))
                                                                      #{})))))  ; merge existing annotations
                          (assoc newmap term 
                                 (conj annotation (hash-map :spans (if (not (nil? (annotation :span)))
                                                                     (set (list (annotation :span)))
                                                                     #{}))))) ; add new annotation 
                        newmap)))
                  {} annotations)))))
  
(defn meta-classify-with-annotations
  "Send record-map containing elements :title and :abstract to base
  classifiers and keep annotations.  Convert annotations to vectors
  and send base classifier vectors to meta-classifier.  Then reconcile
  meta-classifier vector with base-clasifier annotations."
  [l-weights ordered-classifier-key-list record]
  (let [annotated-record-map (get-base-classifier-annotated-records record)
        meta-classifier-result (stacking/classify (double-array l-weights) 
                                                  (stacking/meta-data-map-list-for-record 
                                                   (gen-metadata-classifier-map-from-annotated-records
                                                    annotated-record-map)
                                 ordered-classifier-key-list))]
    (conj (select-keys record [:docid :title :abstract])
          (hash-map
          :meta-classifier-annotations
          (keep-valid-annotations (meta-classifier-result :m-predict-map)
                                           annotated-record-map)))))

(defn meta-classify-records [l-weights ordered-classifier-key-list recordlist]
  (map (fn [record]
         (meta-classify-with-annotations l-weights ordered-classifier-key-list record))
       recordlist))

(defn write-classified-record-to-json-file [record]
  (write-json-to-file (format "data/results/mc%s.json" (record :docid)) record))

(defn write-classified-records-to-jsonfiles [classified-recordlist]
  (dorun 
   (map 
    write-classified-record-to-json-file
    classified-recordlist)))

(defn write-classified-record-to-ednfile [record]
  (pr-object-to-file (format "data/results/mc%s.edn" (record :docid)) record))

(defn write-classified-records-to-ednfiles [classified-recordlist]
  (dorun 
   (map 
    write-classified-record-to-ednfile
    classified-recordlist)))
