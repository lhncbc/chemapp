(ns chem.combine-recognizers
  (:require [metamap-api.metamap-api :as mm-api]
            [chem.annotations :as annot]
            [chem.annotation-utils :as annotation-utils]
            [chem.span-utils :as span-utils]
            [chem.semtypes :as semtypes]
            [chem.partial :as partial]
            [chem.normchem :as normchem]
            [chem.lucene-normchem :as lucene-normchem]
            [chem.mallet-ner :as mallet-ner]))

(defn combine-spans [spanlist0 spanlist1]
  (span-utils/subsume-spans
   (sort-by :start (into [] (set (concat spanlist0 spanlist1))))))

(defn combine-annotations 
  [annotations0 annotations1]
  (annotation-utils/consolidate-adjacent-annotations 
   (sort-by #(-> % :span :start)
            (vals
             (reduce (fn [newmap annotation]
                       (if (contains? newmap (:span annotation))
                         (assoc newmap (:span annotation) (conj (newmap (:span annotation)) annotation))
                         (assoc newmap (:span annotation) annotation)))
                     {} (concat annotations0 annotations1))))))
  
(defn combine [annotator1 annotator2 document]
  (let [result0 (annotator1 document)
        result1 (annotator2 document)]
    (hash-map
     :spans       (combine-spans (result0 :spans) (result1 :spans))
     :annotations (combine-annotations (result0 :annotations) (result1 :annotations)))))

(defn combination-1
  "Normalized Chemical Match plus Partial Chemical Match"
 [document]
  (let [result0 (partial/match document)
        result1 (lucene-normchem/process-document document)]
    (hash-map
     :spans       (combine-spans (result0 :spans) (result1 :spans))
     :annotations (annot/remove-nested-annotations 
                   (combine-annotations (result0 :annotations) (result1 :annotations))))))

(defn combination-2
  "Normalized Chemical Match (MongoDB) plus Mallet CRF"
 [document]
  (let [result0 (mallet-ner/process-document document)
        result1 (normchem/process-document document)]
    (hash-map
     :spans       (combine-spans (result0 :spans) (result1 :spans))
     :annotations (annot/remove-nested-annotations 
                   (combine-annotations (result0 :annotations) (result1 :annotations))))))

(defn combination-3
  "Normalized Chemical Match (Lucene) plus Mallet CRF"
 [document]
  (let [result0 (mallet-ner/process-document document)
        result1 (lucene-normchem/process-document document)
        annotations (annot/remove-nested-annotations 
                     (combine-annotations (result0 :annotations) (result1 :annotations)))]
    (hash-map
     :spans       (map #(:span %) annotations)
     :annotations annotations)))
