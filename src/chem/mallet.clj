(ns chem.mallet
  (:import (java.io FileReader File
                    ObjectOutputStream FileOutputStream
                    ObjectInputStream FileInputStream))
  (:import (cc.mallet.util CommandOption$IntegerArray
                           CommandOption$String))
  (:import (cc.mallet.types Instance InstanceList Sequence))
  (:import (cc.mallet.pipe SimpleTaggerSentence2TokenSequence
                            TokenSequence2FeatureVectorSequence))
  (:import (cc.mallet.fst CRF
                          CRFOptimizableByLabelLikelihood
                          CRFTrainerByL1LabelLikelihood
                          CRFTrainerByLabelLikelihood
                          CRFTrainerByValueGradients
                          CRFWriter
                          MaxLatticeDefault
                          MultiSegmentationEvaluator
                          SimpleTagger
                          SimpleTagger$SimpleTaggerSentence2FeatureVectorSequence
                          TokenAccuracyEvaluator))
  (:import (cc.mallet.pipe.iterator LineGroupIterator))
  (:import (cc.mallet.pipe SerialPipes Pipe))
  (:import (cc.mallet.optimize Optimizable))
  (:import (java.util.regex Pattern))
  (:require [chem.mallet-crfs :as crfs])
  (:require [chem.metamap-tokenization :as mm-tokenize]))

;; code for write features for Mallet Sequence Tagging and Stanford NER

(defn gen-features-and-labels-adhoc [text targetset]
  "Generate features using training text and target sets."
  (filter #(not (nil? %))
          (reduce (fn [newlist item]
                    (if (= (first item) ".")
                      (conj newlist item ["" ""]))
                      (conj newlist item))
                  [] (filter #(not (nil? %))
                             (map (fn [token]
                                    (let [term   (:text token)
                                          cclass (:class token)]
                                      (when (> (count (clojure.string/trim term)) 0)
                                        (if (contains? targetset term)
                                          [term "chemical"]
                                          [term "O"]))))
                                  (mm-tokenize/analyze-text-chemicals-aggressive text))))))

(defn gen-features-and-labels-from-targetset [text targetset]
  "Generate features using training text and target sets."
  (filter 
   #(not (nil? %))
          (reduce 
           (fn [newlist item]
             (if (= (first item) ".")
               (conj newlist item ["" ""]))
             (conj newlist item))
           [] (filter 
               #(not (nil? %))
               (map (fn [token]
                      (let [term   (:text token)
                            cclass (:class token)]
                        (when (> (count (clojure.string/trim term)) 0)
                          (if (contains? targetset term)
                            [term "chemical"]
                            [term "O"]))))
                    (mm-tokenize/analyze-text-chemicals-using-targetset
                     text targetset))))))

(defn gen-token-based-features-and-labels-from-targetset [text targetset]
  "Generate token-based features using training text and target sets."
  (filter 
   #(not (nil? %))
          (reduce 
           (fn [newlist item]
             (if (= (first item) ".")
               (conj newlist item ["" ""]))
             (conj newlist item))
           [] (filter 
               #(not (nil? %))
               (reduce (fn [newlist token]
                         (let [term   (:text token)
                               cclass (:class token)]
                           (if (> (count (clojure.string/trim term)) 0)
                             (if (contains? targetset term)
                               (if (contains? token :text-tokens)
                                 (concat newlist 
                                         (map (fn [text-token]
                                                [text-token "CHEMICAL"])
                                              (:text-tokens token)))
                                 (conj newlist [term "CHEMICAL" ]))
                               (conj newlist [term "O"]))
                             newlist)))
                       [] (mm-tokenize/analyze-text-chemicals-using-targetset
                           text targetset))))))

(def gen-features-and-labels gen-token-based-features-and-labels-from-targetset)

(defn gen-gazette-from-targetset [targetset]
  "Generate gazette features using target sets."
  (vec 
   (map (fn [term]
          (format "CHEMICAL %s" (clojure.string/join " " (mm-tokenize/tokenize term))))
        targetset)))

(defn gen-feature-list-from-record [doc-struct-map]
  (concat
   (gen-features-and-labels
    (:title doc-struct-map)
    (:chemdner-gold-standard doc-struct-map))
   (gen-features-and-labels
    (:abstract doc-struct-map)
    (:chemdner-gold-standard doc-struct-map))))

