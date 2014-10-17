(ns chem.mallet
  (:use [clojure.pprint])
  (:require [clojure.string :as string]
            [skr.tokenization :as tokenization])
  (:use [chem.feature-generation :only (get-sentences)] )
  (:import (java.io FileReader FileInputStream ObjectInputStream StringReader))
  (:import (java.util List))
  (:import (java.util.regex Pattern))
  (:import (cc.mallet.fst SimpleTagger
                          SimpleTagger$SimpleTaggerSentence2FeatureVectorSequence
                          MaxLatticeDefault))
  (:import (cc.mallet.pipe.iterator LineGroupIterator ArrayIterator
                                    StringArrayIterator))
  (:import (cc.mallet.types Instance InstanceList Sequence))
  (:import (crfexample SimpleTaggerTest CRFTuple))
  )


(defonce ^:dynamic *cache-size* 10000)
(defonce ^:dynamic *n-best* 1)

(defn apply-transducer
  "Apply a transducer to an input sequence to produce the k highest-scoring
   output sequences.

   @param model the <code>Transducer</code>
   @param input the input sequence
   @param k the number of answers to return
   @return array of the k highest-scoring output sequences"
 [model input k]
 (if (= k 1)
   (vec (list (.transduce model input)))
   (let [lattice (new MaxLatticeDefault model input nil *cache-size*)]
     (vec (list (.bestOutputSequence lattice k))))))

(defn load-model
  "Load model file containing feature and class alphabets, and crf
   object."
  [filename]
  (with-open [ois (ObjectInputStream. (FileInputStream. filename))]
    (.readObject ois)))

(defn get-input-pipe [crfmodel]
  (.getInputPipe crfmodel))

(defn stringlist-to-stringarray-2d [tokenlist]
  (into-array
   (vec (map #(into-array String (vec (list %)))
             tokenlist))))

(defn populate-testdata [crf testReader]
  (let [p (.getInputPipe crf)]
    (.setTargetProcessing p false)
    (let [testData (new InstanceList p)]
      (.addThruPipe testData
                    (new LineGroupIterator testReader
                         (Pattern/compile "^\\s*$")
                         true))
      ;; (println (str "Number of predicates: " (.size (.getDataAlphabet p))))
      testData)))

  
;; Alternate method of adding labels to lookup index from target alphabet:
;;
;;   (let [p (new SimpleTagger$SimpleTaggerSentence2FeatureVectorSequence)]
;;     (.lookupIndex (.getTargetAlphabet p) "O") ;;  set target alphabet for CHEMICAL and O (not chemical)
;;     (.lookupIndex (.getTargetAlphabet p) "CHEMICAL")
;;
(defn test-reader [crf testReader]
  (let [testData (populate-testdata crf testReader)
        outputs (map (fn [instance] 
                       (let [input (.getData instance)
                             output (apply-transducer crf input 1)]
                         (map (fn [idx]
                                (vec (list (.get (first output) idx)
                                           (.toString (.get input idx)))))
                              (range (.size (first output))))))
                     testData)]
    (hash-map :test-data testData
              :outputs outputs)
    ))

(defn test-file [crf testFilename]
  (test-reader crf (new FileReader testFilename)))

(defn test-document [crfmodel document]
  (with-open [rdr (new StringReader
                       (string/join "\r"
                                    (tokenization/tokenize document 2)))]
    (test-reader crfmodel rdr)))

(defn test-record-v1 [crfmodel record]
  (with-open [rdr (new StringReader
                       (string/join "\r"
                                    (concat 
                                     (tokenization/tokenize-no-ws (:title record))
                                     (tokenization/tokenize-no-ws (:abstract record)))))]
    (test-reader crfmodel rdr)))

(defn test-record  [crfmodel record]
  (with-open [rdr (new StringReader
                       (string/join "\r"
                                    (clojure.core/apply concat
                                           (map #(conj % "\r")
                                                (map #(tokenization/tokenize-no-ws %)
                                                     (concat
                                                      (get-sentences (:title record))
                                                      (get-sentences (:abstract record))))))))]
    (test-reader crfmodel rdr)))

(defn test-tokenlist
  "Whitespace must be removed.  (use: (skr.tokenization/tokenize text 2))"
  [crfmodel tokenlist]
  (with-open [rdr (new StringReader
                    (string/join "\n" tokenlist))]
    (test-reader crfmodel rdr)))
  

;;        
;; bean properties of CRF object
;; user> (keys (bean crf))
;; (:inputPipe
;;  :outputAlphabet
;;  :weights
;;  :defaultWeights
;;  :weightsValueChangeStamp
;;  :inputAlphabet
;;  :numParameters
;;  :class
;;  :maxLatticeFactory
;;  :trainable
;;  :parameters
;;  :generative
;;  :parametersAbsNorm
;;  :sumLatticeFactory
;;  :outputPipe
;;  :weightsStructureChangeStamp)
;; nil

