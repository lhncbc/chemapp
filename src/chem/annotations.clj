(ns chem.annotations
  (:use [clojure.set])
  (:require [clojure.string :as string]))

(defn annotate-text-using-spans
  "Given document and spans annotate document for display.
   variable 'spans' is of the form:

      [{:start begin0, :end end0}
       {:start begin1, :end end1} ...]"
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

(defn annotate-text-using-annotations
  "Given document and spans annotate document for display.
   variable 'annotations' is of the form:

      [{:span {:start begin0, :end end0} :trigger-key foo}
       {:span {:start begin1, :end end1}} ...]"
  [text annotations trigger-key begintag0 endtag0 begintag1 endtag1 eoltag]
  (let [spans (map #(:span %) annotations)
        startset (set (map #(:start %) spans))
        endset (set (map #(:end %) spans))
        triggerset (reduce (fn [newset annot]
                             (if (contains? annot trigger-key)
                               (conj newset (:start (:span annot)))
                               newset)) #{} annotations)]
    (loop [i 0 
           text text
           result (vec "")] 
      (if (< i (count text))
        (let [ch (.charAt text i)
              rch (cond 
                   (contains? startset i) (if (contains? triggerset i)
                                            (str begintag0 ch)
                                            (str begintag1 ch))
                   (contains? endset i) (if (contains? triggerset i) 
                                          (str endtag0 ch)
                                          (str endtag1 ch))
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

(defn gen-record-map-by-docid
  ^{:doc "Generate a map of records keyed by docid from recordlist."}
 [recordlist]
  (into {} 
        (map #(vec (list (:docid %) %))
             recordlist)))


(defn annotate-record
  "Annotate record using supplied function, tag annotations with user supplied engine keyword."
  [engine-keyword func record]
  (conj record
        (hash-map engine-keyword
                  (hash-map :title-result 
                            (func (:title record))
                            :abstract-result 
                            (func (:abstract record))))))

(defn generate-nested-discard-set
  "Generate set of annotations to be discarded because of begin nested
  inside another annotation."
  [annotation-list]
  (let [sorted-annotation-list (vec (sort-by #(-> % :span :start) annotation-list))]
    (reduce (fn [newset [e-annot & targets]]
              (clojure.set/union newset 
                                 (set 
                                  (map #(:span %)
                                       (filter (fn [t-annot]
                                                 (and (>= (-> t-annot :span :start)
                                                          (-> e-annot :span :start))
                                                      (<= (-> t-annot :span :end)
                                                          (-> e-annot :span :end))))
                                               targets)))))
            #{} (concat (partition 4 1 sorted-annotation-list)
                        (partition 2 1 (take-last 4 sorted-annotation-list))))))

(defn remove-nested-annotations
  "Remove annotations nested inside another annotation."
  [annotation-list]
  (let [discardset (generate-nested-discard-set annotation-list)]
    (filter #(and (not (nil? %))
                  (not (contains? discardset (-> % :span))))
            annotation-list)))
