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

(defn list-combinations
  "Generate all possible sublist combinations of supplied list."
  [itemlist]
  (mapv (fn [subitemlist]
          (mapv (fn [subsubitemlist]
                  subsubitemlist)
                (list-head-proper-sublists subitemlist)))
        (list-tail-proper-sublists itemlist)))

(defn find-longest-matches
  "Return the longest spanning entities in supplied token lists
  removing any entities that are subsumed by a longer matching entity."
  [mapped-tokenlist]
  (filter #(not (empty? %))
          (annotation-utils/remove-subsumed-annotations
           (mapcat (fn [list-of-tokenlists]
                     (mapcat (fn [tokenlist]
                               (add-spans-to-entitylist
                                (lookup (join "" (mapv #(:text %) tokenlist)))
                                tokenlist))
                             list-of-tokenlists))
                   (list-combinations mapped-tokenlist)))))

(defn process-document
  "Process document, returning spans, abbreviations, and MeSH ids."
  [document]
  (let [tagged-sentence-list (-> document
                                 sentences/make-sentence-list
                                 sentences/tokenize-sentences
                                 sentences/pos-tag-sentence-list
                                 sentences/enhance-sentence-list-pos-tags)
        abbrev-list (extract-abbrev/extract-abbr-pairs-string document)
        enhanced-annotation-list (mapcat (fn [tagged-sentence]
                                        (let [annotation-list0 (find-longest-matches (:pos-tags-enhanced tagged-sentence))
                                              annotation-list (sort-by #(-> % :span :start) (set annotation-list0))]
                                          (sentences/add-valid-abbreviation-annotations document
                                                                                        annotation-list
                                                                                        abbrev-list)))
                                      tagged-sentence-list)]

    (hash-map :spans (mapv #(:span %) enhanced-annotation-list)
              :annotations enhanced-annotation-list
              :tagged-sentence-list tagged-sentence-list
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
  (let [tagged-sentence-list (-> document
                                 sentences/make-sentence-list
                                 sentences/tokenize-sentences
                                 sentences/pos-tag-sentence-list
                                 sentences/enhance-sentence-list-pos-tags)
        abbrev-list (extract-abbrev/extract-abbr-pairs-string document)
        enhanced-annotation-list (mapcat (fn [tagged-sentence]
                                           (let [annotation-list0 (find-longest-matches (:pos-tags-enhanced tagged-sentence))
                                                 annotation-list (sort-by #(-> % :span :start) (set annotation-list0))]
                                             (sentences/add-valid-abbreviation-annotations document
                                                                                           annotation-list
                                                                                           abbrev-list)
                                             ))
                                         tagged-sentence-list)]    
    (set (mapv #(clojure.string/trim (:keyterm %))
               enhanced-annotation-list))))


