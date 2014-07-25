(ns chem.metamap-annotation
  (:use [clojure.set])
  (:require [clojure.string :as string])
  (:require [chem.metamap-api :as metamap-api])
  (:require [chem.new-metamap-api :as new-metamap-api])
  (:require [chem.semtypes :as semtypes])
  (:require [chem.annotations :as annotlib])
  (:require [chem.metamap-tokenization :as tokenization])
  (:require [chem.stopwords :as stopwords])
  (:require [chem.mti-filtering :as mti-filtering])
  (:require [chem.span-utils :as span-utils])
  (:require [migration.mwi-utilities :as mwi-utilities]))

;; Generate annotations from MetaMap API results

(defn gen-acronym-map [annotationlist]
  ^{:doc "Generate a map of acronyms and expansions keyed by both." }
  (reduce (fn [acronym-map annotation]
            (if (contains? annotation :acronym)
              (assoc acronym-map
                (mwi-utilities/normalize-meta-string (annotation :acronym)) annotation
                (mwi-utilities/normalize-meta-string (annotation :expansion)) annotation)
              acronym-map))
          {} annotationlist))
              
(defn get-matched-words-maps-by-semtypelist
  ^{:doc " Keep matched words for results with semantic types in
           supplied semantic type list." }
 [annotation-list semtypelist]
  (let [semtypeset (set semtypelist)
        acronym-map (gen-acronym-map annotation-list)]
    (set (flatten
          (filter #(not (nil? %))           ;remove any nil elements
                  (map #(if (empty? (intersection semtypeset (set (:semtypes %))))
                          nil
                          (hash-map :terms (string/join " " (:matchedwords %))
                                    :semtypes (:semtypes %)
                                    :conceptid (:conceptid %)
                                    :preferredname (:preferredname %)))
                       annotation-list))))))

(defn get-matched-words-by-semtypelist
  ^{:doc " Keep matched words for results with semantic types in
           supplied semantic type list." }
 [annotation-list semtypelist]
  (let [semtypeset (set semtypelist)
        acronym-map (gen-acronym-map annotation-list)]
    (set (flatten
          (filter #(not (nil? %))           ;remove any nil elements
                  (map #(if (empty? (intersection semtypeset (set (:semtypes %))))
                          nil
                          (string/join " " (:matchedwords %)))
                       annotation-list))))))

(defn filter-matched-words-by-chem-relations [annotation-list]
  (filter #(not (empty? %))           ;remove any empty lists
          (map (fn [annotation]
                 (cond 
                  (contains? annotation :cuilist) (if (> (count (filter mti-filtering/is-chemical-cui? (annotation :cuilist))) 0)
                                                    [(annotation :acronym)]
                                                    [])
                  (contains? annotation :conceptid) (if (mti-filtering/is-chemical-cui? (annotation :conceptid))
                                                     [(:matchedwords annotation)]
                                                     [])))
               annotation-list)))

