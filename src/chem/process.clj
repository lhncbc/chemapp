(ns chem.process
  (:require [chem.metamap-api :as mm-api])
  (:require [chem.annotations :as annot])
  (:require [chem.metamap-annotation :as mm-annot])
  (:require [chem.metamap-tokenization :as mm-tokenization])
  (:require [chem.semtypes :as semtypes])
  (:require [chem.partial :as partial])
  (:require [chem.partial-enhanced :as partial-enhanced])
  (:require [chem.normchem :as normchem])
  (:require [chem.combine-recognizers :as combinelib])
  (:require [chem.mongodb :as chemdb])
  (:require [clojure.set])
  (:gen-class))

(defn process [engine document]
  ^{:doc "Generate annotations from document text."}
  (case engine
    "metamap" (let [mmapi-inst (mm-api/api-instantiate)]
                (mm-annot/annotate-document mmapi-inst document))
    "token"  (mm-tokenization/gen-token-annotations document)
    "token-enhanced" (let [mmapi-inst (mm-api/api-instantiate)]
                       (mm-annot/get-enhanced-annotations
                        mmapi-inst 
                        (:annotations (mm-tokenization/gen-token-annotations document))))
    "partial" (partial/match document)
    "prefix-or-suffix" (partial/prefix-or-suffix-match document)
    "partial-enhanced" (let [mmapi-inst (mm-api/api-instantiate)]
                         (partial-enhanced/match mmapi-inst document))
    "partial-token-enhanced" (let [mmapi-inst (mm-api/api-instantiate)]
                               (partial-enhanced/match2 mmapi-inst document))
    "partial-token-filtered"  (let [mmapi-inst (mm-api/api-instantiate)]
                               (partial-enhanced/filter-match2 mmapi-inst document))
    "fragment" (partial/fragment-match document)
    "normchem" (normchem/process-document document)
    "combine1" (combinelib/combination-1 document)
    ))

(defn process-document-seq [engine document-seq]
    "Generate annotations from raw document sequence text."
  (case engine
    "metamap" (let [mmapi-inst (mm-api/api-instantiate)]
                (map (fn [document]
                       (mm-annot/annotate-document mmapi-inst document))
                     document-seq))
    "partial"  (map partial/match     document-seq)
    "partial-enhanced" (let [mmapi-inst (mm-api/api-instantiate)]
                         (map  (fn [document]
                                 (partial-enhanced/match mmapi-inst document))
                               document-seq))
    "partial-token-enhanced" (let [mmapi-inst (mm-api/api-instantiate)]
                         (map  (fn [document]
                                 (partial-enhanced/match2 mmapi-inst document))
                               document-seq))
    "fragment" (map partial/fragment-match    document-seq)
    "normchem" (map normchem/process-document document-seq)
    "combine1" (map combinelib/combination-1  document-seq)))

;;
;; Chemdner functions
;;

(defn annotate-chemdner-document
  ([annotate-fn document]
     (hash-map :title-result    (annotate-fn  (:title document))
               :abstract-result (annotate-fn  (:abstract document))))
  ([annotate-fn engine document]
     (hash-map (keyword engine)
               (annotate-chemdner-document annotate-fn document))))

(defn add-chemdner-gold-annotations [gold-doc-term-map document]
  ^{:doc "Add chemdner gold standard annotation for document using document's docid."}
  (conj document 
        (hash-map :chemdner-gold-standard (gold-doc-term-map (document :docid)))))

(defn add-token-annotations [document]
  ^{:doc "Add token annotations to document." }
    (conj document 
          (hash-map :token (annotate-chemdner-document mm-tokenization/gen-token-annotations document))))

(defn add-metamap-annotations
  ^{:doc "Annotate document record using MetaMap."}
 ([mmapi-inst engine document]
  (conj document
        (hash-map (keyword engine)
                  (hash-map :title-result    (mm-annot/annotate-document mmapi-inst (:title document))
                            :abstract-result (mm-annot/annotate-document mmapi-inst (:abstract document))))))
 ([mmapi-inst document]
    (add-metamap-annotations mmapi-inst "metamap" document)))


(comment
(defn regenerate-metamap-spans
  "Annotate documentmap using MetaMap."
 ([documentmap engine]
    (conj documentmap
          (hash-map (keyword engine)
                    (hash-map :title-result 
                              (mm-annot/regenerate-spans-and-matched-terms 
                               (:annotations (:title-result (documentmap (keyword engine)))))
                              :abstract-result 
                              (mm-annot/regenerate-spans-and-matched-terms 
                               (:annotations (:abstract-result (documentmap (keyword engine)))))))))
 ([documentmap]
    (regenerate-metamap-spans documentmap "metamap")))
)