(defn gen-token-based-feature-list-from-record [doc-struct-map]
  (concat
   (gen-token-based-features-and-labels-from-targetset
    (:title doc-struct-map)
    (:chemdner-gold-standard doc-struct-map))
   (gen-token-based-features-and-labels-from-targetset
    (:abstract doc-struct-map)
    (:chemdner-gold-standard doc-struct-map))))

(defn gen-feature-list-from-doclist [doc-struct-record-list]
  (apply concat
         (map gen-feature-list-from-record
              doc-struct-record-list)))

(defn gen-unlabeled-data-from-text [text]
  (filter #(not (nil? %))
          (reduce (fn [newlist item]
                    (if (= item ".")
                      (conj newlist item ["" ""])
                      (conj newlist item)))
                  [] (map (fn [token]
                            [(:text token) (:class token)])
                          (mm-tokenize/analyze-text-chemicals-aggressive text)))))

(defn gen-unlabeled-data-from-record [doc-struct-map]
  (concat
   (gen-unlabeled-data-from-text
    (:title doc-struct-map))
   (gen-unlabeled-data-from-text
    (:abstract doc-struct-map))))

(defn gen-unlabeled-data-from-doclist [doc-struct-record-list]
    (apply concat
         (map gen-unlabeled-data-from-record
              doc-struct-record-list)))

(defn write-feature-list [filename feature-list]
  "Write features to file."
  (chem.utils/write-elements filename 
                             (map (fn [feature]
                                    (clojure.string/join "\t" feature))
                                  feature-list)))

(defn write-features-for-record 
  ([prefix record feature-list-func]
     (write-feature-list (format "%s/%s-features.tsv" prefix (:docid record))
                                     (feature-list-func record)))
  ([prefix record]
     (write-features-for-record prefix record gen-feature-list-from-record))
  ([record]
     (write-features-for-record "data/features" record gen-feature-list-from-record)))

(defn write-unlabeled-data-for-record 
  ([prefix record]
     (write-feature-list (format "%s/%s-unlabeled.txt" prefix (:docid record))
                                     (gen-unlabeled-data-from-record record)))
  ([record]
     (write-unlabeled-data-for-record "data/unlabeled" record)))



(defn make-instance [data target name source]
  (new Instance data target name source))

(defn make-instancelist-from-feature-list [feature-list]
  (map (fn [feature]


         )
   feature-list))

(defn train-crf [training-filename model-filename]
  (let [default-label "0"
        orders        (range 1 2)
        iterations    500
        variance      10
        p             (crfs/list2pipe [(new SimpleTaggerSentence2TokenSequence),
                                       (new TokenSequence2FeatureVectorSequence)]
                                      default-label)]
    (.setTargetProcessing p true)
    (let [training-data (crfs/line-group-instance-list p training-filename)
          crf           (crfs/init-new-crf training-data orders default-label variance)
          crft          (new CRFTrainerByLabelLikelihood crf)]
      ;; (.train crf training-data nil nil nil iterations)
      (.setUseSparseWeights crft false)
      (crfs/save-model crf model-filename)  )))

(defn test-crf [model-filename test-filename]
  (let [crf (crfs/load-model model-filename)
        p   (.getInputPipe crf)]
    (.setTargetProcessing p true)
    (let [testing-data (crfs/line-group-instance-list p test-filename)
          evaluator (new TokenAccuracyEvaluator testing-data "what")
          trainer   []]
      (.evaluateInstanceList trainer testing-data)
      (.evaluate evaluator trainer)
)))

(defn run-training-example [training-data testing-data]

  ;; setup:
  ;;    CRF (model) and the state machine
  ;;    CRFOptimizableBy* objects (terms in the objective function)
  ;;    CRF trainer
  ;;    evaluator and writer
  (let [crf (CRF. (.getDataAlphabet training-data)
                  (.getTargetAlphabet training-data))]

    ;; construct the finite state machine
    (.addFullyConnectedStatesForLabels crf)
    ;; initialize model's weights
    (.setWeightsDimensionAsIn crf training-data false)

    ;;  CRFOptimizableBy* objects (terms in the objective function)
    ;; objective 1: label likelihood objective
    (let [optLabel (new CRFOptimizableByLabelLikelihood crf training-data)

          ;; CRF trainer
          opts (into-array CRFTrainerByL1LabelLikelihood optLabel)
          ;; by default, use L-BFGS as the optimizer
          crfTrainer (new CRFTrainerByValueGradients crf opts)
          labels     (into-array String ["I-PER" "I-LOC" "I-ORG" "I-MISC"])
          evaluator  (new MultiSegmentationEvaluator
                          (into-array InstanceList [training-data testing-data])
                          (into-array String ["train" "test"])
                          labels labels)
          ]
      (.addEvaluator crfTrainer evaluator)
      (let [crfWriter (new CRFWriter "ner_crf.model")]
        (.addEvaluator crfTrainer crfWriter)

        ;; all set done, train until convergence
        (.setMaxResets crfTrainer 0)
        (.train crfTrainer training-data Integer/MAX_VALUE)

        ;; evaluate
        (.evaluate evaluator crfTrainer)

 ))))

