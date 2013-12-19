(ns chem.metamap-annotation
  (:use [clojure.set])
  (:require [clojure.string :as string])
  (:require [chem.metamap-api :as mm-api])
  (:require [chem.semtypes :as semtypes])
  (:require [chem.annotations :as annotlib])
  (:require [chem.metamap-tokenization :as tokenization])
  (:require [chem.stopwords :as stopwords]) )

;; Generate annotations from MetaMap API results

(defn get-matched-words-by-semtypelist [annotation-list semtypelist]
  " Keep matched words for results with semantic types in supplied semantic type list."
  (let [semtypeset (set semtypelist)]
    (filter #(not (empty? %))           ;remove any empty lists
            (map #(if (empty? (intersection semtypeset (set (:semtypes %))))
                    '()
                    (list (:semtypes %) (:matchedwords %)))
                 annotation-list))))

(defn build-matchedwordset [annotation-list]
  "Build set of matchwords from matchedword elements of candidates in
   annotation list."
  (set (flatten (map #(string/join " " (:matchedwords %)) annotation-list))))

(defn build-matchedwordset-chemicals-v1 [annotation-list]
  (let [chemtypeset (set semtypes/chemical-semantic-type-list)]
    (set (flatten (map (fn [annotation]
                         (if (> (count (intersection
                                        chemtypeset
                                        (set (:semtypes annotation)))) 0)
                           (string/join " " (:matchedwords annotation))
                           ""))
                       annotation-list)))))

(defn build-matchedwordset-chemicals [annotation-list]
  "Build set of matchwords from matchedword elements of candidates in
   annotation list filtered by semantic types in chemical semantic
   types list."
  (set
   (map 
    #(string/join " " (second %))
    (get-matched-words-by-semtypelist annotation-list semtypes/chemical-semantic-type-list))))

(defn build-concept-map [annotation-list]
  (reduce (fn [newmap annotation]
            (let [conceptid (:conceptid annotation)
                  term (string/join " " (:matchedwords annotation))]
              (assoc newmap term conceptid)))
       annotation-list))

(defn annotation-map [annotation-list]
  "map of annotations by term"
  (reduce (fn [newmap annotation] 
            (let [term (string/join " " (:matchedwords annotation))]
              (assoc newmap term annotation)))
          {} annotation-list))

(defn get-positions [annotation-list]
  (map #(first (:position %))
       annotation-list))

(defn get-mm-spans [annotation-list semtypelist]
  "get spans from metamap annotations and target semantic types."
  (let [semtypeset (set semtypelist)]
    (filter #(not (empty? %))           ;remove any empty lists
            (map (fn [annotation]
                   (if (contains? annotation :semtypes)
                     (if (empty? (intersection semtypeset (set (:semtypes annotation))))
                       '()
                       (let [position (first (:position annotation))]
                         {:start (:start position) :end (+ (:start position) (:length position))}))
                     '()))
                 annotation-list))))

(defn annotate-document [mmapi-inst document]
  "Generate annotations from document text."
  (let [mm-annotations (mm-api/handle-result-list 
                        (mm-api/process-string 
                         mmapi-inst
                         document))
        spans (get-mm-spans
               mm-annotations
               semtypes/chemical-semantic-type-list)
        matched-terms (build-matchedwordset-chemicals
                       mm-annotations)]
    (hash-map :spans spans
              :annotations mm-annotations
              :matched-terms matched-terms)))
  
(defn filter-metamap-result-list [result-list]
  " Filter result list for document keeping highest scoring results,
    and removing results that are in the stopword list.`"
  (sort-by 
   #(nth % 3)
   (filter 
    #(not (contains? stopwords/stopwords (nth % 1)))
    (vals 
     (reduce (fn [rmap result]
               (let [key (nth result 1)]
                 (if (contains? rmap key) 
                   (if (< (nth result 3) (nth (rmap key) 3)) ; MetaMap score are negative (Agh!)
                     (assoc rmap key result)
                     (assoc rmap key (rmap key)))
                   (assoc rmap key result))))
             {} result-list)))))

(defn keep-chemical-annotations [annotation-list]
  (filter #(not (nil? %))
          (map (fn [annotation]
                 (when (not= #{} (intersection (set (annotation :semtypes))
                                               #{"opco" "orch" }) )
                  annotation))
               annotation-list)))

(defn keep-mapping-annotations [annotation-list]
  "keep only mappings, discard acronym and negation annotations (for now)."
  (filter #(not (nil? %))
          (map (fn [annotation]
                 (when (contains? annotation :conceptname)
                   annotation))
               annotation-list)))

(defn gen-ranked-list-from-result [metamap-result]
  (let [docid (metamap-result :docid)]
    (filter-metamap-result-list 
     (map-indexed 
      (fn [idx annotation]
        [docid (string/join " " (:matchedwords annotation)) (inc idx) (:score annotation)])
      (keep-chemical-annotations
       (keep-mapping-annotations (:annotations (metamap-result :abstract-result))))))))


