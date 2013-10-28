(ns chem.annotations
  (:use [clojure.set])
  (:require [clojure.string :as string])
  (:require [chem.semtypes :as semtypes])
  (:require [chem.metamap-tokenization :as tokenization]))

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

(defn annotate-text [text spans begintag endtag eoltag]
  "Given document and spans annotate document for display.
   variable 'spans' is of the form [[begin0 end0] [begin1 end1] ...]"
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
