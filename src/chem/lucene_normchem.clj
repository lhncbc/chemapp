(ns chem.lucene-normchem
  (:import (java.io BufferedReader FileReader FileWriter)
           (java.util.regex Matcher Pattern)
           (java.lang Character))
  (:require [clojure.string :as string]
            [opennlp.nlp :as nlp]
            [chem.lucene :as lucene]
            [skr.tokenization :as mm-tokenization]
            [chem.span-utils :as span-utils]
            [chem.stopwords :as stopwords]
            [skr.mwi-utilities :as mwi-utilities]
            [chem.annotation-utils :as annotation-utils]
            [chem.extract-abbrev :as extract-abbrev]
            [chem.sentences :as sentences]))

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
;; See loading program SmallCollectionExample.java in
;; /net/lhcdevfiler/vol/cgsb5/ind/II_Group_WorkArea/wjrogers/studio/java/lucene/src/main/java/examples
;;
;;      cd /net/lhcdevfiler/vol/cgsb5/ind/II_Group_WorkArea/wjrogers/studio/java/lucene
;;      ./run.sh examples.SmallCollectionExample /rhome/wjrogers/lucenedb/example normchem2015db.dump
;;
;; 

(def ^:dynamic *max-slice-size* 15)

(defn nmslookup-one
  "return first match of normalized form of input term."
  [term]
  (lucene/lookup-one (mwi-utilities/normalize-meta-string term)))

(defn nmslookup
  "Return list of terms that approximately match normalized form of
  input term."
  [term]
  (map #(assoc % :text term)
       (lucene/nmslookup (mwi-utilities/normalize-meta-string term) 1000)))

(defn filter-matches 
  "Remove any empty matches and matches in which token is not at head
   of normalized string."
  ([term matchlist]
     (let [normterm (mwi-utilities/normalize-meta-string term)
           ntpattern (re-pattern (format "^%s" normterm))]
       (filter #(re-find ntpattern (:ncstring %))
               matchlist)))
  ([candidate]
     (assoc candidate :matches 
            (filter-matches (-> candidate :token :text) (-> candidate :matches)))))

(defn add-token-indices
  "Add index of token to tokenmap. ({:index n})"
  [tokenlist]
  (map-indexed (fn [idx token]
                 (assoc token :index idx))
               tokenlist))

(defn list-partial-matches
  "Return the list of token that have a partial match in the index."
  [tokenlist]
  (filter #(not (empty? (:matches %)))
          (map (fn [token]
                 {:token token
                  :matches 
                  (if (and (not (contains? stopwords/stopwords (:text token)))
                           (Character/isLetter (first (:text token)))
                           (> (count (:text token)) 1)
                           (contains? #{"JJ" "NN" "NNP" "NNS" "NNPS"} (:part-of-speech token)))
                    (filter-matches (:text token) (nmslookup (:text token)))
                    '())})
               (add-token-indices tokenlist))))

(defn get-exact-matches-original
  "Given a candidate list of matches for head token in tokenlist, find
   subsets of tokenlist with head token with a match in the candidate list."
  [candidate tokenlist]
  (sort-by :start
           (set
            (filter #(and (not (empty? %)) (not (nil? %)))
                    (map (fn [slice-index]
                           (let [index (-> candidate :token :index)
                                 term (string/join 
                                       (map #(:text %)
                                            (subvec tokenlist index (min (count tokenlist)
                                                                         (+ index slice-index)))))
                                 normterm (mwi-utilities/normalize-meta-string term)
                                 matches (:matches candidate)]
                             (map #(assoc % :span {:start (-> candidate :token :span :start)
                                                   :end (+ (-> candidate :token :span :start)
                                                           (count (string/trim normterm)))})
                                  (filter #(= normterm (:ncstring %))
                                          matches))))
                         (range 1 *max-slice-size*))))))

(defn get-exact-matches
  "Given a candidate list of matches for head token in tokenlist, find
   subsets of tokenlist with head token with a match in the candidate list."
  [candidate tokenlist]
  (sort-by :start
           (set
            (filter #(and (not (empty? %)) (not (nil? %)))
                    (map (fn [slice-index]
                           (let [index (-> candidate :token :index)
                                 subtokenlist (subvec (vec tokenlist) index (min (count tokenlist)
                                                                               (+ index slice-index)))
                                 term (string/join (map #(:text %) subtokenlist))
                                 normterm (mwi-utilities/normalize-ast-string term)
                                 matches (:matches candidate)]
                             (if (= (count term) (count normterm))
                               (map #(assoc %
                                            :pos (-> candidate :token :part-of-speech)
                                            :span {:start (-> candidate :token :span :start)
                                                   :end (+ (-> candidate :token :span :start)
                                                           (count (string/trim normterm)))})
                                    (map #(assoc %
                                                 :text term
                                                 :tokenlength (count subtokenlist))
                                         (filter #(= normterm (:ncstring %))
                                                 matches)))
                               [])))
                         (range 1 *max-slice-size*))))))

(defn find-exact-matches
  "Find any exact matches in tokenlist from partial match list."
  [partial-match-list tokenlist]
  (filter #(not (contains? stopwords/stopwords (:text %)))
          (flatten
           (filter #(not (empty? %))
                   (map (fn [partial-match]
                          (get-exact-matches partial-match tokenlist))
                        partial-match-list)))))

(defn process-document
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
        partial-match-list (list-partial-matches tokenlist)
        annotation-list (find-exact-matches partial-match-list tokenlist)
        enhanced-annotation-list (sentences/add-valid-abbreviation-annotations document
                                                                     annotation-list
                                                                     abbrev-list)]
    (hash-map :spans (sort-by :start (map #(:span %) enhanced-annotation-list))
              :annotations (sort-by #(-> % :span :start) (set enhanced-annotation-list))
              :sentence-list tagged-sentence-list
              :tokenlist tokenlist
              :abbrev-list abbrev-list)))



