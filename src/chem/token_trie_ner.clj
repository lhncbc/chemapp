(ns chem.token-trie-ner
  (:require [chem.stopwords :as stopwords])
  (:require [chem.token-trie :as token-trie]))

(def ^:dynamic *max-token-scan* 15)

(defn build-normchem-trie [normchem-recordmap-list]
  (token-trie/build-trie 
   (filter #(and (> (count (first %)) 2) (nil? (stopwords/stopwords (first %))))
           (map #(list (:text %) (select-keys % [:meshid :cid :smiles]))
                normchem-recordmap-list))))

(defn new-chem-trie 
  "create chem term trie, populating it with MeSH record-list and terms-not-in-mesh"
  [mesh-recordmap-list chemdner-terms-not-in-mesh]
  (token-trie/build-trie 
   (filter #(and (> (count (first %)) 2) (nil? (stopwords/stopwords (first %))))
           (concat
            (map #(list (:text %) (select-keys % [:meshid :cid :smiles]))
                 mesh-recordmap-list)
            (map #(list % (hash-map :id "CHEMDNER")) chemdner-terms-not-in-mesh)))))

;; recursive versions of tag-text and maximal right scan

(defn maximal-right-scan-recursive
  "recursive version of tag-text and maximal right scan"
 [trie input-tokenlist]
  (let [tokenlist (subvec (vec input-tokenlist) 0 (min *max-token-scan* (count input-tokenlist)))]
    (loop [n 1
           resultlist []]
      (let [candidate (clojure.string/lower-case 
                       (clojure.string/join
                        (map #(:text %) (take n tokenlist))))
            candidate-resultlist
            (if (token-trie/in-trie? trie candidate)
              (conj resultlist (conj (hash-map :text (token-trie/get-val trie candidate)
                                               :span (hash-map :start (-> tokenlist first :span :start)
                                                               :end (+ (-> tokenlist first :span :start)
                                                                       (count candidate))))
                                     (token-trie/get-meta-data trie candidate)))
              resultlist)]
        (if (empty? (drop n tokenlist))
          candidate-resultlist
          (recur (inc n) candidate-resultlist))))))

(defn tag-text-recursive
  "recursive version of tag-text"
  [trie text]
  (loop [tokenlist (vec (chem.metamap-tokenization/analyze-text text))
         resultlist []]
    (let [token-text (:text (first tokenlist))
          is-beginning (if (not (nil? token-text))
                         (token-trie/prefix-matches trie (clojure.string/lower-case token-text))
                         false)]
      (if (empty? tokenlist)
        resultlist
        (recur (rest tokenlist) 
               (if is-beginning
                 (concat resultlist (maximal-right-scan-recursive trie tokenlist))
                 resultlist))))))

;; iterative versions of tag-text and maximal right scan

(defn maximal-right-scan-iterative
  "iterative version of maximal right scan"
 [trie input-tokenlist]
  (let [tokenlist (subvec input-tokenlist 0 (min *max-token-scan* (count input-tokenlist)))]
    (filter #(not (nil? %))
            (map-indexed (fn [n token]
                           (let [candidate (clojure.string/lower-case
                                            (clojure.string/join
                                             (map #(:text %) (take (inc n) tokenlist))))]
                             (when (token-trie/in-trie? trie candidate)
                               (list (conj
                                      (hash-map :text (token-trie/get-val trie candidate)
                                                :span (hash-map :start (-> tokenlist first :span :start)
                                                                :end (+ (-> tokenlist first :span :start)
                                                                        (count candidate))))
                                      (token-trie/get-meta-data trie candidate))))))
                         tokenlist))))

(defn tag-text-iterative
  "iterative version of tag-text"
 [trie text]
  (let [tokenlist (vec (chem.metamap-tokenization/analyze-text text))]
    (filter #(not (empty? %))
            (reduce (fn [newlist [idx token]]
                      (if (token-trie/prefix-matches trie (clojure.string/lower-case (:text token)))
                        (conj newlist (maximal-right-scan-iterative trie (subvec tokenlist idx)))
                        newlist))
                    [] (map-indexed #(list %1 %2) tokenlist)))))

(defn annotate-text 
  [a-trie text]
  (tag-text-recursive a-trie text))

(defn annotate-record [engine-keyword trie record]
  (conj record
        (hash-map engine-keyword
                  (hash-map :title-result 
                            (hash-map :annotations
                                      (annotate-text trie (:title record)))
                            :abstract-result 
                            (hash-map :annotations
                                      (annotate-text trie (:abstract record)))))))

(defn make-pairs
  "Make annotations from Trie NER into pairs usable by meta-classifier."
  ([annotated-record engine-keyword]
     (vec
      (set
       (map #(vec (list (:text %) 1.0))
            (concat 
             (-> annotated-record engine-keyword :title-result :annotations)
             (-> annotated-record engine-keyword :abstract-result :annotations))))))
  ([annotated-record]
     (make-pairs annotated-record :trie-ner)))