(defn build-matchedwordset
  "Build set of matchwords from matchedword elements of candidates in
   annotation list."
  [annotation-list]
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

(defn build-matchedwordset-chemicals
  "Build set of matchwords from matchedword elements of candidates in
   annotation list filtered by semantic types in chemical semantic
   types list."
  [annotation-list]
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

(defn annotation-map
  "Generate map of annotations by term."
  [annotation-list]
  (reduce (fn [newmap annotation] 
            (let [term (string/join " " (:matchedwords annotation))]
              (assoc newmap term annotation)))
          {} annotation-list))

(defn get-positions [annotation-list]
  "get positional info from annotations"
  (map #(first (:position %))
       annotation-list))

(defn get-mm-spans 
  "Get spans from metamap annotations and target semantic types."
  [annotation-list semtypelist]
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

(defn gen-text-from-mm-annotation [annotation]
)

(defn gen-span-from-mm-positional-info [annotation]
  ^{:doc "Generate span from MetaMap positional information." }
  (filter #(not (nil? %))
          (when (contains? annotation :position)
            (let [position (first (:position annotation))]
              {:start (:start position) :end (+ (:start position) (:length position))}))))

(defn annotate-document
  "Generate annotations from document text."
  [mmapi-inst document-text]
  (let [result (metamap-api/handle-result-list
                (metamap-api/process-utf8-string mmapi-inst document-text "-C"))]
    (hash-map :annotations result)))

(defn filter-document-annotations [annotation-list semtypelist]
  ^{:doc "Keep annotations that have semantic-types in semtypelist." }
  (let [semtypeset (set semtypelist)]
    (filter #(not (empty? %))           ;remove any empty lists
            (map (fn [annotation]
                   (if (contains? annotation :semtypes)
                     (if (empty? (intersection semtypeset (set (:semtypes annotation))))
                       '()
                       annotation)
                     annotation ))      ;keep acronyms and negex 
                 annotation-list))))
  
(defn filter-metamap-result-list
  " Filter result list for document keeping highest scoring results,
    and removing results that are in the stopword list.`"
  [result-list]
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
                                               #{"carb" "opco" "orch" }) )
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


(defn filter-short-annotations [annotationlist]
  "Remove any annotations that are not useful to MetaMap.  This
includes punctuation, numbers, words of length 1."
  (filter (fn [annotation]
            (and (> (count (:text annotation)) 1)
                 (not= (:class annotation) "pn")))
            annotationlist))

(defn get-enhanced-annotations [mmapi annotationlist]
  "Query MetaMap using term processing to determine concepts for
   annotation texts for document."
  (map (fn [annotation]
         (let [mm-annotationlist 
                (metamap-api/process-utf8-string mmapi (:text annotation) "-zC")
               mappings-list (first (:mappings-list (first mm-annotationlist)))]
           (if (empty? mm-annotationlist)
             annotation
             (conj annotation
                   {:conceptlist (map #(:conceptid %) mappings-list)}
                   {:semtypelist (map #(:semtypes %) mappings-list)}))))
       (filter-short-annotations annotationlist)))

(defn update-metamap-annotations [document-struct-map]
  (conj document-struct-map 
        (hash-map :metamap
                  (hash-map
                   :title-result 
                   (hash-map :annotations
                             (map #(if (contains? % :position)
                                     (conj % {:span (gen-span-from-mm-positional-info %)})
                                     %)
                                  (:annotations
                                   (:title-result
                                    (:metamap document-struct-map)))))
                   :abstract-result
                   (hash-map :annotations 
                             (map #(if (contains? % :position)
                                     (conj % {:span (gen-span-from-mm-positional-info %)})
                                     %)
                                  (:annotations
                                   (:abstract-result
                                    (:metamap document-struct-map)))))))))

(defn filter-ev-by-chem-relations
  "remove ev elements that don't satisfy is-chemical-cui?"
 ([ev-list cui-func]
  (filter (fn [el]
            (and (contains? el :conceptid)
                 (cui-func (el :conceptid))))
          ev-list))
 ([ev-list]
     (filter-ev-by-chem-relations ev-list mti-filtering/is-chemical-cui?)))

(defn keep-ev-elements [resultlist]
  (filter #(contains? % :position) resultlist))

(defn get-spans-from-ev-elements [resultlist]
  "Get a list of spans from positional information in ev elements. "
  (apply concat
         (map (fn [el]
                (map (fn [pos-el]
                       (hash-map :start (:start pos-el)
                                 :end   (+ (:start pos-el) (:length pos-el))))
                     (el :position)))
              resultlist)))

(defn expand-ev-extents [resultlist input-text]
  "determine if extents found by metamap are a sub-extent of a large extent in input text"
  (map (fn [span]
         (span-utils/find-bounds-of-string input-text (span :start)))
  (get-spans-from-ev-elements resultlist)))