(defn gen-data-instance-list-from-file [data-filename]
  ^{:doc "Generate instance list from data contained in supplied filename."}
  (let [default-label "0"
        orders        (range 1 2)
        iterations    500
        variance      10
        p             (crfs/list2pipe [(new SimpleTaggerSentence2TokenSequence),
                                       (new TokenSequence2FeatureVectorSequence)]
                                      default-label)]
    (.setTargetProcessing p true)
    (crfs/line-group-instance-list p data-filename)))



;;
;; Your input file should be in the following format:
;;
;;         Bill \t CAPITALIZED \t noun
;;         slept \t non-noun
;;         here \t LOWERCASE \t STOPWORD \t non-noun
;;
;;
;; That is, each line represents one token, and has the format:
;;
;;     feature1 feature2 ... featuren label


(defn command-option-integer-array
  [owner name arg-name arg-type arg-required
   default-value-vector shortdoc longdoc]
  (new CommandOption$IntegerArray owner arg-name arg-type arg-required 
       (into-array Integer/TYPE default-value-vector) shortdoc longdoc))

(defn command-option-string
  [owner name arg-name arg-type arg-required
   default-value shortdoc longdoc]
  (new CommandOption$String owner arg-name arg-type arg-required 
       default-value shortdoc longdoc))


;; Default values for SimpleTagger/train
;;
;; parameter	type			default value
;;-------------------------------------------------------
;; training	InstanceList		data (required)
;; testing:	InstanceList		data or null
;; eval		TrasducerEvaluator	data or null
;; orders	int[]			new int[]{ 1 }
;; defaultLabel	String			"O"
;; forbidden	String			"\\s"
;; allowed	String			".*"
;; connected	boolean			true
;; iterations	int			500
;; var		double			10.0
;; crf		CRF			previous crf or null.
;;
;; 
;; (def crf (SimpleTagger/train instance-list nil nil (into-array Integer/TYPE [1]) 
;;                     "O" "\\s" ".*" true 500 10.0 null))

(defn train-crf [new-list-of-instance-lists]
^{:doc "Given a list of instance list, one document per instance list,
         train CRF model using SimpleTagger/train."}
  (loop [crf nil
         list-of-instance-lists new-list-of-instance-lists]
    (if (= (count list-of-instance-lists) 0)
      crf
      (let [instance-list (first list-of-instance-lists)]
        (recur (SimpleTagger/train instance-list nil nil (into-array Integer/TYPE [1]) 
                                   "O" "\\s" ".*" true 500 10.0 crf)
               (rest list-of-instance-lists))))))
    


(defn apply-model [model input k]
  (let [cache-size-option 100000]
    (if (= k 1)
      (into-array Sequence (list (.transduce model input)))
      (.toArray Sequence
                (.bestOutputSeqences
                 (new MaxLatticeDefault model input nil (.value cache-size-option))
                 k)))))


(defn save-model [crf file]
  (with-open [s (ObjectOutputStream. (FileOutputStream. file))]
    (.writeObject s crf)))

(defn load-model [file]
  (with-open [s (ObjectInputStream. (FileInputStream. file))]
    (.readObject s)))

(defn load-data [filename]
  (let [p (new SimpleTagger$SimpleTaggerSentence2FeatureVectorSequence)]
    (.lookupIndex (.getTargetAlphabet p) "O")
    (let [test-data (new InstanceList p)]
      (with-open [rdr (new FileReader filename)]
        (.addThruPipe test-data (new LineGroupIterator rdr (Pattern/compile "^\\s*$") true))
        test-data))))

(defn apply-test-data-instances [crf test-data]
  "apply crf to data contained in test instances"
  (map (fn [instance]
         (let [input (.getData instance)]
           (apply crf input 1)))
   test-data))

;; fin
