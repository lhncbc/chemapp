(ns chem.annotations
  (:use [clojure.set])
  (:require [clojure.string :as string]))

(defn annotate-text
  "Given document and spans annotate document for display.
   variable 'spans' is of the form:

        [{:start begin0, :end end0} {:start begin1, :end end1} ...]"
  [text spans begintag endtag eoltag]
  (let [startset (set (map #(:start %) spans))
        endset (set (map #(:end %) spans))]
    (loop [i 0 
           text text
           result (vec "")] 
      (if (< i (count text))
        (let [ch (.charAt text i)
              rch (cond 
                 (contains? startset i) (str begintag ch)
                 (contains? endset i) (str endtag ch)
                 (= ch \newline) (str ch eoltag)
                 :else ch)]
          (recur (inc i) text (conj result rch)))
        (string/join result)))))


(defn get-spans-from-annotations [annotationlist]
  (sort-by #(:start %)
           (set
            (flatten
             (map (fn [annotation]
                    (annotation :span))
                  annotationlist)))))

(defn get-matched-terms-from-annotations [annotationlist]
  (set
   (map (fn [annotation]
          (annotation :text))
        annotationlist)))

(defn get-terms-from-annotations [annotationlist]
  (set
   (map (fn [annotation]
          (annotation :text))
        annotationlist)))

(defn list-matched-terms 
  ([engine-keyword result-keyword docmap]
     (get-matched-terms-from-annotations
      (:annotations (result-keyword (engine-keyword docmap)))))
  ([engine-keyword docmap]
     (union 
      (get-matched-terms-from-annotations
       (:annotations (:title-result (engine-keyword docmap))))
      (get-matched-terms-from-annotations
       (:annotations (:abstract-result (engine-keyword docmap)))))))

(defn list-annotations
  ([engine-keyword result-keyword docmap]
     (:annotations (result-keyword (engine-keyword docmap))))
  ([engine-keyword docmap]
     (union (:annotations (:title-result (engine-keyword docmap)))
             (:annotations (:abstract-result (engine-keyword docmap))))))

(defn list-spans [engine-keyword result-keyword docmap]
  (get-spans-from-annotations
   (:annotations (result-keyword (engine-keyword docmap)))))

(defn list-matched-terms-from-engine-list
  [engine-keyword-list docmap]
  (apply concat 
         (map (fn [engine-keyword]
                (list-matched-terms engine-keyword docmap))
              engine-keyword-list)))
  
(defn fix-partial-normchem-result [document-struct-map]
  (let [resultmap (:partial-normchem document-struct-map)]
    (conj document-struct-map 
          (hash-map :partial-normchem
                    (hash-map
                     :title-result
                     (hash-map :annotations (resultmap :title-result))
                     :abstract-result
                     (hash-map :annotations (resultmap :abstract-result)))))))


(defn fix-metamap-result [document-struct-map]
  (let [resultmap (:metamap-fixed document-struct-map)]
    (conj document-struct-map 
          (hash-map :metamap
                    (hash-map :title-result
                              (hash-map :annotations
                                        (resultmap :title-result))
                              :abstract-result
                              (hash-map :annotations
                                        (resultmap :abstract-result)))))))

(defn gen-record-map-by-docid [recordlist]
  ^{:doc "Generate a map of records keyed by docid from recordlist."}
  (into {} 
        (map #(vec (list (:docid %) %))
             recordlist)))


(defn annotate-record [engine-keyword func record]
  "Annotate record using supplied function, tag annotations with user supplied engine keyword."
  (conj record
        (hash-map engine-keyword
                  (hash-map :title-result 
                            (func (:title record))
                            :abstract-result 
                            (func (:abstract record))))))