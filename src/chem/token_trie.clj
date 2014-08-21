(ns chem.token-trie
  (:require [chem.metamap-tokenization :as tokenization]))

;; a token-based (rather-than character-based) trie.
;;

(defn map-filter [f coll] (map f (filter f coll)))

(defn tokenize [x] (map #(:text %) (tokenization/analyze-text x)))

(defn add-to-trie [trie x meta-data]
  (let [tokenlist (tokenize x)]
    (assoc-in trie tokenlist (merge (get-in trie x) {:val x
                                                     :meta-data meta-data
                                                     :terminal true}))))

(defn in-trie? [trie x]
  "Returns true if the value x exists in the specified trie."
  (:terminal (get-in trie (tokenize x)) false))

(defn prefix-matches [trie prefix]
  "Returns a list of matches with the prefix specified in the trie specified."
  (map-filter :val (tree-seq map? vals (get-in trie (tokenize prefix)))))

(defn build-trie
  "Builds a trie over the values in the specified seq coll."
  [coll]
  (reduce (fn [newtrie [token meta-data]]
            (add-to-trie newtrie token meta-data))
          {} coll))
                   
(defn get-meta-data [trie x]
  "Returns metadata if the value x exists in the specified trie."
  (:meta-data (get-in trie (tokenize x)) nil))

(defn get-val [trie x]
  "Returns val if the value x exists in the specified trie."
  (:val (get-in trie (tokenize x)) nil))