(defn add-partial-match-annotations 
  ^{:doc "Annotate document using chemical partial match."}
  [document]
  (conj document 
        (annotate-chemdner-document partial/prefix-or-suffix-match "partial" document)))

(defn add-token-annotations 
  ^{:doc "Annotate document using metamap-style tokenization."}
  [document]
  (conj document 
        (annotate-chemdner-document mm-tokenization/gen-token-annotations "token" document)))

(defn add-normchem-annotations 
  ^{:doc "Add normchem annotations to document."}
  [document]
  (conj document 
        (annotate-chemdner-document normchem/process-document "normchem" document)))

(defn filter-partial-match-using-normchem [document]
 ^{:doc "Keep partial-match annotations that are represented in
         normalized supplementary chemical database." }
 (conj document 
       (hash-map :partial-normchem
                 (hash-map :title-result
                           (hash-map :annotations
                                     (filter
                                      #(not (nil? (% :cstring)))
                                      (map
                                       (fn [annotation] 
                                         (let [result 
                                               (into
                                                {} 
                                                (normchem/nmslookup "nmsnormchem" (:text annotation)))]
                                           (if result 
                                             (merge annotation {:meshid (result "meshid")
                                                                :pubchemid (result "pubchemid")
                                                                :cstring (result "cstring")
                                                                :ncstring (result "ncstring")})
                                             nil)))
                                       (annot/list-annotations :partial :title-result document))))
                           :abstract-result
                           (hash-map :annotations
                                     (filter
                                      #(not (nil? (% :cstring)))
                                      (map
                                       (fn [annotation] 
                                         (let [result 
                                               (into
                                                {} 
                                                (normchem/nmslookup "nmsnormchem" (:text annotation)))]
                                           (if result 
                                             (merge annotation {:meshid (result "meshid")
                                                                :pubchemid (result "pubchemid")
                                                                :cstring (result "cstring")
                                                                :ncstring (result "ncstring")})
                                             nil)))
                                       (annot/list-annotations :partial :abstract-result document))))))))

(defn add-normchem-annotations-removing-udas [document]
  "Create new document with new normchem result with User defined
   acronyms removed from matching-terms element."
  (if (contains? document :acronyms)
    (let [acronym-set (set (map (fn [el]
                                  (:acronym el))
                                (:acronyms document)))
          normchem-document (annotate-chemdner-document normchem/process-document "normchem" document)
          normchem-annotset (:normchem normchem-document)]
      (conj document 
            (hash-map :normchem
                      (conj normchem-annotset
                            (hash-map :title-result
                                      (conj (:title-result normchem-annotset)
                                            (hash-map :matched-terms 
                                                      (filter #(not (contains? acronym-set %)) 
                                                              (:matched-terms (:title-result normchem-annotset))))))
                            (hash-map :abstract-result
                                      (conj (:abstract-result normchem-annotset)
                                            (hash-map :matched-terms 
                                                      (filter #(not (contains? acronym-set %)) 
                                                              (:matched-terms (:abstract-result normchem-annotset))))))))))
    document))


(defn merge-matched-terms [matched-term-keyword keyword-list document]
  "combine matched-term sets of selected annotators in keyword-list"
  (conj document 
        (hash-map matched-term-keyword
                  (apply clojure.set/union 
                         (map (fn [annotator]
                                (clojure.set/union 
                                 (:matched-terms (:title-result (document annotator)))
                                 (:matched-terms (:abstract-result (document annotator)))))
                              keyword-list)))))

(defn collect-acronyms [document]
  (conj document 
        (hash-map
         :acronyms
         (if (contains? document :metamap)
           (concat
            (filter #(contains? % :acronym)
                    (:annotations (:title-result (:metamap document))))
            (filter #(contains? % :acronym)
                    (:annotations (:abstract-result (:metamap document)))))))))

(defn generate-matched-terms [engine-keyword-list document]
  (reduce (fn [newdocument engine-keyword]
            (conj newdocument 
                  (hash-map engine-keyword 
                            (conj (document engine-keyword)
                                  (hash-map :title-result
                                            (conj (:title-result (document engine-keyword))
                                                  (hash-map :matched-terms
                                                            (annot/get-matched-terms-from-annotations 
                                                             (:annotations (:title-result (document engine-keyword))))))
                                            :abstract-result
                                            (conj (:abstract-result (document engine-keyword))
                                                  (hash-map :matched-terms
                                                            (annot/get-matched-terms-from-annotations 
                                                             (:annotations (:abstract-result (document engine-keyword)))))))))))
          document engine-keyword-list))

(defn generate-mm-matched-terms 
  ([document]
     (generate-mm-matched-terms document :metamap))
  ([document engine-keyword]
     (conj document 
           (hash-map engine-keyword 
                     (conj (document engine-keyword)
                           (hash-map :title-result
                                     (conj (:title-result (document engine-keyword))
                                           (hash-map :matched-terms 
                                                     (clojure.set/union 
                                                      (mm-annot/get-matched-words-by-semtypelist
                                                       (:annotations (:title-result (engine-keyword document)))
                                                       semtypes/chemical-semantic-type-list))))
                                     :abstract-result
                                     (conj (:abstract-result (document engine-keyword))
                                           (hash-map :matched-terms
                                                     (mm-annot/get-matched-words-by-semtypelist
                                                      (:annotations (:abstract-result (engine-keyword document)))
                                                      semtypes/chemical-semantic-type-list)))))))))

(defn flow1 [mm-annotated-document]
  "Preconditions: document must have been annotated by functions
   add-metamap-annotations and add-partial-match-annotations.

   1. Apply metamap to documents filtering by semantic types
      keep terms
      keep User defined acronyms (UDAs)
   2. Apply partial match
   3. Apply normchem and remove any terms that are UDAs.
   "
  (merge-matched-terms :flow1-matched-terms [:partial :metamap]
                       (generate-matched-terms [:partial :normchem]
                                               (-> mm-annotated-document 
                                                   collect-acronyms
                                                   add-normchem-annotations-removing-udas
                                                   generate-mm-matched-terms))))

(defn map-flow1
  "Use flow1 on documents in document sequence previously annotated by
  function add-metamap-annotations."
  [mm-annotated-document-seq]
  (chemdb/init)                         ; connect to "chem" mongodb database
  (map flow1 mm-annotated-document-seq))

(defn subsume-flow 
  "subsume any term span inside another term span"
  ([annotated-document]
     (subsume-flow annotated-document [:partial :normchem]))
  ([annotated-document annotator-list]
     (conj 
      annotated-document
      (hash-map 
       :subsume-matched-terms
       (set (concat 
             (chem.span-utils/realize-spans 
              (:title annotated-document)
              (chem.span-utils/subsume-spans
               (chem.span-utils/concat-spans annotated-document annotator-list :title-result)))
             (chem.span-utils/realize-spans 
              (:abstract annotated-document)
              (chem.span-utils/subsume-spans
               (chem.span-utils/concat-spans annotated-document annotator-list :abstract-result)))))))))

(defn enchilada0-flow [annotated-document]
  (subsume-flow annotated-document [:token-opsin :partial-normchem :partial-opsin]))

(defn map-flow
  [annotated-document-seq flow-func]
  (map flow-func
       annotated-document-seq))

(defn add-gold-standard-to-document-seq
  "Add gold standard annotations to each document in document sequence."
  [gold-term-map document-seq]
  (map add-chemdner-gold-annotations
       document-seq))

(defn add-mm-concepts-to-annotations 
  ^{:doc "Augment existing annotations using MetaMap."}
  ([mmapi-inst document annotation-keyword new-keyword]
     (let [annot-result (document annotation-keyword)]
       (conj document 
             (hash-map
              (keyword new-keyword)
              (hash-map
               :title-result
               (hash-map
                :annotations
                (mm-annot/get-enhanced-annotations
                 mmapi-inst    (-> annot-result :title-result :annotations)))
               :abstract-result
               (hash-map
                :annotations
                (mm-annot/get-enhanced-annotations
                 mmapi-inst    (-> annot-result :abstract-result :annotations))))))))
  ([mmapi-inst document annotation-keyword]
     (add-mm-concepts-to-annotations 
      mmapi-inst document
      annotation-keyword (keyword (str (name annotation-keyword) "-with-mm-annotations")))))

(defn mm-annotate-results 
  "Augment annotations with MetaMap."
  [token-results]
  (let [mmapi-inst (mm-api/api-instantiate)]
    (map (fn [result]
           (hash-map :docid           (:docid result)
                     :title-result    (hash-map :annotations 
                                                (mm-annot/get-enhanced-annotations
                                                 mmapi-inst 
                                                 (:annotations (:title-result result))))
                     :abstract-result (hash-map :annotaions
                                                (mm-annot/get-enhanced-annotations
                                                 mmapi-inst
                                                 (:annotations (:abstract-result result))))
                     :method (str (:method result) "-enhanced")))
         
         token-results)))

(defn process-chemdner-document [engine document]
  (conj document 
        (hash-map (keyword engine)
                  (case engine
                    "metamap" (let [mmapi-inst (mm-api/api-instantiate)]
                                (hash-map :title-result    (mm-annot/annotate-document mmapi-inst (:title document))
                                          :abstract-result (mm-annot/annotate-document mmapi-inst (:abstract document))))
                    "token" (annotate-chemdner-document mm-tokenization/gen-token-annotations engine document)
                    "token-enhanced" (let [mmapi-inst (mm-api/api-instantiate)]
                                       (annotate-chemdner-document 
                                        (fn [a-document]
                                          (mm-annot/get-enhanced-annotations
                                           mmapi-inst 
                                           (:annotations (mm-tokenization/gen-token-annotations a-document))))
                                        document))
                    "partial"  (annotate-chemdner-document partial/match document)
                    "partial-enhanced" (let [mmapi-inst (mm-api/api-instantiate)]
                                         (hash-map :title-result    (partial-enhanced/match mmapi-inst (:title document))
                                                   :abstract-result (partial-enhanced/match mmapi-inst (:abstract document))))
                    "partial-token-enhanced" (let [mmapi-inst (mm-api/api-instantiate)]
                                               (hash-map :title-result    (partial-enhanced/match2 mmapi-inst (:title document))
                                                         :abstract-result (partial-enhanced/match2 mmapi-inst (:abstract document))))
                    "partial-token-filtered" (let [mmapi-inst (mm-api/api-instantiate)]
                                               (hash-map :title-result    (partial-enhanced/filter-match2 mmapi-inst (:title document))
                                                         :abstract-result (partial-enhanced/filter-match2 mmapi-inst (:abstract document))))
                    "fragment" (annotate-chemdner-document partial/fragment-match document)
                    "normchem" (annotate-chemdner-document normchem/process-document document)
                    "combine1" (annotate-chemdner-document combinelib/combination-1 document)))))

(defn process-chemdner-document-seq [engine document-seq]
  "Generate annotations from structured document sequence text.
     Document elements contain fields mapped by the keywords :docid, :title, :abstract."
  (case engine
    "metamap" (let [mmapi-inst (mm-api/api-instantiate)]
                (map (fn [document]
                       (conj document
                             (hash-map (keyword engine)
                                       (hash-map :title-result    (mm-annot/annotate-document mmapi-inst (:title document))
                                                 :abstract-result (mm-annot/annotate-document mmapi-inst (:abstract document))))))
                     document-seq))
    "token" (map #(annotate-chemdner-document mm-tokenization/gen-token-annotations engine %) document-seq)
    "token-enhanced" (let [mmapi-inst (mm-api/api-instantiate)]
                       (map #(conj % (annotate-chemdner-document 
                                             (fn [document]
                                               (mm-annot/get-enhanced-annotations
                                                mmapi-inst 
                                                (:annotations (mm-tokenization/gen-token-annotations document))))
                                             engine %)) document-seq))
    "partial"  (map #(conj % (annotate-chemdner-document partial/match engine %)) document-seq)
    "partial-enhanced" (let [mmapi-inst (mm-api/api-instantiate)]
                         (map (fn [document]
                                (conj document
                                      (hash-map :title-result    (partial-enhanced/match mmapi-inst (:title document))
                                                :abstract-result (partial-enhanced/match mmapi-inst (:abstract document))
                                                :method          engine)))
                              document-seq))
    "partial-token-enhanced" (let [mmapi-inst (mm-api/api-instantiate)]
                               (map (fn [document]
                                      (conj document
                                            (hash-map :title-result    (partial-enhanced/match2 mmapi-inst (:title document))
                                                      :abstract-result (partial-enhanced/match2 mmapi-inst (:abstract document))
                                                      :method          engine)))
                                    document-seq))
    "partial-token-filtered" (let [mmapi-inst (mm-api/api-instantiate)]
                               (map (fn [document]
                                      (conj document 
                                            (hash-map (keyword engine)
                                                      (hash-map :title-result    (partial-enhanced/filter-match2 mmapi-inst (:title document))
                                                                :abstract-result (partial-enhanced/filter-match2 mmapi-inst (:abstract document))))))
                                    document-seq))
    "fragment" (map #(conj % (annotate-chemdner-document partial/fragment-match engine %)) document-seq)
    "normchem" (doall (map #(conj % (annotate-chemdner-document normchem/process-document engine %)) document-seq))
    "combine1" (doall (map #(conj % (annotate-chemdner-document combinelib/combination-1 engine %)) document-seq))))


;; (def partial-token-filtered-result (chem.process/process-chemdner-document-seq "partial-token-filtered" training-records))


