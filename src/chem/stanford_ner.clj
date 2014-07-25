(ns chem.stanford-ner
  (:import (java.util List ArrayList))
  (:import (edu.stanford.nlp.ie.crf CRFClassifier))
  (:import (edu.stanford.nlp.ling CoreAnnotations))
  (:import (gov.nih.nlm.nls.ner.stanford ListProb))
  (:gen-class))

;; Functions for loading and using Stanford NER Classifiers

(defn load-classifier [serialized-classifier-filename]
  (CRFClassifier/getClassifierNoExceptions serialized-classifier-filename))

(defn classify-to-string 
  ([classifier text]
     (.classifyToString classifier text))
  ([classifier text output-format preserve-spacing]
     (.classifyToString classifier text output-format preserve-spacing)))

(defn classify-with-inline-xml [classifier text]
  (.classifyWithInlineXML classifier text))

(defn triple-to-map [triple]
  (hash-map :probability (.first triple)
            :start (.second triple)
            :end (.third triple)))

(defn convert-labels [java-label-map]
  (let [label-map (into {} java-label-map)]
    (into {} 
          (map (fn [entry]
                 (vec (list (first entry)
                            (triple-to-map (second entry)))))
               label-map))))

(defn java-prob-map-to-prob-map [java-prob-map]
  (map (fn [java-map-entry]
         (vec (list (first java-map-entry)
                    (convert-labels (second java-map-entry)))))
       java-prob-map))

(defn gen-prob-map [classifier sentence]
  "Convert map of maps to clojure structures."
  ;; (read-string (pr-str (ListProb/printProbsDocument classifier sentence)))
  (java-prob-map-to-prob-map (into {} (ListProb/getProbsDocument classifier sentence))))

(defn prob-map-to-table [prob-map]
  "Produce a table usable by print-table."
  (map #(conj (hash-map "TEXT" (first %)) (second %)) prob-map))

(defn get-probabilities-and-extents [classifier text]
  (let [sentence-list (.classify classifier text)]
    (apply concat
           (map (fn [sentence]
                  (prob-map-to-table 
                   (gen-prob-map classifier sentence)))
                sentence-list))))
  
(defn annotate-document 
  ([classifier target-class document-text]
     (filter #(> (% :probability) 0.5)
             (map (fn [entrymap]
                    (let [text   (entrymap "TEXT")
                          extent (entrymap target-class)
                          start  (extent :start)
                          end    (extent :end)]
                      (hash-map 
                       :text        (subs document-text start end)
                       :class       target-class
                       :probability (extent :probability)
                       :start       start
                       :end         end)))
                  (get-probabilities-and-extents classifier document-text))))
  ([classifier document-text]
     (annotate-document classifier "CHEMICAL" document-text)))

(defn annotate-document-v1 [classifier document-text]
  ^{:doc "annotate document text using Stanford NER classifier." }
  (hash-map :annotations
            (map (fn [triple]
                   (let [class (.first triple)
                         start (.second triple)
                         end   (.third triple)]
                     (hash-map 
                      :text  (subs document-text start end)
                      :class class
                      :start start
                      :end   end)))
                 (.classifyToCharacterOffsets classifier document-text))))

(defn annotate-record 
  ([classifier record]
     (annotate-record :stanford-ner classifier record))
  ([engine-keyword classifier record]
     (conj record
           (hash-map engine-keyword
                     (hash-map :title-result 
                               (hash-map :annotations
                                         (chem.stanford-ner/annotate-document classifier (:title record)))
                               :abstract-result 
                               (hash-map :annotations
                                         (chem.stanford-ner/annotate-document classifier (:abstract record))))))))

(defn annotate-record-list [classifier recordlist] 
  (map #(annotate-record classifier %)
       recordlist))

;; Example of using Stanford Classifier Java API directly:
;; 
;; user> (import edu.stanford.nlp.ie.crf.CRFClassifier)
;; edu.stanford.nlp.ie.crf.CRFClassifier
;; user> (def classifier (CRFClassifier/getClassifier "ner-model.ser.gz"))
;; #'user/classifier
;; user> classifier
;; #<CRFClassifier edu.stanford.nlp.ie.crf.CRFClassifier@444760c4>
;; user> (.classifyToString classifier "The present study aims to identify the association between androgen status and metabolic activity in skeletal and cardiac muscles of adult rats with transient gestational/neonatal-onset hypothyroidism.")
;; "The/O present/O study/O aims/O to/O identify/O the/O association/O between/O androgen/chemical status/O and/O metabolic/O activity/O in/O skeletal/O and/O cardiac/O muscles/O of/O adult/O rats/O with/O transient/O gestational/neonatal-onset/O hypothyroidism/O./O"
;; user>

(defn get-probs-document [classifier document-sentence]
  (let [p (.documentToDataAndLabels classifier document-sentence)
        cliqueTree (.getCliqueTree classifier p)
        classIndex (.classIndex classifier)]
    (map (fn [i]
           (let [wi (.get document-sentence i)]
             (hash-map :text (.get wi edu.stanford.nlp.ling.CoreAnnotations$TextAnnotation)
                       :begin (.get wi edu.stanford.nlp.ling.CoreAnnotations$CharacterOffsetBeginAnnotation)
                       :end (.get wi edu.stanford.nlp.ling.CoreAnnotations$CharacterOffsetEndAnnotation)
                       :labels (into {} (map (fn [label]
                                               (let [index (.indexOf classIndex label)
                                                      prob 0 ;; (.prob cliqueTree i index)
                                                     ]
                                                 [(keyword label) prob]))
                                             classIndex))  )))
         (range (.length cliqueTree)))))

(defn get-probs-document [classifier sentence-list]
  (map #(get-probs-document classifier %) sentence-list))


(defn clj-prob-map-to-table [prob-map]
  "Produce a table usable by print-table."
  (map #(conj (hash-map "TEXT" (first %)) (second %)) prob-map))

(defn gen-clj-prob-map [classifier sentence]
  "Convert map of maps to clojure structures."
  ;; (read-string (pr-str (ListProb/printProbsDocument classifier sentence)))
  (get-probs-document classifier sentence))

(defn get-clj-probabilities-and-extents [classifier text]
  (let [sentence-list (.classify classifier text)]
    (apply concat
           (map (fn [sentence]
                  (clj-prob-map-to-table 
                   (gen-clj-prob-map classifier sentence)))
                sentence-list))))

(defn make-pairs
  "Given record containing Stanford NER annotations, 
   return a list of terms and their associated weights."
  [record]
  (vec (map #(vec (list (:text %) (:probability %)))
            (-> record :stanford-ner :abstract-result :annotations))))

(defn classify-record
  "Given a Stanford NER classifier instance and a chemdner record map,
   return a list of terms and their associated weights."
  [classifier record]
  (make-pairs (annotate-record classifier record)))
