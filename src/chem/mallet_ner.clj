(ns chem.mallet-ner
  (:import (java.io StringReader))
  (:require [clojure.string :as string]
            [chem.mallet]
            [chem.rules]
            [chem.partial :as partial]
            [chem.annotations :as annotations]
            [chem.annotation-utils :as annot-utils]
            [chem.evaluation :as eval]
            [chem.stopwords :as stopwords]
            [chem.token-partial :as token-partial]
            [chem.feature-generation :as fg]
            [skr.tokenization :as tokenization]
            [chem.opennlp :refer [get-sentences tokenize pos-tag]]
            [chem.english-words :as englishwords])
  (:gen-class))

(defonce ^:dynamic *crf-models-dir* "data/models/mallet")
;;(def ^:dynamic *crf-model-default-filename* (str *crf-models-dir* "/all.multi-label.seqtagcrf.model"))
(def ^:dynamic *crf-model-default-filename* (str *crf-models-dir* "/seqtagcrf-alt-lex.model"))
;;(def ^:dynamic *crf-model-default-filename* (str *crf-models-dir* "/multiclass-seqtagcrf.model"))
(defonce ^:dynamic *crf-model* nil)

(defn set-crf-model! [crf-model]
  (def ^:dynamic *crf-model* crf-model))

(defn set-crf-model-from-file! [crf-modelfilename]
  (def ^:dynamic *crf-model* (chem.mallet/load-model crf-modelfilename)))

(defn init 
  ([]
     (set-crf-model-from-file! *crf-model-default-filename*))
  ([model-filename]
     (set-crf-model-from-file! model-filename)))

(defn add-offset-to-spans
  "Increase token span indexes by offset."
  [tokenlist offset]
  (map #(assoc % :span {:start (+ (:start (:span %)) offset)
                        :end (+ (:end (:span %)) offset)})
       tokenlist))

(defn extract-sentence-features [sentence-smap]
  (let [sentence-tokenlist
        (fg/add-pos-tags
         (add-offset-to-spans
          (tokenization/analyze-text-chemicals (:text sentence-smap))
          (:start sentence-smap)))]
    (map (fn [token]
           (conj token {:location (:location sentence-smap)}))
     sentence-tokenlist)))

(defn make-sentence-map [source-text location sentence]
  (hash-map :text sentence
            :location location
            :start (.indexOf source-text sentence)))

(defn extract-document-features [document-text location]
  (map #(extract-sentence-features %)
        (map #(make-sentence-map document-text location %)
             (@get-sentences document-text))))

(defn tag-unlabelled-document-features
  "a sordid attempt at adding additional features to feature vector.
   currently:
     token-type: (LC,MC,NU,IC, etc.) [disabled]
     is-chemical-fragment?  (CHEMFRAG)
     is-stopword?           (STOPWORD)
     pos                    (part-of-speech)"
  [document-tokenlists]
  (map (fn [sentence-featurelist]
         (map #(list (:text %)
                     ;; (:location %)
                     ;; (string/upper-case (tokenization/classify-token-2 (first %)))
                     (fg/chem-fragment-present (first %))
                     (fg/is-stopword (:text %))
                     (:pos %)
                     (:class %))
              sentence-featurelist))
       document-tokenlists))

(defn assemble-sentences
  "Combine sentence feature list into sentence to send to mallet SimpleTagger."
  [sentences]
  (string/join "\n\n"
               (map (fn [sentence]
                      (string/join "\n" 
                                   (map (fn [token]
                                          (string/join " " token))
                                        sentence)))
                    sentences)))

(defn outputs-to-annotations
  "Convert Mallet outputs to annotations and filter out any
   annotations that have bad endings."
 [document-tokenlists outputs]
 (apply concat
        (map (fn [feature-tokenlist output]
               (filter #(and (> (count (:text %)) 1)
                             (not= (:output %) "O")
                             (not (chem.rules/has-badending? (:text %))))
                       (map #(assoc % :output (first %2) :mallet (second %2))
                            feature-tokenlist output)))
             document-tokenlists outputs)))

(defn outputs-to-annotations-raw
  "Convert Mallet outputs to annotations and filter out any
   annotations that have bad endings."
 [document-tokenlists outputs]
 (apply concat
        (map (fn [feature-tokenlist output]
               (map #(assoc % :output (first %2) :mallet (second %2))
                    feature-tokenlist output))
             document-tokenlists outputs)))

