(ns chem.chemdner-tools
  (:import (java.io BufferedReader FileReader FileWriter))
  (:gen-class))


(defn load-chemdner-abstracts
  "## 1) chemdner_abs_training.txt : Training set abstracts

   This file contains plain-text, UTF8-encoded PubMed abstracts in a 
   tab-separated format with the following three columns:

   1. Article identifier (PMID, PubMed identifier)
   2. Title of the article
   3. Abstract of the article"
 [filename]
  (doall
   (map 
    (fn [line]
      (let [fields (vec (.split line "\t"))]
        (hash-map :docid    (java.lang.Integer/parseInt (nth fields 0)) ; use read-string instead?
                  :title    (nth fields 1)
                  :abstract (nth fields 2))))
    (line-seq (BufferedReader. (FileReader. filename))))))

(defn mongodb-abstract-fields-recfn
  "Each input record is of form

      [id title abstract]

   all are strings.

   Records are loaded as:

       {:docid id :title title :abstract abstract}

   Use command below to load abstracts into mongodb:

       (chem.mongodb/load-record-seq abstract-seq :tablename )
       (require '[somnium.congomongo :as m])
       (m/add-index! :tablename [:recid])"
  [fields]
  (hash-map :docid    (java.lang.Integer/parseInt (nth fields 0)) ; use read-string instead?
            :title    (nth fields 1)
            :abstract (nth fields 2)))

(defn mongodb-hash-recfn [record]
  record)

;; ## 3) Training data annotations: 
;;
;; chemdner\_ann\_training\_13-07-31.txt
;;
;; This file contains manually generated annotations of chemical
;; entities of the training dataset.
;;
;; It consists of tab-separated fields containing:
;;
;; 1. Article identifier (PMID)
;; 2. Type of text from which the annotation was derived (T: Title, A: Abstract)
;; 3. Start offset
;; 4. End offset
;; 5. Text string of the entity mention
;; 6. Type of chemical entity mention
;;    (ABBREVIATION,FAMILY,FORMULA,IDENTIFIERS,MULTIPLE,SYSTEMATIC,TRIVIAL)

(defn load-chemdner-annotations [filename]
  "Load Training data annotations."
  (map 
   (fn [line]
     (let [fields (vec (.split line "\t"))]
       {:docid (java.lang.Integer/parseInt (nth fields 0))
        :location  (nth fields 1)
        :start (java.lang.Integer/parseInt (nth fields 2))
        :end   (java.lang.Integer/parseInt (nth fields 3))
        :text  (nth fields 4)
        :chemtype  (nth fields 5)}))
   (line-seq (BufferedReader. (FileReader. filename)))))

;; ## 4) Training data Gold Standard file for the Chemical document
;; indexing (CDI) sub-task: cdi\_ann\_training\_13-07-31.txt
;;
;; Given a set of documents, for this subtask, the participants are
;; asked to return for each of them a ranked list of chemical entities
;; described within each of these documents. You are not required to
;; provide the specific type of chemical entity class.
;;
;; It consists of tab-separated fields containing:
;;
;; 1. Article identifier (PMID)
;; 2. Text string of the entity mention
;;
;; An example is shown below:
;;
;;     22258629	malondialdehyde
;;     22288603	mikanolide

(defn load-training-document-entity-gold-standard
  "Load gold standard for training document entities."
 [filename]
  (map 
   (fn [line]
     (let [fields (vec (.split line "\t"))]
       {:docid (java.lang.Integer/parseInt (nth fields 0))
        :term (nth fields 1)}))
   (line-seq (BufferedReader. (FileReader. filename)))))

;; ## 5) Training data Gold Standard file for the Chemical entity mention
;;    recognition (CEM) sub-task: cem\_ann\_training\_13-07-31.txt
;;
;; Given a set of documents, for this subtask, the participants have
;; to return the start and end indices corresponding to all the
;; chemical entities mentioned in this document. You are not requested
;; to provide the specific type of chemical entity subclass.
;;
;; It consists of tab-separated columns containing:

;; 1. Article identifier (PMID)
;; 2. Offset string consisting in a triplet joined by the ':'
;;    character. You have to provide the text type (T: Title,
;;    A:Abstract), the start offset and the end offset.
;;
;; An example illustrating the format is shown below:
;;
;;     21826085	A:946:957
;;     22080034	A:1072:1081
;;     22080034	A:1305:1314
;;     22080034	A:1383:1392
;;     22080034	A:190:199

(defn load-training-mention-gold-standard
  "Load gold standard for training mentions."
 [filename]
  (map 
   (fn [line]
     (let [fields         (.split line "\t")
           pmid           (nth fields 0) 
           offset-triplet (vec (.split (nth fields 1) "\\:"))]
       {:pmid (java.lang.Integer/parseInt pmid)
        :offset-triplet offset-triplet}))
   (line-seq (BufferedReader. (FileReader. filename)))))

(defn gen-training-records-map
  "Generate map of training records by docid."
  [training-records]
  (reduce (fn [newmap el]
            (assoc newmap (:docid el) el))
          {} training-records))

(defn gen-training-annotations-map
  "build map of mti or training annotations by docid"
  [annotation-list]
  (reduce (fn [newmap el]
            (let [docid (:docid el)
                  term  (:term el)]
              (if (contains? newmap docid)
                (assoc newmap docid (conj (newmap docid) term))
                (assoc newmap docid (set (list term))))))
          {} annotation-list))

(defn gen-chemdner-training-gold-map [chemdner-training-cdi-gold-list]
  (gen-training-annotations-map chemdner-training-cdi-gold-list))

(defn gen-record-map [records] 
  (into {} (map #(vec (list (:docid %) %)) records)))

(defn add-chemdner-gold-annotations
  ^{:doc "Add chemdner gold standard annotation for document using document's docid."}
  [gold-doc-term-map document]
  (conj document 
        (hash-map :chemdner-gold-standard (gold-doc-term-map (document :docid)))))

(defn add-gold-standard-to-records [training-records chemdner-training-cdi-gold-map]
  (map #(add-chemdner-gold-annotations chemdner-training-cdi-gold-map %)
       training-records))

(defn gen-termset-from-chemdner-gold-standard [records]
  (set (apply concat (map #(:chemdner-gold-standard %) records))))

;; ## Additional comments

;; To evaluate the performance of your system we recommend you to use the
;; BioCreative evaluation library scripts. You can also directly download 
;; it from the BioCreative Resources page at:

;; http://www.biocreative.org/resources/biocreative-ii5/evaluation-library/

;; This webpage explains in detail how to install the library and how it works. 

;; For both of the tasks you should use the --INT evaluation option like shown below:

;; bc-evaluate --INT prediction\_file evaluation\_file

;; As the --INT option is chosen by default, you can also run this script without the
;; argument:

;; bc-evaluate prediction\_file evaluation\_file

;; Example evaluation files for both subtasks were described above.


;; ## A) Prediction format for the CDI subtask

;; Please make sure that your predictions are compliant with the formatting information provided for the --INT option of the evaluation library (The webpage and the bc-evaluate -h and bc-evaluate -d option provide you with more details.)

;; In short, you have to provide a tab-separated file with:

;; 1. Article identifier
;; 2. The chemical entity mention string
;; 3. The rank of the chemical entity returned for this document
;; 4. A confidence score

;; Example cases are provided online in the CHEMDNER sample set (June 25, 2013)

;; (http://www.biocreative.org/resources/corpora/bc-iv-chemdner-corpus/#bc-iv-chemdner-corpus:downloads)

;; An example prediction for the sample set is shown below:

;;     6780324	LHRH	1	0.9
;;     6780324	FSH	2	0.857142857143
;;     6780324	3H2O	3	0.75
;;     6780324	(Bu)2cAMP	4	0.75
;;     6780324	vitro	5	0.666666666667
;;     6780324	plasminogen	6	0.5
;;     6780324	ethylamide	7	0.5
;;     6780324	beta-3H]testosterone	8	0.5
;;     6780324	NIH-FSH-S13	9	0.5
;;     6780324	D-Ser-(But),6	10	0.5
;;     6780324	4-h	11	0.5
;;     6780324	3-isobutyl-l-methylxanthine	12	0.5
;;     2231607	thymidylate	1	0.666666666667
;;     2231607	acid	2	0.666666666667
;;     2231607	TS	3	0.666666666667

;; ## B) Prediction format for the CEM subtask

;; Please make sure that your predictions are compliant with the formatting information provided for the --INT option of the evaluation library (The webpage and the bc-evaluate -h and bc-evaluate -d option provide you with more details.)

;; In short, you have to provide a tab-separated file with:

;; 1. Article identifier (PMID)
;; 2. Offset string consisting in a triplet joined by the ':' character. You have to provide the text type (T: Title, A:Abstract), the start offset and the end offset.
;; 3. The rank of the chemical entity returned for this document
;; 4. A confidence score

;; Example from the sample set (from June 25, 2013) is shown below:

;;     6780324	A:104:107	1	0.5
;;     6780324	A:1136:1147	2	0.5
;;     6780324	A:1497:1500	3	0.5
;;     6780324	A:162:167	4	0.5
;;     6780324	A:17:21	5	0.5
;;     6780324	A:319:330	6	0.5
;;     6780324	A:448:452	7	0.5

;;     (def training-filename (str chem.paths/training-dir "/chemdner_abs_training.txt"))
;;     (def training-records (chem.chemdner-tools/load-chemdner-abstracts training-filename))
;;     (def training-record-map (into {} (map #(vec (list (:docid %) %)) training-records)))
;;     (def training-annot-filename (str chem.paths/training-dir "/chemdner_ann_training_13-07-31.txt"))
;;     (def training-annotations (chem.chemdner-tools/load-chemdner-annotations training-annot-filename))
;;     (def development-filename (str chem.paths/training-dir "/chemdner_abs_development.txt"))
;;     (def development-records (chem.chemdner-tools/load-chemdner-abstracts development-filename))


(defn gen-term-attribute-map 
  "Populate chemical attribute map with chemical type attributes
  obtained from training-set annotations."
  [chemdner-annotation-list]
  (reduce (fn [newmap record]
            (let [{start :start
                   end :end
                   text :text
                   type :chemtype} record]
              (if (contains? newmap text)
                (assoc newmap text (assoc (newmap text) text {:start start
                                                              :end end
                                                              :type type}))
                (assoc newmap text {:start start 
                                    :end end
                                    :type type}))))
          {} chemdner-annotation-list))

(defn gen-docid-term-attribute-map 
  "Generate a map of docid -> term -> attributes from chemdner-annotation-list."
  [chemdner-annotation-list]
  (reduce (fn [newmap record]
            (let [{docid :docid
                   location :location
                   start :start
                   end :end
                   text :text
                   type :chemtype} record]
              (if (contains? newmap docid)
                (assoc newmap docid 
                       (conj (newmap docid)
                               (assoc (newmap docid) text 
                                      (if (contains? (newmap docid) text)
                                        (conj ((newmap docid) text)
                                              {:location location 
                                               :start start
                                               :end end
                                               :type type})
                                        (list {:location location
                                               :start start
                                               :end end
                                               :type type})))))
                (assoc newmap docid {text (list {:location location
                                           :start start 
                                           :end end
                                           :chemtype type})}))))
          {} chemdner-annotation-list))

(defn gen-docid-span-start-attribute-map
  "Generate a map of docid -> span-start -> attributes from chemdner-annotation-list."
  [chemdner-annotation-list]
  (reduce (fn [newmap record]
            (let [{docid :docid
                   location :location
                   start :start
                   end :end
                   text :text
                   type :chemtype} record]
              (if (contains? newmap docid)
                (assoc newmap docid 
                       (conj (newmap docid)
                               (assoc (newmap docid) start
                                      (if (contains? (newmap docid) start)
                                        (conj ((newmap docid) start)
                                              {:location location 
                                               :start start
                                               :end end
                                               :text text
                                               :type type})
                                        (list {:location location
                                               :start start
                                               :end end
                                               :text text
                                               :type type})))))
                (assoc newmap docid {start (list {:location location
                                                  :start start 
                                                  :end end
                                                  :text text
                                                  :chemtype type})}))))
          {} chemdner-annotation-list))
