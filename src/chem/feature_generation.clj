(ns chem.feature-generation
  (:import (java.io FileWriter))
  (:require [opennlp.nlp :as nlp]
            [clojure.string :as string]
            [skr.tokenization :as tokenization]
            [chem.span-utils :as span-utils]
            [chem.stopwords :as stopwords]
            [chem.dictionaries :as dict]
            [chem.chemdner-tools :as chemdner-tools]
            [chem.token-partial :as token-partial]
            [chem.opennlp :refer [get-sentences pos-tag]]))

;; When using Mallet:
;;
;; Your input file should be in the following format:
;;
;;         Bill CAPITALIZED noun
;;         slept non-noun
;;         here LOWERCASE STOPWORD non-noun
;;
;;
;; That is, each line represents one token, and has the format:
;;
;;     feature1 feature2 ... featuren label

(defonce ^:dynamic *chemical-type-map* nil)

(defn set-chemical-term-type-map! 
  [term-attribute-map]
  (def ^:dynamic *chemical-type-map* term-attribute-map))

(defn load-chemical-term-type-map
  "Populate *chemical-type-map* with chemical type attributes obtained
  from training-set annotations."
 [chemdner-annotation-list]
  (def ^:dynamic *chemical-type-map*
    (chemdner-tools/gen-term-attribute-map chemdner-annotation-list)))

(defn chemical-type [term]
  (let [result (*chemical-type-map* term)]
    (if result
      (:type result)
      "")))

(defonce ^:dynamic *chemical-docid-span-start-attribute-map* nil)

(defn set-chemical-docid-span-start-attribute-map! 
  [docid-span-start-attribute-map]
  (def ^:dynamic *chemical-docid-span-start-attribute-map* docid-span-start-attribute-map))

(defn load-chemical-docid-span-start-attribute-map
  "Populate *chemical-type-map* with chemical type attributes obtained
  from training-set annotations.
"
 [chemdner-annotation-list]
  (def ^:dynamic *chemical-docid-span-start-attribute-map*
    (chemdner-tools/gen-docid-span-start-attribute-map chemdner-annotation-list)))

(defn add-offset-to-spans
  "Increase token span indexes by offset."
  [tokenlist offset]
  (map #(assoc % :span {:start (+ (:start (:span %)) offset)
                        :end (+ (:end (:span %)) offset)})
       tokenlist))

(defn list-attribute [docid location token]
  (map #(:type %)
       (filter #(and (>= (-> token :span :start) (:start %))
                     (<  (-> token :span :start) (:end %)))
               (filter #(= location (:location %))
                       (sort-by :start (map first (vals (*chemical-docid-span-start-attribute-map* docid))))))))

(defn get-attribute [docid location token]
  (first
   (map #(:type %)
        (filter #(and (>= (-> token :span :start) (:start %))
                      (<  (-> token :span :start) (:end %)))
               (filter #(=  (:location %) location)
                       (sort-by :start (map first (vals (*chemical-docid-span-start-attribute-map* docid)))))))))

(defn apply-token-attribute-type
  "If token is in docid-span-start-attribute-map then apply attribute
  to token. Return token in anycase."
  [docid location token]
  (let [attribute
        (if (contains? *chemical-docid-span-start-attribute-map* docid)
          (if (contains? (get *chemical-docid-span-start-attribute-map* docid) (-> token :span :start))
            (:type (first (get (get *chemical-docid-span-start-attribute-map* docid) (-> token :span :start))))
            (get-attribute docid location token))
          nil)]
    (if (not (nil? attribute))
      (assoc token :type attribute :location location :output "CHEMICAL")
      (assoc token :type "O" :location location :output "O"))))

