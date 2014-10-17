(ns chem.pipeline
  (:use [chem.utils])
  (:use [chem.paths])
  (:use [chem.chemdner-tools])
  (:require [clojure.data.json :as json])
  (:require [clojure.string :as string])
  (:require [chem.process :as process])
  (:require [chem.metamap-annotation :as mm-annot])
  (:gen-class))

;; element format

(defn annotate-document [document engine]
  "Annotate supplied document with specified annotation engine.
   (annotate-document (nth document-seq 0) \"normchem\") "
  (hash-map :docid           (document :docid)
            :title-result    (process/process engine (:title document))
            :abstract-result (process/process engine (:abstract document))
            :method          engine))

(defn annotate-document-set [engine document-seq]
  (process/process-chemdner-document-seq engine document-seq))

(defn gen-scored-list-from-result [result]
  "return list of [doc annotation score"
  (let [docid (result :docid)]
    (vec (set 
          (map (fn [annotation]
                 [docid (:text  annotation) 0.5])
               (:annotations (result :abstract-result)))))))

(defn gen-ranked-list-from-result [result]
  "Generate chemdner result records from annotation result.
   Currently, scoring is missing."
  (let [docid (result :docid)]
    (map-indexed (fn [idx el]
                   [docid (nth el 1) (inc idx) (nth el 2)])
                 (gen-scored-list-from-result result))))

(defn gen-ranked-list-from-result-original [result]
  "Generate chemdner result records from annotation result.
   Currently, scoring is missing."
  (let [docid (result :docid)]
        (map-indexed (fn [idx annotation]
                       [docid (:text  annotation) (inc idx) 0.5])
                     (:annotations (result :abstract-result)))))

(defn gen-chemdner-result-list 
  ([result-list]
     (apply concat (map gen-ranked-list-from-result result-list)))
  ([engine result-list]
     (case engine
       "metamap"  (apply concat (map mm-annot/gen-ranked-list-from-result result-list))
       "normchem" (apply concat (map gen-ranked-list-from-result                  result-list))
     )))
      

(defn format-chemdner-result-element
  ([docid term rank score]
     (format "%d\t%s\t%d\t%f\n" docid term rank score))
  ([el]
     (let [docid (nth el 0)
           term (nth el 1)
           rank (nth el 2)
           score (nth el 3)]
       (format "%d\t%s\t%d\t%f" docid term rank score))))

(defn write-chemdner-result-element [w docid term rank score]
  "format: docid \t chemical \t rank \t confidence-score

   example:

       6780324  LHRH        1   0.9
       6780324  FSH         2   0.857142857143
       6780324  3H2O        3   0.75
       6780324  (Bu)2cAMP   4   0.75"
  (.write w (format "%d\t%s\t%d\t%f\n" 
                    docid term rank score)))

;; 
;;    user> (def result-list (chem.pipeline/process chemdner-dev-abstracts))
;;    #'user/result-list
;; 
(defn write-json-annotation [w annotation]
  (.write w (format "%s\n" (json/write-str annotation))))

(defn serialize-annotation-list [annotation-list]
  (map #(json/write-str %) annotation-list))

(defn write-annotations-to-file [outfilename annotation-list]
  (chem.utils/write-elements outfilename (serialize-annotation-list annotation-list)))

(defn read-annotations-from-file [infilename]
  (map #(json/read-str % :key-fn keyword)
       (chem.utils/line-seq-from-file infilename)))


;;    (def training-filename (str chem.paths/training-dir "/chemdner_abs_training.txt"))
;;    (def training-records (chem.chemdner-tools/load-chemdner-abstracts training-filename))
;;
;;
;;    (def partial-abs-annotation-list (chem.pipeline/annotate-document-set "partial" training-records))
;;    (write-annotations-to-file "partial-abs-annotations.json" partial-abs-annotation-list)
;;    (def partial-chemdner-result-list (chem.pipeline/gen-chemdner-result-list partial-abs-annotation-list))
;;    (chem.utils/write-elements "partial-chemdner-result-list.txt"
;;       (map chem.pipeline/format-chemdner-result-element partial chemdner-result-list))

;;    (def normchem-abs-annotation-list (chem.pipeline/annotate-document-set "normchem" training-records))
;;    (write-annotations-to-file "normchem-abs-annotations.json" normchem-abs-annotation-list)
;;    (def normchem-chemdner-result-list (chem.pipeline/gen-chemdner-result-list normchem-abs-annotation-list))
;;    (chem.utils/write-elements "normchem-chemdner-result-list.txt"
;;      (map chem.pipeline/format-chemdner-result-element normchem-chemdner-result-list))

;;    (def metamap-abs-annotation-list (chem.pipeline/annotate-document-set  "metamap" training-records))
;;    (write-annotations-to-file "metamap-abs-annotations.json" metamap-abs-annotation-list)
;;    (def metamap-chemdner-result-list (chem.pipeline/gen-chemdner-result-list "metamap" metamap-abs-annotation-list))
;;    (chem.utils/write-elements "metamap-chemdner-result-list.txt"
;;      (map chem.pipeline/format-chemdner-result-element metamap-chemdner-result-list))

;;    (def abs-annotation-list (chem.pipeline/read-annotations-from-file "abs-annotations.json"))