(defn filter-short-annotations [annotation-list]
  (filter (fn [annotation]
            (> (count (:text annotation)) 1))
          annotation-list))

(defn annotate-text-raw
  [document-text location]
 (let [document-tokenlists(extract-document-features document-text location)
        feature-tokenlists (tag-unlabelled-document-features document-tokenlists)
        reader (new StringReader (assemble-sentences feature-tokenlists))
        result (chem.mallet/test-reader *crf-model* reader)]
      (outputs-to-annotations-raw document-tokenlists (:outputs result))))

(defn annotate-text
  [document-text location]
  (let [document-tokenlists(extract-document-features document-text location)
        feature-tokenlists (tag-unlabelled-document-features document-tokenlists)
        reader (new StringReader (assemble-sentences feature-tokenlists))
        result (chem.mallet/test-reader *crf-model* reader)]
      (outputs-to-annotations document-tokenlists (:outputs result))))


(defn annotate-document
  [document-text location]
  (let [document-tokenlists (extract-document-features document-text location)
        feature-tokenlists (tag-unlabelled-document-features document-tokenlists)
        reader (new StringReader (assemble-sentences feature-tokenlists))
        result (chem.mallet/test-reader *crf-model* reader)]
    (filter-short-annotations
      (outputs-to-annotations document-tokenlists (:outputs result)))))

(defn annotate-record 
  "Annotate record"
  ([record]
     (annotate-record :mallet-ner record))
  ([engine-keyword record]
     (assoc record
       engine-keyword (hash-map
                       :title-result (hash-map
                                      :annotations (annotate-document (:title record) "T"))
                       :abstract-result (hash-map
                                         :annotations (annotate-document (:abstract record) "A"))
                       ))))

(defn gen-spans-from-span [annotation]
  (assoc annotation :spans [(annotation :span)]))

(defn sim-meta-classifier-for-record
  "Simulate use of meta-classifier."
  [record]
  (let [annotated-record (annotate-record record)]
    (assoc (select-keys annotated-record [:docid :title :abstract])
      :meta-classifier-annotations 
      (map gen-spans-from-span
           (-> annotated-record  :mallet-ner :abstract-result :annotations)))))

(defn gen-chemdner-resultlist
  [annotated-record-list]
  (apply concat
         (map #(eval/docid-termlist-to-chemdner-result
                (:docid %)
                (eval/get-annotation-termlist % :mallet-ner))
              annotated-record-list)))

(defn gen-labels-with-probability
  "Convert Stanford NER tables to Stacking format with labels
   containing term$docid$start$end and score."
  [docid ner-table]
  (map (fn [row]
         (list docid (row :text) 1.0))
       ner-table))

(defn gen-mallet-ner-meta-data
  "Convert annotations into format that can be used by Dina's Meta Stacking program."
  ([annotated-recordlist]
     (gen-mallet-ner-meta-data annotated-recordlist :mallet-ner))
  ([annotated-recordlist enginekywd]
   (vec 
    (apply concat
           (vec 
            (map (fn [record]
                   (concat
                    (gen-labels-with-probability (:docid record) (-> record enginekywd :title-result))
                    (gen-labels-with-probability (:docid record) (-> record enginekywd :abstract-result))))
                 annotated-recordlist))))))

(defn filter-english-words
  "remove any annotations using english words."
  [annotationlist]
  (filterv (fn [annotation]
            (let [term (:text annotation)]
              (not (englishwords/is-real-word? term))))
           annotationlist))


(defn consolidate-BIO-annotations
  "Consolidate multiple tokens adjacent to each other.  Irrelevant
  tokens must be removed beforehand. "
  [annotation-list]
)

(defn normalize-output-labels
  [annotation-list]
  (mapv (fn [annotation]
          (if (:output annotation)
            (if (= (subs (:output annotation) 0 2) "B-")
              (assoc annotation :output (subs (:output annotation) 2))
              annotation)
            annotation))
   annotation-list))

(defn process-document
  "Annotate input document string instance."
  ([^String document]
   (process-document document "A"))
  ([^String document field]
   (let [annotations (filter #(not (nil? %))
                             (normalize-output-labels (annot-utils/consolidate-adjacent-annotations
                                                       (annotate-document document field))))]
     (hash-map :spans (if (empty? annotations) [] (mapv #(:span %) annotations))
               :annotations annotations ))))