(defn add-pos-tags
  "Add part of speech info for tokens in tokenlist."
 [maptokenlist]
  (map merge
       maptokenlist
       (map #(hash-map :text (first %) :pos (second %)) 
            (@pos-tag (map #(:text %) maptokenlist)))))

(defn extract-sentence-features [docid sentence-smap]
  (let [sentence-tokenlist
        (add-pos-tags
         (add-offset-to-spans
          (tokenization/analyze-text-chemicals-aggressive (:text sentence-smap))
         (:start sentence-smap)))]
    (map (fn [token]
           (apply-token-attribute-type docid (:location sentence-smap) token))
     sentence-tokenlist)))

(defn make-sentence-map
  "Record position of each sentence in source text."
 [source-text location sentence]
  (hash-map :text sentence
            :location location
            :start (.indexOf source-text sentence)))
            
(defn extract-record-features [record]
  (map #(extract-sentence-features (:docid record) %)
       (concat
        (map #(make-sentence-map (:title record) "T" %)
             (@get-sentences (:title record)))
        (map #(make-sentence-map (:abstract record) "A" %)
             (@get-sentences (:abstract record))))))

(defn is-stopword [text]
  (if (contains? stopwords/stopwords (string/lower-case text))
    "STOPWORD" ""))

(defn chem-fragment-present [text]
  (if (or (not (empty? (token-partial/has-chemical-prefix (string/lower-case text))))
          (not (empty? (token-partial/has-chemical-infix (string/lower-case text)))))
    (cond 
     (not (empty? (token-partial/has-chemical-prefix (string/lower-case text)))) 
     (string/join " " (token-partial/has-chemical-prefix (string/lower-case text)))
     (not (empty? (token-partial/has-chemical-infix (string/lower-case text))))
     (string/join " " (token-partial/has-chemical-infix (string/lower-case text))))
    ""))

(defn mark-if-chemical [token]
  (if (not= (:output token) "O")
    "CHEMICAL"
    "O"))

(defn tag-record-features
  "a sordid attempt at adding additional features to feature vector.
   currently:
     token-type: (LC,MC,NU,IC, etc.) [disabled]
     is-chemical-fragment?  (CHEMFRAG)
     is-stopword?           (STOPWORD)
     class                  token class (metamap+ token tags: lc, ch, nu, )
     pos                    part-of-speech (penn treebank tags)
     output                 (is-chemical or not (CHEMICAL,O))
     type                   chemical type"
  [record]
  (map (fn [sentence-featurelist]
         (map #(list (:text %)
                     ;; (string/upper-case (tokenization/classify-token-2 (first %)))
                     ;; (:location %)
                     (chemical-type (first %))
                     (is-stopword (:text %))
                     (:class %)
                     (:type %))
              sentence-featurelist))
       (extract-record-features record)))

(defn gen-feature-document-list [document-list]
  (map #(assoc (select-keys % [:docid])
          :sentence-features (tag-record-features %))
   document-list))

(defn write-sentence-features-with-spaces
  "write features converting space features to \\s."
  [w sentence-feature-list]
  (dorun 
   (map
    #(.write w (format "%s %s\n" (if (= (first %) " ")
                                   "\\s"
                                   (first %)) (string/join " " (rest %))))
    sentence-feature-list)))

(defn write-sentence-features
  "write features converting space features to \\s."
  [w sentence-feature-list]
  (dorun 
   (.write w
           (format "%s"
                   (string/join " " 
                                (filter #(not (= (first %) " "))
                                        sentence-feature-list))))))

(defn write-stream-record-features 
  [w record]
  (dorun 
   (map #(do (write-sentence-features w %)
             (.write w "\n"))         ;add a blank line between sentences.
        (:sentence-features record))))

(defn write-record-features [filename record]
  (with-open [w (FileWriter. filename)]
    (dorun 
     (map #(do (write-sentence-features w %)
               (.write w "\n"))         ;add a blank line between sentences.
          (:sentence-features record)))))

(defn write-feature-document-list
  [feature-dir feature-document-list]
  (dorun 
   (map #(write-record-features
          (format "%s/%s.fv" feature-dir (:docid %)) %)
        feature-document-list)))


(defn write-feature-document-list-one-file
  [filename feature-document-list]
  (with-open [w (FileWriter. filename)]
    (dorun 
     (map #(do (write-stream-record-features w %)
               (.write w "\n"))
          feature-document-list))))


(defn write-sentence-unlabelled-features-with-spaces
  "Write features without labels for testing purposes."
[w sentence-feature-list]
  (dorun 
   (map
    #(.write w (format "%s\n" (if (= (first %) " ")
                                   "\\s"
                                   (first %))))
    sentence-feature-list)))

(defn write-sentence-unlabelled-features
  "Write features without labels for testing purposes."
  [w sentence-feature-list]
  (dorun 
   (.write w (format "%s"
                     (string/join " "
                                  (butlast 
                                   (filter #(not (= (first %) " "))
                                           sentence-feature-list)))))))


(defn write-stream-record-unlabelled-features 
[w record]
(dorun 
 (map #(do (write-sentence-unlabelled-features w %)
           (.write w "\n"))         ;add a blank line between sentences.
      (:sentence-features record))))

(defn write-record-unlabelled-features 
  [filename record]
  (with-open [w (FileWriter. filename)]
    (write-stream-record-unlabelled-features w record)))

(defn write-unlabelled-feature-document-list
  [feature-dir feature-document-list]
  (dorun 
   (map #(write-record-unlabelled-features
          (format "%s/%s.fv" feature-dir (:docid %)) %)
        feature-document-list)))


;; chemical features
;;
;;
