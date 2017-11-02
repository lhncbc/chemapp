(ns chem.sentences
  (:import (java.io BufferedReader FileReader FileWriter))
  (:import (java.util.regex Matcher Pattern))
  (:require [clojure.string :as string]
            [clojure.set :refer [intersection]]
            [skr.tokenization :as mm-tokenization]
            [chem.span-utils :as span-utils]
            [chem.stopwords :as stopwords]
            [skr.mwi-utilities :as mwi-utilities]
            [chem.annotation-utils :as annotation-utils]
            [chem.extract-abbrev :as extract-abbrev]
            [chem.opennlp :refer [get-sentences tokenize pos-tag]])
  (:gen-class))

(defn make-sentence-list 
  [^String document]
  (map #(hash-map :sentence %
                  :span {:start (.indexOf document ^String %)
                         :end (+ (.indexOf document ^String %) (count %))})
       (get-sentences document)))

(defn tokenize-sentences
  "expects list of sentence structures of form {:sentence <string> :span {:start <int> :end <int>}}
  returns list of maps of form {:sentence <sentence> :tokenlist <tokenlist>} 

     tokenize-style: style of tokenization 

       0 : keep whitespace
       2 : strip whitespace
       3 : strip whitespace, don't break on dashes '-'
       4 : strip whitespace, don't break on dashes '-' or parens
       5 : strip whitespace, preserve IUPAC chemicals (aggressive)
       6 : strip whitespace, preserve IUPAC chemicals, remove trailing commas. (aggressive)
       7 : keep whitespace, don't break on dashes '-'
       8 : keep whitespace, don't break on dashes '-' or parens
       9 : keep whitespace, preserve IUPAC chemicals (aggressive)
      10 : keep whitespace, preserve IUPAC chemicals, remove trailing commas. (aggressive)"
  ([sentence-list]
   (tokenize-sentences 2))
  ([sentence-list tokenize-style]
   (map (fn [sentence]
          (assoc sentence :tokenlist (mm-tokenization/tokenize (:sentence sentence) tokenize-style)))
        sentence-list)))

(defn pos-tag-sentence-list 
  " Expects list of maps of form {:sentence <sentence> :tokenlist <tokenlist>} 
  returns list of maps of form: {:sentence <sentence> :tokenlist <tokenlist> :pos-tags <tokenlist with part-of-speech tags>}"
  [tokenized-sentence-list]
  (map (fn [sentence]
         (assoc sentence :pos-tags (pos-tag (:tokenlist sentence))))
       tokenized-sentence-list))


(defn add-spans-to-sentence-pos-tags
  "Spans to tokens with part-of-speech info.  Convert tokens to maps
  with keywords :part-of-speech :text and :span.  If passing a
  sentence map structure (smap) then add mapping :pos-tags-enhanced to
  hold token map. "
  ([^String sentence-text sentence-span tagged-tokenlist]
   (map (fn [token]
          (let [^String token-text (nth token 0)
                sentence-start (:start sentence-span) 
                start (.indexOf sentence-text token-text) ;start within sentence
                end   (+ start (count token-text))]
            (hash-map :text token-text
                      :part-of-speech (nth token 1)
                      :span {:start (+ sentence-start start)
                             :end   (+ sentence-start start (count token-text))})))
        tagged-tokenlist))
  ([sentence-smap]
     (assoc sentence-smap 
       :pos-tags-enhanced 
       (add-spans-to-sentence-pos-tags (:sentence sentence-smap) (:span sentence-smap) (:pos-tags sentence-smap)))))

(defn add-spans-and-classes-to-sentence-pos-tags-no-ws
  "Spans and character classes to tokens with part-of-speech info.
  Convert tokens to maps with keywords :part-of-speech :text
  and :span.  If passing a sentence map structure (smap) then add
  mapping :pos-tags-enhanced to hold token map. "
  ([^String sentence-text sentence-span tagged-tokenlist]
   (filter #(not= (:class %) "ws")
           (map (fn [token]
                  (let [^String token-text (nth token 0)
                        sentence-start (:start sentence-span) 
                        start (.indexOf sentence-text token-text) ;start within sentence
                        end   (+ start (count token-text))]
                    (hash-map :text token-text
                              :part-of-speech (nth token 1)
                              :class (mm-tokenization/classify-token token-text)
                              :span {:start (+ sentence-start start)
                                     :end   (+ sentence-start start (count token-text))})))
                tagged-tokenlist)))
  ([sentence-smap]
     (assoc sentence-smap 
       :pos-tags-enhanced 
       (add-spans-and-classes-to-sentence-pos-tags-no-ws
        (:sentence sentence-smap) (:span sentence-smap) (:pos-tags sentence-smap)))))

