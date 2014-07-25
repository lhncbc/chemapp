(ns chem.partial-enhanced
  (:use     [clojure.set])
  (:require [chem.partial :as partial])
  (:require [chem.metamap-api :as metamap-api])
  (:require [chem.metamap-annotation :as mm-annotation])
  (:require [chem.metamap-tokenization :as mm-tokenization])
  (:require [chem.mti-filtering :as mti-filtering]))

(defn match [mmapi document]
  "Do partial/match and enhance annotations with conceptids if
   possible."
  (let [result (partial/match document)]
    (conj result {:annotations (mm-annotation/get-enhanced-annotations mmapi (:annotations result))})))

(defn match2 [mmapi document]
  "Do partial/match and enhance annotations with conceptids if
   possible."
  (let [partial-result (partial/match document)
        token-result (mm-tokenization/gen-token-annotations document)]
    (conj partial-result token-result 
          {:annotations (mm-annotation/get-enhanced-annotations mmapi (:annotations partial-result))}
          {:annotations (mm-annotation/get-enhanced-annotations mmapi (:annotations token-result))})))

(defn is-chemical [annotation]
  (let [conceptlist (:conceptlist annotation)
        semtypelist (:semtypelist annotation)]
    (and (or (> (count (intersection mti-filtering/is-element? (set conceptlist))) 0)
             (> (count (intersection mti-filtering/small-biochemicals-isa-object-cuiset (set conceptlist))) 0)
             (> (count (intersection mti-filtering/is-small-biochemical? (set conceptlist))) 0)
             (> (count (intersection mti-filtering/is-peptide? (set conceptlist))) 0)
             (> (count (intersection mti-filtering/is-nucleotide? (set conceptlist))) 0)
             (> (count (intersection mti-filtering/is-polymer? (set conceptlist))) 0)
             (> (count (intersection mti-filtering/organic-nitrogen-compound-cuiset (set conceptlist))) 0)
             (> (count (intersection mti-filtering/valid-semtypes (set (flatten semtypelist)))) 0))
         (= (count (intersection mti-filtering/exclude-semtypes (set (flatten semtypelist)))) 0))))

(defn filter-results [result]
  "filter title or abstract result that has been annotated with UMLS concepts
   Example:
   (filter-results {:abstract-result partial-enhanced-result))"
   (let [new-annotation-list
        (filter is-chemical
                (:annotations result))]
    (hash-map :annotations new-annotation-list )))

(defn add-chemical-classes [annotation]
  (let [conceptlist (:conceptlist annotation)
        semtypelist (:semtypelist annotation)]
    (conj annotation 
          {:chemical-classes
           (apply clojure.set/union 
                  (map (fn [concept]
                         (set (filter #(not (nil? %))
                                   (list (when (mti-filtering/is-element? concept) :element)
                                         (when (mti-filtering/small-biochemicals-isa-object-cuiset concept) :small-biochemical)
                                         (when (mti-filtering/is-small-biochemical? concept) :small-biochemical)
                                         (when (mti-filtering/is-peptide? concept) :peptide)
                                         (when (mti-filtering/is-nucleotide? concept) :nucleotide)
                                         (when (mti-filtering/is-polymer? concept) :polymer)
                                         (when (mti-filtering/organic-nitrogen-compound-cuiset concept) :organic-nitrogen-Compound)))))
                conceptlist))})))

(defn add-chemical-class-annotations [result]
  )

(defn filter-match [mmapi document]
  "Do partial-match and enhance annotations with conceptids using
   MetaMap.  If possible then filter using MRREL."
  (let [result (partial/match document)]
    (conj result (filter-results {:annotations (mm-annotation/get-enhanced-annotations mmapi (:annotations result))}))))

(defn filter-match2 [mmapi document]
  "Do partial-match and a metamap-style tokenization only and enhance
   annotations with conceptids using MetaMap.  If possible then filter
   using MRREL."
  (let [partial-result (partial/match document)
        token-result (mm-tokenization/gen-token-annotations document)]
    (conj partial-result token-result 
          (filter-results
           {:annotations (mm-annotation/get-enhanced-annotations mmapi (:annotations partial-result))})
          (filter-results
           {:annotations (mm-annotation/get-enhanced-annotations mmapi (:annotations token-result))}))))


