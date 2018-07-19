(ns chem.irutils-normchem
  (:import (java.lang Character)
           (irutils InvertedFileContainer))
  (:require [clojure.string :as string :refer [join split lower-case]]
            [clojure.set :refer [union intersection]]
            [opennlp.nlp :as nlp]
            [chem.opennlp]
            [chem.irutils :as irutils]
            [skr.tokenization :as mm-tokenization]
            [chem.span-utils :as span-utils]
            [chem.stopwords :as stopwords]
            [chem.english-words :as englishwords]
            [skr.mwi-utilities :as mwi-utilities]
            [chem.annotation-utils :as annotation-utils]
            [chem.extract-abbrev :as extract-abbrev]
            [chem.sentences :as sentences]
            [chem.greek-convert :refer [convert-greek-chars]]
            [entityrec.string-utils :refer [list-head-proper-sublists
                                            list-tail-proper-sublists]]
            [entityrec.find-longest-match :as eflm]
            )
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
;; For information for index creation, see also, chem/irutils.clj and scripts/normchem_ivf.clj

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

(def term-id-exclusions
  "Terms that should be ignored/removed if specified for a particular id"
  {"ar"  "D001128",   ; Argon D001128 (not AR)
   "glu" "C514131",   ; methacryloylamidoglutamic acid
   "lead" "D007854", ; Lead (metal) D007854 (on lead [as in leading or leader])
   "doi" "C015952"   ; 4-iodo-2,5-dimethoxyphenylisopropylamine (but DOI means document online identifier)
   "com" "D015080"
   })

(defn in-excluded-map
  [el]
  (= (term-id-exclusions (lower-case (:text el))) (:meshid el)))

(defn lookup
  "Return list of terms that approximately match normalized form of
  input term."
  ([term]
   (let [lcterm (lower-case term)]
     (if (and (or (Character/isLetter (.charValue (first term)))
                  (Character/isDigit (.charValue (first term))))
              (> (count term) 2)
              (not (or (contains? stopwords/stopwords lcterm)
                       (englishwords/is-real-word? lcterm))))
       (set
        (filter #(not (in-excluded-map %))
                (map #(assoc % :text term)
                     (concat
                      (irutils/lookup *normchem-index* lcterm )
                      (irutils/lookup *normchem-index* (convert-greek-chars lcterm))))))
       {})))
  ([term tokenlist]
   (lookup term)))

(def ^:dynamic *memoized-lookup* (memoize lookup))

(defn filter-ws-pn
  "filter whitespace and punctuation from mapped-tokenlist"
  [tokenlist]
  (filter (fn [token] (not (contains? #{"pd" "ws" "pn" "cm"} (:class token))))
         tokenlist))

(defn add-spans-to-entitylist
  "Add span of tokenlist to each entity"
  ([entitylist tokenlist]
   (map (fn [entity]
          (assoc entity
                 :span {:start (-> tokenlist first :span :start)
                        :end (-> tokenlist last :span :end)}
                 :text (join " " (mapv #(:text %) tokenlist))))
        entitylist))
  ([entitylist tokenlist text]
   (map (fn [entity]
          (assoc entity
                 :span {:start (-> tokenlist first :span :start)
                        :end (-> tokenlist last :span :end)}
                 :text (subs text
                             (-> tokenlist first :span :start)
                             (-> tokenlist last :span :end))))
        entitylist)))

(defn list-combinations
  "Generate all possible sublist combinations of supplied list."
  [itemlist]
  (mapv (fn [subitemlist]
          (mapv (fn [subsubitemlist]
                  subsubitemlist)
                (list-head-proper-sublists subitemlist)))
        (list-tail-proper-sublists itemlist)))

(defn hybrid-lookup
  [tokenlist]
  (let [term0 (join "" (mapv #(:text %) tokenlist))]
    (concat 
     (lookup (join " " (mapv #(:text %) tokenlist)))
     (if (empty? term0) term0 (lookup term0)))))

(defn basic-lookup
  [tokenlist]
  (lookup (join " " (mapv #(:text %) tokenlist))))

(defn find-longest-matches
  "Return the longest spanning entities in supplied token lists
  removing any entities that are subsumed by a longer matching entity."
  ([mapped-tokenlist]
   (filter #(not (empty? %))
           (annotation-utils/remove-subsumed-annotations
            (mapcat (fn [list-of-tokenlists]
                      (mapcat (fn [[entitylist tokenlist]]
                                (add-spans-to-entitylist entitylist tokenlist))
                              (map (fn [tokenlist]
                                     (vector (basic-lookup tokenlist)
                                             tokenlist))
                                   list-of-tokenlists)))
                    (list-combinations mapped-tokenlist)))))
  ([mapped-tokenlist text]
   (filter #(not (empty? %))
           (annotation-utils/remove-subsumed-annotations
            (mapcat (fn [list-of-tokenlists]
                      (mapcat (fn [[entitylist tokenlist]]
                                (add-spans-to-entitylist entitylist tokenlist text))
                              (map (fn [tokenlist]
                                     (vector (basic-lookup tokenlist)
                                             tokenlist))
                                   list-of-tokenlists)))
                    (list-combinations mapped-tokenlist))))))

(defn find-matches-in-tagged-sentence
  ([tagged-sentence]
   (sort-by #(-> % :span :start)
            (set (find-longest-matches (:pos-tags-enhanced tagged-sentence)))))
  ([tagged-sentence text]
   (sort-by #(-> % :span :start)
            (set (find-longest-matches (:pos-tags-enhanced tagged-sentence) text))))
  ([tagged-sentence text abbrev-list]
   (sort-by #(-> % :span :start)
            (union
             (set (find-longest-matches (:pos-tags-enhanced tagged-sentence) text))
             (set (mapcat (fn [abbrev]
                            (if (contains? abbrev :mesh-set)
                              (map #(assoc % :span (-> abbrev :short-form :span))
                                   (:mesh-set abbrev))
                              '()))
                          abbrev-list)))
            )))

(defn check-terms
  [tagged-sentence]
  tagged-sentence)

(defn lookup-abbreviations
  "add any concepts that match abbreviations"
  [abbrev-list]
  (mapv (fn [abbrev]
          (let [mesh-set (-> abbrev :long-form :text lookup)]
            (if (empty? mesh-set)
              abbrev
              (assoc abbrev :mesh-set mesh-set))))
        abbrev-list))

(defn gen-annotations
  "Process document, returning spans, abbreviations, and MeSH ids."
  [document]
  (let [tagged-sentence-list (-> document
                                 sentences/make-sentence-list
                                 (sentences/tokenize-sentences 8)
                                 sentences/pos-tag-sentence-list
                                 sentences/enhance-sentence-list-pos-tags-spans-and-classes-no-ws)
        abbrev-list (lookup-abbreviations (extract-abbrev/extract-abbr-pairs-string document))]
    (mapcat (fn [tagged-sentence]
              (let [annotation-list (find-matches-in-tagged-sentence tagged-sentence document
                                                                     (filter #(contains? % :mesh-set)
                                                                             abbrev-list))]
                (sentences/add-valid-abbreviation-annotations document
                                                              annotation-list
                                                              abbrev-list)))
            tagged-sentence-list)))

(defn process-document
  "Process document, returning spans, abbreviations, and MeSH ids."
  [document]
  (let [enhanced-annotation-list (gen-annotations document)]
    (hash-map :spans (mapv #(:span %) enhanced-annotation-list)
              :annotations enhanced-annotation-list)))

(defn process-document-explore
  [document]
  (let [tokenlist (mm-tokenization/analyze-text-chemicals document)
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
  (set (mapv #(clojure.string/trim (:ncstring %))
             (:annotations (gen-annotations document)))))


(defn process-document-alt
  "Process document, returning spans, abbreviations, and MeSH ids."
  [document]
  (let [result (mapcat (fn [sentence]
                         (eflm/find-longest-match (mm-tokenization/analyze-text sentence) lookup))
                       (chem.opennlp/get-sentences document))]
    (hash-map
     :spans (mapv #(:span %) result)
     
     :annotations (mapcat (fn [record]
                          (mapv #(assoc % :span (:span record))
                                (:entity-list record)))
                          result)
    )))
  
