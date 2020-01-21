(ns chem.meta-classify
  (:import (edu.stanford.nlp.ie.crf CRFClassifier))
  (:import (edu.stanford.nlp.ling CoreAnnotations))
  (:import (gov.nih.nlm.nls.ner.stanford ListProb))
  (:require [clojure.set]
            [chem.setup]
            [chem.annotations]
            [chem.stanford-ner :as stanford-ner]
            [chem.evaluation :as eval]
            [chem.stacking-prep :as stacking-prep]
            [chem.stacking :as stacking]
            [chem.chemdner-tools :as chemdner-tools]
            [chem.token-trie :as token-trie]
            [chem.token-trie-ner :as token-trie-ner])
  (:use [chem.utils])
  (:import (stacking RunTest RP MLRstackClassifier MLRstackTrainer)
           (java.util HashMap)))

;; Initialization

(defn gen-record-map [records] 
  (into {} (map #(vec (list (:docid %) %)) records)))

(defonce ^:dynamic *crf-ner-classifier* nil)

(defonce chemdner-data        (chem.setup/load-chemdner-data))
(defonce chemdner-gold-map             (chemdner-data :chemdner-training-cdi-gold))
(defonce chemdner-training-cdi-gold    (chemdner-data :chemdner-training-cdi-gold))
(defonce chemdner-development-cdi-gold (chemdner-data :chemdner-development-cdi-gold))

(defonce chemdner-training-cdi-gold-map
  (chemdner-tools/gen-training-annotations-map chemdner-training-cdi-gold))
(defonce chemdner-development-cdi-gold-map
  (chemdner-tools/gen-training-annotations-map chemdner-development-cdi-gold))

(defonce training-records
   (map #(chemdner-tools/add-chemdner-gold-annotations chemdner-training-cdi-gold-map %)
        (chemdner-data :training-records)))

(defonce training-record-map (gen-record-map training-records))

(defonce test-records (chemdner-data :test-records))

(defonce test-record-map (gen-record-map test-records))

(defonce development-records
   (map #(chemdner-tools/add-chemdner-gold-annotations chemdner-development-cdi-gold-map %)
        (chemdner-data :development-records)))


;; load chemdner gold standard terms from all training documents
(defonce chemdner-gold-term-list
  (clojure.set/union
   (chemdner-tools/gen-termset-from-chemdner-gold-standard training-records)
   (chemdner-tools/gen-termset-from-chemdner-gold-standard development-records)))

;; load normalized chemical record list
;; (def normchemfn "normchemdb.dat")
(def normchemfn "/net/lhcdevfiler/vol/cgsb5/ind/II_Group_WorkArea/wjrogers/Projects/chem/pug/diy/normchem2017db.dump")
(defonce normchem-record-list (line-seq-from-file normchemfn))

(defonce normchem-recordmap-list (map (fn [record]
                                        (let [fields (clojure.string/split record #"\t")]
                                          (hash-map :text (nth fields 0)
                                                    :meshid (nth fields 1)
                                                    :cid (nth fields 2)
                                                    :smiles (nth fields 3))))
                                      normchem-record-list))
(defonce normchem-term-list (map #(first (clojure.string/split % #"\t"))
                                   normchem-record-list))


(defonce chemdner-terms-not-in-mesh
  (clojure.set/difference (set chemdner-gold-term-list) (set normchem-term-list)))

(defonce ^:dynamic *chem-trie*
  (token-trie-ner/new-chem-trie normchem-recordmap-list chemdner-terms-not-in-mesh))

(defn reinit-trie []
  (def ^:dynamic *chem-trie*
    (token-trie-ner/new-chem-trie normchem-record-list chemdner-terms-not-in-mesh)))

(defn init []
  ;; (chem.mongodb/remote-init)
  (def *crf-ner-classifier* 
  (CRFClassifier/getClassifier "ner-model.dev.training.ser.gz"))
)

;; (chem.mongodb/remote-init)

;; base classifier functions

(defn stanford-ner-classify-record
  "Classify record using Stanford NER classifier trained for chemicals."
 [record] 
  (stanford-ner/classify-record *crf-ner-classifier* record))

(defn token-trie-ner-tag-record
  "tag chemicals using Dina's trie matcher"
  [record]
  (vec
   (set
    (map #(vec (list (:text %) 1.0))
         (let [trie-ner-result 
               (:trie-ner
                (token-trie-ner/annotate-record :trie-ner *chem-trie* record))]
           (concat (-> trie-ner-result :title-result :annotations)
                   (-> trie-ner-result :abstract-result :annotations)))))))


;; wrappers for token-trie-ner and stanford-ner annotation functions

(defn token-trie-ner-annotate-record [record]
  (token-trie-ner/annotate-record :trie-ner *chem-trie* record))

(defn stanford-ner-annotate-record [record]
  (stanford-ner/annotate-record *crf-ner-classifier* record))

;; meta-classifier functions

(defn gen-base-metadata-classifier-map
  ([record]
     (hash-map :enchilada0   (into {} (stacking/enchilada0-classify-record record))
               :trie-ner     (into {} (token-trie-ner-tag-record record))
               :stanford-ner (into {} (stanford-ner-classify-record record))))
  ([record ordered-classifier-key-list classify-func-list]
     (reduce (fn [newmap [classifier-keyword classify-func]]
               (assoc newmap classifier-keyword (into {} (classify-func record))))
             {} (map #(list %1 %2) ordered-classifier-key-list classify-func-list))))

(defn meta-classify
  "Send record-map containing elements :title and :abstract to base
  classifiers and then send results of base classifiers to
  meta-classifier."
  ([l-weights ordered-classifier-key-list record]
     (stacking/classify
      (double-array l-weights) 
      (stacking/meta-data-map-list-for-record
       (gen-base-metadata-classifier-map record)
       ordered-classifier-key-list)))
  ([l-weights ordered-classifier-key-list classify-func-list record]
     (stacking/classify
      (double-array l-weights) 
      (stacking/meta-data-map-list-for-record
       (gen-base-metadata-classifier-map record ordered-classifier-key-list classify-func-list)
       ordered-classifier-key-list))))

(defn get-base-classifier-annotated-records-v1
  ([record]
     (get-base-classifier-annotated-records-v1 record 
                                            [:enchilada0 :trie-ner :stanford-ner]
                                            [stacking/enchilada0-annotate-record
                                             token-trie-ner-annotate-record
                                             stanford-ner-annotate-record]))
  ([record ordered-classifier-key-list ordered-annotate-record-func-list]
     (reduce (fn [newmap [classifier-keyword annotate-record-func]]
                 (assoc newmap classifier-keyword (annotate-record-func record)))
             {} (map #(list %1 %2) ordered-classifier-key-list ordered-annotate-record-func-list))))

(defn get-base-classifier-annotated-records
  ([record]
     (get-base-classifier-annotated-records record 
                                            [:enchilada0 :trie-ner :stanford-ner]
                                            [stacking/enchilada0-annotate-record
                                             token-trie-ner-annotate-record
                                             stanford-ner-annotate-record]))
  ([record ordered-classifier-key-list ordered-annotate-record-func-list]
     (reduce (fn [newmap [classifier-keyword annotated-record]]
               (assoc newmap classifier-keyword annotated-record))
             {} (map (fn [classifier-keyword annotate-record-func]
                        (vec (list classifier-keyword (annotate-record-func record))))
                      ordered-classifier-key-list ordered-annotate-record-func-list))))


(defn gen-metadata-classifier-map-from-annotated-records
  ([annotated-record-map]
     (hash-map :enchilada0   (into {} (stacking/make-pairs (annotated-record-map :enchilada0)))
               :trie-ner     (into {} (token-trie-ner/make-pairs (annotated-record-map :trie-ner)))
               :stanford-ner (into {} (stanford-ner/make-pairs (annotated-record-map :stanford-ner)))))
  ([annotated-record-map ordered-classifier-key-list ordered-make-pairs-func-list]
     (reduce (fn [newmap pair]
               (let [[classifier-keyword make-pairs-func] pair]
                 (assoc newmap classifier-keyword (into {} (make-pairs-func (annotated-record-map classifier-keyword))))))
             {} (map #(list %1 %2) ordered-classifier-key-list ordered-make-pairs-func-list))))
  
(defn keep-valid-annotations
  [m-predict-map ordered-classifier-key-list annotated-record-map]
  (let [termset (set (map first m-predict-map))
        engines ordered-classifier-key-list
        annotations (flatten (concat 
                              (pmap #(-> annotated-record-map % % :abstract-result :annotations)
                                   engines)
                              (pmap #(-> annotated-record-map % % :title-result :annotations)
                                   engines)))]
    ;; traverse annotations keeping valid ones
    (pmap #(dissoc % :span)              ; remove span elements from annotations (keep :spans elements)
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
  ([l-weights ordered-classifier-key-list ordered-annotate-record-func-list ordered-make-pairs-func-list record]
     (let [annotated-record-map (get-base-classifier-annotated-records record ordered-classifier-key-list 
                                                                       ordered-annotate-record-func-list)
           meta-classifier-result (stacking/classify (double-array l-weights) 
                                                     (stacking/meta-data-map-list-for-record 
                                                      (gen-metadata-classifier-map-from-annotated-records
                                                       annotated-record-map 
                                                       ordered-classifier-key-list ordered-make-pairs-func-list)
                                                      ordered-classifier-key-list))]
       (conj (select-keys record [:docid :title :abstract])
             (hash-map
              :meta-classifier-annotations
              (keep-valid-annotations (meta-classifier-result :m-predict-map) ordered-classifier-key-list
                                      annotated-record-map))))))
   

(defn meta-classify-records [l-weights ordered-classifier-key-list
                             ordered-annotate-record-func-list
                             ordered-make-pairs-func-list recordlist]
  (map (fn [record]
         (meta-classify-with-annotations l-weights ordered-classifier-key-list
                                         ordered-annotate-record-func-list
                                         ordered-make-pairs-func-list record))
       recordlist))

(defn meta-classifier-annotations-to-chemdner-result [annotated-record]
  (map-indexed #(vec (list (:docid annotated-record) (:text %2) (inc %1) 1.0))
               (:meta-classifier-annotations annotated-record)))

(defn write-classified-record-to-json-file [record]
  (write-json-to-file (format "data/results/mc%s.json" (record :docid)) record))

(defn write-classified-records-to-jsonfiles [classified-recordlist]
  (dorun 
   (pmap 
    write-classified-record-to-json-file
    classified-recordlist)))

(defn write-classified-record-to-ednfile [record]
  (pr-object-to-file (format "data/results/mc%s.edn" (record :docid)) record))

(defn write-classified-records-to-ednfiles [classified-recordlist]
  (dorun 
   (pmap 
    write-classified-record-to-ednfile
    classified-recordlist)))

;; Example of using Meta-classifier:
;;
;; user> (require '[chem.meta-classify :as meta-classify])
;; user> (require '[chem.stacking :as stacking])
;; user> (require '[chem.partial :as partial])
;; user> (def l-weights (chem.utils/read-from-file-with-trusted-contents "l-weights.edn"))
;; user> l-weights
;; [0.16856062873907118 0.9314245909833611 0.44243178592978527 0.024734636996052328]

;; user> (def ordered-classifier-key-list
;;   (chem.utils/read-from-file-with-trusted-contents "ordered-classifier-key-list.edn"))
;; user> ordered-classifier-key-list
;; [:trie-ner :stanford-ner :partial :enchilada0]
;; user> (def ordered-annotate-func-list
;;        [meta-classify/token-trie-ner-annotate-record meta-classify/stanford-ner-annotate-record
;;         partial/partial-annotate-record stacking/enchilada0-annotate-record] )
;; user>
;; user> (def ordered-make-pairs-func-list
;;         [token-trie-ner/make-pairs stanford-ner/make-pairs partial/make-pairs stacking/make-pairs])
;; user> (meta-classify-with-annotations l-weights ordered-classifier-key-list
;;                                 ordered-annotate-func-list
;;                                 ordered-make-pairs-func-list record)
;;
;;  


;;   user> (defonce ordered-classify-func-list [meta-classify/token-trie-ner-tag-record
;;						     meta-classify/stanford-ner-classify-record
;; 						     partial/partial-classify-record
;; 						     chem.stacking/enchilada0-classify-record])
;;   #'user/ordered-classify-func-list
;;   user> (def mc-result (meta-classify/meta-classify l-weights
;;            ordered-classifier-key-list ordered-classify-func-list (user/training-record-map 23645248)))
;;   #'user/mc-result
;;   user> mc-result
;;   {:m-predict-map
;;    {"iodide" 1.087384219477041,
;;     "oestrogen" 1.0597240124797604,
;;     "oestradiol" 0.12538217105443572,
;;     "17beta-oestradiol" 1.0791314486328285,
;;     "sodium iodide" 0.12538217105443572,
;;     "sodium" 1.5153182946396677},
;;   :l-weights
;;    [0.12538217105443572
;;     0.9587276559273739
;;     0.46979711904955357
;;     0.02238078795265458]}
;;   user> 
