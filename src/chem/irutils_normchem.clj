(ns chem.irutils-normchem
  (:import (irutils InvertedFileContainer))
  (:require [clojure.string :as string :refer [join split lower-case]]
            [opennlp.nlp :as nlp]
            [chem.irutils :as irutils]
            [skr.tokenization :as mm-tokenization]
            [chem.span-utils :as span-utils]
            [chem.stopwords :as stopwords]
            [skr.mwi-utilities :as mwi-utilities]
            [chem.annotation-utils :as annotation-utils]
            [chem.extract-abbrev :as extract-abbrev]
            [chem.sentences :as sentences]
            [entityrec.string-utils :refer [list-head-proper-sublists
                                            list-tail-proper-sublists]])
  (:gen-class))

;; How did we load this?
;;
;; Table input file format (tab-separated)
;;
;;     term mesh cid smiles normalized-term
;;
;;  BDB chem database extraction program: 
;;
;;      /net/lhcdevfiler/vol/cgsb5/ind/II_Group_WorkArea/wjrogers/Projects/chem/pug/diy/dumpdb_ts.py
;;
;;  Generate table from BDB database created from mockup.py (in same directory as dumpdb_ts.py)
;;     
;;      python dumpdb_ts.py --db=Collections/meshchem/normchem2015db normchem2015db.dump
;;
;; See also, chem/irutils.clj and scripts/normchem_ivf.clj

(defonce ^:dynamic *memoized-normalize-ast-string* (memoize mwi-utilities/normalize-ast-string))

;; make sure irutils uses mmap for reading indexes
(System/setProperty "ifread.mapped","true")

(defonce gwa "/net/lhcdevfiler/vol/cgsb5/ind/II_Group_WorkArea")
(defonce gwah (str gwa "/wjrogers"))
(defonce ivfpath (str gwah "/studio/clojure/chem/data/ivf"))
(defonce tablepath (str ivfpath "/normchem/tables"))
(defonce indexpath (str ivfpath "/normchem/indices"))
(defonce indexname "normchem2017")
(def ^:dynamic *normchem-index* (irutils/create-index tablepath indexpath indexname))
(def annot-fields [:keyterm :text :meshid :pubchemid :synonym0 :synonym1 :span])

(defn lookup
  "Return list of terms that approximately match normalized form of
  input term."
  [term]
  (let [lcterm (lower-case term)]
    (if (and (or (Character/isLetter (first term))
                 (Character/isDigit (first term)))
             (> (count term) 2)
             (not (contains? stopwords/stopwords lcterm)))
      (map #(assoc % :text term)
           (irutils/lookup *normchem-index* lcterm ))
      {})))

(def ^:dynamic *memoized-lookup* (memoize lookup))

(defn filter-ws-pn
  "filter whitespace and punctuation from mapped-tokenlist"
  [tokenlist]
  (filter (fn [token] (not (contains? #{"pd" "ws" "pn" "cm"} (:class token))))
         tokenlist))

(defn add-spans-to-entitylist
  "Add span of tokenlist to each entity"
  [entitylist tokenlist]
  (map (fn [entity]
         (assoc entity :span {:start (-> tokenlist first :span :start)
                              :end (-> tokenlist last :span :end)}))
       entitylist))

(defn find-longest-matches
  "Return the longest spanning entities in supplied token lists
  removing any entities that are subsumed by a longer matching entity."
  [mapped-tokenlist]
  (filter #(not (nil? %))
          (annotation-utils/remove-subsumed-annotations
           (apply concat
                  (map (fn [subtokenlist]
                         (let [list-of-lists (list-head-proper-sublists subtokenlist)]
                           (loop [subtokenlist (first list-of-lists)
                                  rest-tokenlists (rest list-of-lists)
                                  entitylist (add-spans-to-entitylist
                                              (lookup (join "" (mapv #(:text %) subtokenlist)))
                                              subtokenlist)]
                             (cond (seq entitylist)  entitylist
                                   (empty? (first rest-tokenlists)) []
                                   :else (recur (first rest-tokenlists)
                                                (rest rest-tokenlists)
                                                (add-spans-to-entitylist
                                                 (lookup (join "" (mapv #(:text %) (rest subtokenlist))))
                                                 (rest subtokenlist)))))))
                       (list-tail-proper-sublists mapped-tokenlist))))))

(defn process-document
  "Process document, returning spans, abbreviations, and MeSH ids."
  [document]
  (let [tagged-sentence-list (-> document
                                 sentences/make-sentence-list
                                 sentences/tokenize-sentences
                                 sentences/pos-tag-sentence-list
                                 sentences/enhance-sentence-list-pos-tags)
        sentence-tokenlist (vec (apply concat (map #(:pos-tags-enhanced %) tagged-sentence-list)))
        doc-tokenlist (mm-tokenization/analyze-text document)
        tokenlist (sentences/add-pos-tags-to-document-tokenlist doc-tokenlist sentence-tokenlist)
        abbrev-list (extract-abbrev/extract-abbr-pairs-string document)
        annotation-list0 (find-longest-matches tokenlist)
        annotation-list (sort-by #(-> % :span :start) (set annotation-list0))
        enhanced-annotation-list (sentences/add-valid-abbreviation-annotations document
                                                                               annotation-list
                                                                               abbrev-list)]
    (hash-map :spans (mapv #(:span %) enhanced-annotation-list)
              :annotations enhanced-annotation-list
              :sentence-list tagged-sentence-list
              :tokenlist tokenlist
              :abbrev-list abbrev-list)))


(defn process-document-explore
  [document]
  (let [tokenlist (mm-tokenization/analyze-text document)
        abbrev-list (extract-abbrev/extract-abbr-pairs-string document)
        annotation-list (find-longest-matches tokenlist)]
    (hash-map :spans (map #(:span %) annotation-list)
              :annotations annotation-list
              :tokenlist tokenlist
              :abbrev-list abbrev-list)))

(defn index-document
  "Process document, returning set of unique terms found (including
  abbreviations)."
  [document]
  (let [tokenlist (mm-tokenization/analyze-text document)
        abbrev-list (extract-abbrev/extract-abbr-pairs-string document)
        annotation-list (find-longest-matches tokenlist)
        enhanced-annotation-list (if (empty? annotation-list)
                                   annotation-list
                                   (sentences/add-valid-abbreviation-annotations document
                                                                                 annotation-list
                                                                                 abbrev-list))]
    (set (mapv #(clojure.string/trim (:keyterm %))
               enhanced-annotation-list))))


