(ns chem.brat
  (:import (java.util LinkedHashSet))
  (:require [clojure.string :as string]
            [clojure.set :as setop]
            [clojure.java.io :as io]
            [chem.utils :as utils]))

(defn uniq-ordered-list
  "Remove duplicates from ordered list."
  [ordered-list]
  (let [ordered-set (LinkedHashSet.)]
       ;; This should probably use amalloy's ordered-set
       ;; implementation (amalloy/ordered) instead of LinkedHashSet.
    (.addAll ordered-set ordered-list)
    (into [] ordered-set)))

(defn consolidate-spans 
  "consolidate annotations in with the same span."
  [brat-result]
  (vec (vals 
        (reduce (fn [newmap annotation]
                  (let [span (:span annotation)]
                    (if (contains? newmap span)
                      (assoc newmap span
                             (assoc (newmap span) 
                               :reference-list
                               (vec (concat (:reference-list (newmap span)) (:reference-list annotation)))))
                      (assoc newmap span annotation))))
                {} brat-result))))

(defn gen-reference-list 
  "From annotation map instance generate list of normalization
   annotations repreenting semantic type and concept ids of annotation."
  [annotation]
  (if (contains? annotation :meshid)
    (vector 
     (hash-map :recordtype "N"
               :rid  "MeSHid"
               :eid  (annotation :meshid)
               :text (:cstring annotation)))
    []))

(defn annotations-to-brat 
  "Convert annotation list to un-normalized, unconsolidated BRAT (BRAT
  Rapid Annotation Tool) format with no ids.

  each map representing a text annotation 
  {:type \"<type of entity>\"
   :recordtype \"T\" (always T)
   :span <span of entity {:start <number> :end <number>}
   :text <exact text from original document>
   :reference-list <sequence of maps representing normalization annotations>
  }"
  [annotation-list]
  (map (fn [annotation]
         (hash-map
          :type "Chemical"
          :recordtype "T"
          :span (:span annotation)
          :text (:text annotation)
          :reference-list (gen-reference-list annotation)))
       annotation-list))

(defn serialize-brat-annotations
  "write brat annotations to list of strings."
  [brat-result]
  (map (fn [annotation]
         (case (:recordtype annotation)
           "T" (format "%s%d\t%s %d %d\t%s" 
                       (:recordtype annotation)
                       (:id annotation)
                       (:type annotation)
                       (-> annotation :span :start)
                       (-> annotation :span :end)
                       (:text annotation))
           "N" (format "%s%d\tReference T%d %s:%s\t%s" 
                       (:recordtype annotation)
                       (:id annotation)
                       (:text-annotation-id annotation)
                       (:rid annotation)
                       (:eid annotation)
                       (:text annotation))))
               brat-result))

(defn flatten-annotations
  [annotation-list]
  (reduce (fn [newlist annotation] 
            (if (> (count (:reference-list annotation)) 0)
              (concat newlist (cons annotation (:reference-list annotation)))
              (conj newlist annotation)
              ))
            [] annotation-list))

(defn id-brat-normalization-annotations
  "add ids to normalization annotation in reference-list of text annotations"
  [brat-text-annotation-list]
  (map-indexed (fn [tidx text-annotation]
                 (assoc text-annotation 
                   :reference-list (map-indexed (fn [idx norm-annotation]
                                                  (assoc norm-annotation
                                                    :id (+ (* (inc tidx) 10) idx)
                                                    :text-annotation-id (:id text-annotation)))
                                                (:reference-list text-annotation))))
       brat-text-annotation-list))

(defn id-brat-text-annotations
  [brat-text-annotation-list]
  (map-indexed (fn [idx text-annotation]
                 (assoc text-annotation
                   :id (inc idx)
                   :record-type "T"
                   :type "Chemical"))
               brat-text-annotation-list))

(defn brat-str
  [annotations]
  (apply str (map #(format "%s\n" %) 
                  (serialize-brat-annotations 
                   (flatten-annotations
                    (id-brat-normalization-annotations 
                     (id-brat-text-annotations
                      (annotations-to-brat annotations))))))))

(defn write-brat
  [annotation-list outfn]
  (with-open [wtr (io/writer outfn)]
    (dorun (.write wtr (brat-str annotation-list)))))
