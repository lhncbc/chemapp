(ns chem.bio-feature-generation
  (:import (java.io FileWriter))
  (:require [opennlp.nlp :as nlp]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [reduce-fsm :as fsm]
            [skr.tokenization :as tokenization]
            [chem.sentences]
            [chem.chemdner-tools :as chemdner-tools]))

;; Given:
;;
;; + a document
;; + a list of match terms or matched spans
;; 
;; The process:
;;
;; Tokenize document with positional information comparing spans of
;; tokens with spans in matched spanlist tag begining token and
;; intermediate tokens withing span for each span.

(defn filter-section
  "Filter cemlist based on desired section.

  E.G.: Get just abstract spans for document 22401597:
  
       (bfg/filter-section (chemdner-training-cem-gold-map \"22401597\") \"A\")
  "
  [cemlist section]
  (filterv #(= (:section %) section) cemlist))

(defn filter-location
  "Filter cemlist based on desired location.

  E.G.: Get just abstract spans for document 22401597:
  
       (bfg/filter-location (chemdner-training-cem-gold-map \"22401597\") \"A\")
  "
  [cemlist location]
  (filterv #(= (:location %) location) cemlist))


(defn cem->spanlist
  "keep :start and :end keys"
  [cemlist]
  (mapv #(select-keys % [:start :end])
        cemlist))

(defn tag-tokens-using-spans
  "Add BIO tags to tokens using information from spanlist"
  [text spanlist]
  (let [tokenlist (tokenization/analyze-text-chemicals text)]
    (if (empty? spanlist)
      (mapv (fn [token]
              (assoc token :output "O"))
            tokenlist)
      (mapv (fn [token]
              (reduce (fn [newtoken span]
                        (cond
                          (= (-> newtoken :span :start) (:start span))
                          (assoc newtoken :output "B-CHEMICAL")
                          
                          (and (>= (-> newtoken :span :start) (:start span))
                               (<=  (-> newtoken :span :end) (:end span)))
                          (assoc newtoken :output "I-CHEMICAL")
                          
                          :else
                          (if (not (contains? newtoken :output))
                            (assoc newtoken :output "O")
                            newtoken)))
                      token spanlist))
            tokenlist))))

(defn tag-tokens-using-annotations
  "Add labels to tokens using information from CHEMDNER annotation-list"
  [text annotation-list]
  (let [tokenlist (tokenization/analyze-text-chemicals text)]
    (if (empty? annotation-list)
      (mapv (fn [token]
              (assoc token :output "O"))
            tokenlist)
      (mapv (fn [token]
              (reduce (fn [newtoken annotation]
                        (cond
                          (= (-> newtoken :span :start) (:start annotation))
                          (assoc newtoken :output (str "B-" (:type annotation)))
                          
                          (and (>= (-> newtoken :span :start) (:start annotation))
                               (<=  (-> newtoken :span :end) (:end annotation)))
                          (assoc newtoken :output (str "I-" (:type annotation)))
                          
                          :else
                          (if (not (contains? newtoken :output))
                            (assoc newtoken :output "O")
                            newtoken)))
                      token annotation-list))
          tokenlist))))

(defn is-space-original?
  [text]
  (= (count (string/trim text)) 0))

(defn is-space?
  [text]
  (= (count (string/trim (string/replace text "(^\\h*)|(\\h*$)" ""))) 0))

(defn render-tagged-tokens
  "Render tokens as term output, one per line.

  E.G.:

      new O
      triterpene B-CHEMICAL
      as O
      1α B-CHEMICAL
      , I-CHEMICAL
      3β I-CHEMICAL
      , I-CHEMICAL
      25-trihydroxy-9(11)-ene-16-one-9 I-CHEMICAL
      , I-CHEMICAL
      10-seco-9 I-CHEMICAL
      , I-CHEMICAL
      19-cyclolanostane I-CHEMICAL
      (1) O
      along O "
  [text tagged-tokenlist]
  (let [sentencelist (chem.sentences/make-sentence-list text)
        sentence-end-set (reduce (fn [newset sentence]
                                   (conj newset (-> sentence :span :end)))
                                   #{} sentencelist)]
    (string/join "\n"
                 (map (fn [token]
                        (if (contains? sentence-end-set (-> token :span :end))
                          (str (:text token) " " (:output token) "\n") ;; is at end of sentence?
                          (str (:text token) " " (:output token))))
                      (filter #(not (is-space? (:text %))) ; remove any space tokens including thinspace
                              tagged-tokenlist)))))

(defn render-cem-feature-files
  "Render training files to dest-dir using records from
  training-records and spans from training-cem-gold-map."
  [dest-dir training-records training-cem-gold-map]
  (dorun 
   (map (fn [document]
          (with-open [wtr (io/writer (format "%s/%s.fv" dest-dir (:docid document)))]
            (.write wtr
                    (format "%s\n"
                            (render-tagged-tokens
                             (:title document)
                             (tag-tokens-using-spans
                              (:title document)
                              (cem->spanlist
                               (filter-section
                                (training-cem-gold-map (:docid document))
                                "T"))))))
            (.write wtr
                    (format "%s\n"
                            (render-tagged-tokens
                             (:abstract document)
                             (tag-tokens-using-spans
                              (:abstract document)
                              (cem->spanlist
                               (filter-section
                                (training-cem-gold-map (:docid document))
                                "A"))))))))
        training-records)))

(defn render-annotation-feature-files
  "Render training files to dest-dir using records from
  training-records and spans from training-cem-gold-map."
  [dest-dir training-records training-docid-annotation-list-map]
  (dorun 
   (map (fn [document]
          (with-open [wtr (io/writer (format "%s/%s.fv" dest-dir (:docid document)))]
            (.write wtr
                    (format "%s\n"
                            (render-tagged-tokens
                             (:title document)
                             (tag-tokens-using-annotations
                              (:title document)
                               (filter-location
                                (training-docid-annotation-list-map (:docid document))
                                "T")))))
            (.write wtr
                    (format "%s\n"
                            (render-tagged-tokens
                             (:abstract document)
                             (tag-tokens-using-annotations
                              (:abstract document)
                               (filter-location
                                (training-docid-annotation-list-map (:docid document))
                                "A")))))))
        training-records)))

;; finite state machine for consolidating features from svm or deep
;; learning recognizers.  Uses reduce-fsm

(defn handle-event
  [acc evt & _]
  ;;(println (format "acc: %s, event: %s" acc evt))
  acc)

(defn add-token
  [acc evt & _]
  ;;(println (format "acc: %s, event: %s" acc evt))
  (assoc acc :accum (conj (:accum acc) evt)))

(defn emit-term-reset-add-token
  "emit accumulated term, reset accumulator, add current token to
  accumulator."
  [acc evt & _]
  ;;(println (format "acc: %s, event: %s" acc evt))
  (assoc acc :accum (list evt) :sequence (conj (:sequence acc) (:accum acc))))

(defn emit-term-reset
  "emit accumulated term, reset accumulator, add current token to
  accumulator."
  [acc evt & _]
  ;;(println (format "acc: %s, event: %s" acc evt))
  (assoc acc :accum [] :sequence (conj (:sequence acc) (:accum acc))))

(defn is-b-element?
  [[state event]]
  (= (nth (:output event) 0) \B))

(defn is-i-element?
  [[state event]]
  (= (nth (:output event) 0) \I))

(defn is-o-element?
  [[state event]]
  (= (nth (:output event) 0) \O))

(fsm/defsm bio-fsm
  [[:start
    [_ :guard is-b-element?] -> {:action add-token} :s1
    [_ :guard is-o-element?] -> {:action handle-event} :start]
   [:s1                                             
    [_ :guard is-b-element?] -> {:action emit-term-reset-add-token} :s1
    [_ :guard is-i-element?] -> {:action add-token} :s1
    [_ :guard is-o-element?] -> {:action emit-term-reset} :start]]
  :default-acc {:accum [] :sequence []}
  :dispatch :event-acc-vec
  )

(defn merge-annotations
  "Merge annotations in list into one annotation."
  [text annotation-list]
  (let [start (-> annotation-list first :span :start)
        end   (-> annotation-list last :span :end)]
  (hash-map
   :text (subs text start end)
   :location (:location (first annotation-list))
   :span {:start start
          :end end}
   :output (subs (:output (first annotation-list)) 2)
   :mallet (string/join "|" (map #(:mallet %) annotation-list))
   :origin annotation-list
  )))

(defn convert-bio-annotations
  "Given original input text and tokens tagged by machine learning system,
  convert multi-token B-I... annotations into combined one-token
  annotations, preserving one-token B- annotations.  remove B prefix
  in output names.  I.E. B-<type> becomes <type>."
  [text mapped-tokenlist]
  (let [{accum :accum sequence :sequence} (bio-fsm mapped-tokenlist)
        final-sequence (if (empty? accum)
                         sequence
                         (conj sequence accum))]
    (map #(merge-annotations text %)
         final-sequence)))
    
  
