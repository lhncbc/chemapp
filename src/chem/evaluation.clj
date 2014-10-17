(ns chem.evaluation
  (:use [clojure.set])
  (:require [clojure.string :as string])
  (:require [chem.annotations :as annot]))

;; simple evaluation tools

(defn terms-wrong [goldset termset]
  (difference termset goldset))

(defn terms-missing [goldset termset]
  (difference goldset termset))

(defn terms-right [goldset termset]
  (intersection goldset termset))

(defn add-wrong-missing [document terms-keyword]
  (conj document 
        (hash-map :wrong (terms-wrong (document :chemdner-gold-standard) 
                                      (document terms-keyword))
                  :missing (terms-missing (document :chemdner-gold-standard) 
                                          (document terms-keyword))
                  :right (terms-right (document :chemdner-gold-standard) 
                                      (document terms-keyword)))))

(defn add-wrong-missing-engine-list [document engine-list]
  (conj document 
        (hash-map :wrong (terms-wrong (document :chemdner-gold-standard) 
                                      (set (annot/list-matched-terms-from-engine-list engine-list document)))
                  :missing (terms-missing (document :chemdner-gold-standard) 
                                          (set (annot/list-matched-terms-from-engine-list engine-list document)))
                  :right (terms-right (document :chemdner-gold-standard) 
                                      (set (annot/list-matched-terms-from-engine-list engine-list document))))))

(defn map-terms [document-list terms-keyword]
  "Build map by docid of missing. wrong, gold, and result terms using
   list documents containing result terms label by terms-keyword."
  (reduce  (fn [newmap document]
                (assoc newmap 
                  (document :docid)
                  (add-wrong-missing document terms-keyword)))
          {} document-list))

(defn get-annotation-termlist [record engine-keyword]
  (set 
   (concat 
    (map #(:text %) (-> record engine-keyword :abstract-result :annotations))
    (map #(:text %) (-> record engine-keyword :title-result :annotations)))))

(defn doc-terms-to-chemdner-result
  [doc-result terms-keyword]
  (map-indexed (fn [idx term]
                 [(:docid doc-result) term (inc idx) "1.0"])
               (set (doc-result terms-keyword))))

(defn doc-engine-result-to-chemdner-result
  [doc-result engine-keyword]
  (map-indexed (fn [idx term]
                 [(:docid doc-result) term (inc idx) "1.0"])
               (set (annot/list-matched-terms engine-keyword doc-result))))

(defn doc-engine-list-result-to-chemdner-result
  [doc-result engine-keyword-list]
  (map-indexed (fn [idx term]
                 [(:docid doc-result) term (inc idx) "1.0"])
               (set (annot/list-matched-terms-from-engine-list engine-keyword-list doc-result))))

(defn docid-termlist-to-chemdner-result [docid termlist]
  (map-indexed (fn [idx term]
                 [docid term (inc idx) "1.0"])
               (set termlist)))

(defn get-terms-from-method [record method-keyword]
  (set (concat (annot/get-matched-terms-from-annotations (:annotations (:title-result (method-keyword record))))
               (annot/get-matched-terms-from-annotations (:annotations (:abstract-result (method-keyword record)))))))

(defn get-spans-from-method [record method-keyword]
  (set (concat (annot/get-spans-from-annotations (:annotations (:title-result (method-keyword record))))
               (annot/get-spans-from-annotations (:annotations (:abstract-result (method-keyword record)))))))

(defn get-annotations-from-method [record method-keyword]
  (set (concat (:annotations (:title-result (method-keyword record)))
               (:annotations (:abstract-result (method-keyword record))))))


(defn result-list-to-chemdner-resultlist [records matched-terms-keyword]
  (apply concat (map #(doc-terms-to-chemdner-result % matched-terms-keyword) records)))

(defn engine-result-list-to-chemdner-result [records engine-keyword]
  (apply concat (map #(doc-engine-result-to-chemdner-result % engine-keyword) records)))

(defn engine-list-result-list-to-chemdner-result [records engine-keyword-list]
  (apply concat (map #(doc-engine-list-result-to-chemdner-result % engine-keyword-list) records)))

(defn write-chemdner-resultlist-to-file [filename chemdner-resultlist]
  (with-open [w (java.io.FileWriter. filename)]
    (dorun (map #(.write w (str (string/join "\t" %) "\n"))
                chemdner-resultlist))))

;; convert to chemdner format
(defn gen-flow1-chemdner-resultlist [flow1-records]
  (doall
   (apply concat
          (map (fn [record]
                 (docid-termlist-to-chemdner-result 
                  (:docid record)
                  (:flow1-matched-terms record)))
               flow1-records))))

(defn gen-metamap-chemdner-resultlist [metamap-records]
  (doall
   (apply concat
          (map (fn [record]
                 (docid-termlist-to-chemdner-result 
                  (:docid record)
                  (set (concat
                        (annot/get-matched-terms-from-annotations (-> record :metamap :title-result :annotations))
                        (annot/get-matched-terms-from-annotations (-> record :metamap :abstract-result :annotations))))))
               metamap-records))))

(defn gen-partial-chemdner-resultlist [partial-match-records]
  (doall
   (apply concat
          (map (fn [record]
                 (docid-termlist-to-chemdner-result 
                  (:docid record)
                  (set (concat (annot/get-matched-terms-from-annotations (:annotations (:title-result (:partial record))))
                               (annot/get-matched-terms-from-annotations (:annotations (:abstract-result (:partial record))))))))
               partial-match-records))))

(defn gen-metamap-partial-chemdner-resultlist [metamap-partial-records]
  (doall
   (apply concat
          (map (fn [record]
                 (docid-termlist-to-chemdner-result 
                  (:docid record)
                  (set (concat (annot/get-matched-terms-from-annotations (:annotations (:title-result (:partial record))))
                               (annot/get-matched-terms-from-annotations (:annotations (:abstract-result (:partial record))))
                               (annot/get-matched-terms-from-annotations (:annotations (:title-result (:metamap record))))
                               (annot/get-matched-terms-from-annotations (:annotations (:abstract-result (:metamap record))))))))
               metamap-partial-records))))

(defn gen-metamap-partial-subsume-chemdner-resultlist [metamap-partial-subsume-records]
  (doall
   (apply concat
          (map (fn [record]
                 (docid-termlist-to-chemdner-result 
                  (:docid record)
                  (set (:subsume-matched-terms record))))
               metamap-partial-subsume-records))))

(defn gen-chemdner-resultlist
  [annotated-record-list engine-keyword]
  (apply concat
         (map #(docid-termlist-to-chemdner-result
                (:docid %)
                (get-annotation-termlist % engine-keyword))
              annotated-record-list)))

