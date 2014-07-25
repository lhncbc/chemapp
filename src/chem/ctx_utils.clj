(ns chem.ctx-utils
  (:import (gov.nih.nlm.ctx TrieHashTable GenericConcept GenericSentence)))

(defn new-trie-hash-table []
  (new TrieHashTable))

(defn new-concept
  ([name]
     (let [genericConcept (new GenericConcept name)]
       (.addSimpleToken genericConcept name)
       (.setPosition genericConcept 0)
       (.setOffset genericConcept 0)
       genericConcept))
  ([name meshid]
     (let [genericConcept (new GenericConcept name)]
       (.addSimpleToken genericConcept name)
       (.setPosition genericConcept 0)
       (.setOffset genericConcept 0)
       (.setCUI genericConcept meshid)
       genericConcept)))

(defn add-termlist-to-trie [trie-hash-table termlist]
  (map #(.put trie-hash-table (new-concept %)) termlist))

(defn begins-with [trie term]
  (map #(select-keys (bean %) [:CUI :conceptName :firstWord])
       (.beginsWith trie (clojure.string/lower-case term))))

(defn gen-termset-from-chemdner-gold-standard [records]
  (set (apply concat (map #(:chemdner-gold-standard %) records))))

(defn tag-text0 [trie-hash-table text]
  (filter #(and (not (nil? (:trie-match %)))
                (not (empty? (:trie-match %))))
          (map (fn [token]
                 (let [result (begins-with trie-hash-table (clojure.string/lower-case token))]
                       (when result
                         (hash-map :text token :trie-match result))))
               (chem.metamap-tokenization/tokenize text 5))))

(defn tag-text [trie-hash-table text]
  (vec
   (filter #(and (not (nil? (:trie-match %)))
                (not (empty? (:trie-match %))))
           (map (fn [token]
                  (let [result (begins-with trie-hash-table (clojure.string/lower-case (:text token)))]
                    (when result
                      (conj token (hash-map :trie-match result)))))
                (concat (chem.metamap-tokenization/analyze-text-chemicals-aggressive text)
                        (chem.metamap-tokenization/analyze-text text))))))

(defn tag-text-using-partial-match [trie-hash-table text]
  (vec
   (filter #(and (not (nil? (:trie-match %)))
                 (not (empty? (:trie-match %))))
           (map (fn [annotation]
                  (let [result (begins-with trie-hash-table (clojure.string/lower-case (:text annotation)))]
                    (when result 
                      (conj annotation (hash-map :trie-match result)))))
                (:annotations (chem.partial/match text))))))

;; add MeSH (normchem) terms to trie

(defn add-mesh-terms-to-trie 
  "add normchem terms (or any terms) to hash table based trie, and return trie 
   input table:
       term \tab meshui \tab cid \tab smiles"
  [trie-hash-table normchemlinelist]
  (dorun 
   (map (fn [line]
          (let [fields (clojure.string/split line #"\t")
                term   (nth fields 0)
                meshui (nth fields 1)
                cid    (nth fields 2)
                smiles (nth fields 3)]
            (when (and (> (count term) 1) 
                       (not (chem.stopwords/stopwords (clojure.string/lower-case term))))
              (.put trie-hash-table (new-concept term meshui)))))
        normchemlinelist)))

(defn add-chemdner-gold-terms-to-trie
  "add chemdner terms not in MeSH to trie"
  [trie-hash-table non-mesh-chemdner-gold-term-list]
  (dorun 
   (map (fn [term]
          (when (and (> (count term) 1) 
                     (not (chem.stopwords/stopwords (clojure.string/lower-case term))))
            (.put trie-hash-table (new-concept term "CHEMDNER"))))
        non-mesh-chemdner-gold-term-list)))

(defn new-term-trie 
  "create term trie, populating it with MeSH record-list and terms-not-in-mesh"
  [mesh-record-list chemdner-terms-not-in-mesh]
  (let [term-trie (chem.ctx-utils/new-trie-hash-table)]
    (add-mesh-terms-to-trie term-trie mesh-record-list)
    (add-chemdner-gold-terms-to-trie term-trie chemdner-terms-not-in-mesh)
    term-trie))

(def trie (new-trie-hash-table))

(defn promote-cuis [annotationlist]
  (map (fn [annotation]
         (if (annotation :trie-match)
           (conj annotation (hash-map :cui (-> annotation :trie-match first :CUI )))
           annotation)) 
       annotationlist))

(defn annotate-text 
  [a-trie text]
  (concat (promote-cuis (tag-text0 a-trie text))
          (promote-cuis (tag-text a-trie text))
          (promote-cuis (tag-text-using-partial-match a-trie text))))

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
  "Make annotations from enchilada0 NER into pairs usable by meta-classifier."
  ([annotated-record engine-keyword]
     (vec
      (set
       (map #(vec (list (:text %) 1.0))
            (concat 
             (-> annotated-record engine-keyword :title-result :annotations)
             (-> annotated-record engine-keyword :abstract-result :annotations))))))
  ([annotated-record]
     (make-pairs annotated-record :trie-ner)))