(defn enhance-sentence-list-pos-tags
  [sentence-list]
  (map add-spans-to-sentence-pos-tags sentence-list))

(defn enhance-sentence-list-pos-tags-spans-and-classes-no-ws
  [sentence-list]
  (map add-spans-and-classes-to-sentence-pos-tags-no-ws sentence-list))

(defn gen-token-span-map-from-tokenlist 
  "build a map of token keyed by span in text."
  [tokenlist]
  (reduce (fn [newmap token]
            (assoc newmap (:span token) token))
          {} tokenlist))

(defn get-element-span-map-from-elementlist
  [elementlist get-span-func]
  (reduce (fn [newmap element]
            (assoc newmap (get-span-func element) element))
          {} elementlist))

(defn add-pos-tags-to-document-tokenlist
  "If token from document tokenlist has same span of part-of-speech
  token in span map then merge them, else then just leave token as
  is."
  [document-tokenlist sentence-tokenlist]
  (let [token-span-map (gen-token-span-map-from-tokenlist sentence-tokenlist)]
    (vec
     (map (fn [token]
            (if (contains? token-span-map (:span token))
              (conj token (token-span-map (:span token)))
              token))
          document-tokenlist))))

(defn find-pattern-1
  "Return list of extents matching pattern
   This is implemented using Java methods, Clojure method would be shorter" 
  [^String pattern-expression ^String passage]
  (let [^Pattern pattern-pattern (Pattern/compile pattern-expression)
        pattern-string-matcher (.matcher pattern-pattern passage)]
    (loop [extents []]
      (if (not (.find pattern-string-matcher))
        extents
        (recur (conj extents {:group  (.group pattern-string-matcher)
                              :start (.start pattern-string-matcher)
                              :end (.end   pattern-string-matcher)}))))))

(defn find-pattern  "Return list of extents matching pattern
   This is implemented using Java methods, Clojure method would be shorter" 
  [^Pattern pattern ^String passage]
  (let [pattern-string-matcher (.matcher pattern passage)]
    (loop [extents []]
      (if (not (.find pattern-string-matcher))
        extents
        (recur (conj extents {:group  (.group pattern-string-matcher)
                              :start (.start pattern-string-matcher)
                              :end (.end   pattern-string-matcher)}))))))

(defn find-term
  [term text]
  (find-pattern-1 (format "\\b%s\\b" (-> term
                                         (string/replace "(" "\\(")
                                         (string/replace ")" "\\)")
                                         (string/replace "+" "\\+"))) text))

(defn make-abbrev-map
  [abbrev-list]
  (reduce (fn [newmap abbrev-info]
            (assoc newmap 
              (-> abbrev-info :short-form :text) abbrev-info
              (-> abbrev-info :long-form :text) abbrev-info))
          {} abbrev-list))

(defn make-annotations
  [text annotation new-text]
  (map (fn [match]
         (assoc annotation :text new-text
                :span {:start (:start match)
                       :end   (:end match)}))
         (find-term new-text text)))

(defn add-valid-abbreviation-annotations
  "Add any abbreviations that map to entities to annotation list."
  [text annotation-list abbrev-list]
  (if (empty? abbrev-list)
    annotation-list
    (let [abbrev-text-set (set (map #(-> % :long-form :text string/lower-case) abbrev-list))
          annot-text-set (set (map #(-> % :text string/trim string/lower-case) annotation-list))]
      (if (empty? (intersection abbrev-text-set annot-text-set))
        ;; if no long form of abbreviation is in the entity set then return the annotation list unchanged
        annotation-list
        ;; else add any abbreviations that map to entities.
        (let [abbrev-map (make-abbrev-map abbrev-list)]
          (flatten
           (map (fn [annotation] 
                  (if (contains? abbrev-map (:text annotation))
                    (if (= (string/lower-case (-> (abbrev-map (:text annotation)) :long-form :text))
                           (string/lower-case (:text annotation)))
                      (do
                        (comment
                          (print (format "%s : (%s -> %s)\n" (:text annotation)
                                         (-> (abbrev-map (:text annotation)) :long-form :text)
                                         (-> (abbrev-map (:text annotation)) :short-form :text))))
                        (cons annotation
                              (make-annotations text
                                                annotation
                                                (-> (abbrev-map (:text annotation)) :short-form :text))))
                      (do 
                        (comment
                          (print (format "%s -> short form: %s:%s\n" (:text annotation)
                                         (-> (abbrev-map (:text annotation)) :short-form :text)
                                         (-> (abbrev-map (:text annotation)) :long-form :text))))
                        (cons annotation
                              (make-annotations text
                                                annotation
                                                (-> (abbrev-map (:text annotation)) :short-form :text)))))
                    annotation))
                annotation-list)))))))
  
