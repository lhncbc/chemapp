(ns chem.lucene-normchem
  (:import (java.io BufferedReader FileReader FileWriter))
  (:require [clojure.string :as string]
            [chem.lucene :as lucene]
            [skr.tokenization :as mm-tokenization]
            [chem.span-utils :as span-utils]
            [chem.stopwords :as stopwords]
            [skr.mwi-utilities :as mwi-utilities]
            [chem.annotation-utils :as annotation-utils]))

(def ^:dynamic *max-slice-size* 15)

(defn nmslookup-one
  "return first match of normalized form of input term."
  [term]
  (lucene/lookup-one (mwi-utilities/normalize-meta-string term)))

(defn nmslookup
  "Return list of terms that approximately match normalized form of
  input term."
  [term]
  (lucene/nmslookup (mwi-utilities/normalize-meta-string term) 1000))

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
                  (if (> (count (:text token)) 1)
                    (filter-matches (:text token) (nmslookup (:text token)))
                    '())})
               (add-token-indices tokenlist))))

(defn get-exact-matches
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
  (let [tokenlist (mm-tokenization/analyze-text document)
        partial-match-list (list-partial-matches tokenlist)
        annotation-list (find-exact-matches partial-match-list tokenlist)]
    (hash-map :spans (sort-by :start (map #(:span %) annotation-list))
              :annotations (sort-by #(-> % :span :start) (set annotation-list)))))



