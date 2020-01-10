(ns chem.process
  (:require [clojure.java.io :as io]
            ;; [metamap-api.metamap-api :as mm-api]
            [chem.annotations :as annot]
            ;; [chem.metamap-annotation :as mm-annot]
            [skr.tokenization :as mm-tokenization]
            [chem.semtypes :as semtypes]
            [chem.partial :as partial]
            [chem.partial-enhanced :as partial-enhanced]
            [chem.irutils-normchem :as irutils-normchem]
            [chem.lucene-normchem :as lucene-normchem]
            [chem.combine-recognizers :as combinelib]
            [clojure.set :refer [union intersection]]
            [chem.mallet-ner :as mallet-ner]
            [chem.cdi :refer [write-cdi-result]]
            [chem.brat :refer [write-brat]]
            [chem.utils :as utils])
  (:gen-class))

(defonce bad-engines 
  {:prefix-or-suffix {:func (fn [document]
                              (let [result (partial/prefix-or-suffix-match document)]
                                (conj result {:spans (map #(:span %) (:annotations result))})))
                      :label "IUPAC Prefix or Suffix Match"}
   ;; :metamap {:func (fn [document]
   ;;                   (let [mmapi-inst (mm-api/api-instantiate)]
   ;;                     (mm-annot/annotate-document mmapi-inst document)))
   ;;           :label "MetaMap"}
   })

;; (defonce disabled-engines
;;  {
   ;; :mongodb-normchem {:func (fn [document] (normchem/process-document document))
   ;;            :label "Normalized Chemical Match (MongoDB)"}
   ;; :partial-enhanced {:func (fn [document]
   ;;                            (let [mmapi-inst (mm-api/api-instantiate)]
   ;;                              (partial-enhanced/match mmapi-inst document)))
   ;;                    :label "Partial Chemical Match Enhanced"}
   ;; :partial-token-enhanced {:func (fn [document]
   ;;                                  (let [mmapi-inst (mm-api/api-instantiate)]
   ;;                                    (partial-enhanced/match2 mmapi-inst document)))
   ;;                          :label "Partial Token Chemical Match Enhanced"}
   ;; :partial-token-filtered {:func (fn [document]
   ;;                                  (let [mmapi-inst (mm-api/api-instantiate)]
   ;;                                    (partial-enhanced/filter-match2 mmapi-inst document)))
   ;;                          :label "Partial Chemical Token Filtered"}
   ;; :token-enhanced {:func (fn [document]
   ;;                          (let [mmapi-inst (mm-api/api-instantiate)]
   ;;                            (mm-annot/get-enhanced-annotations
   ;;                             mmapi-inst 
   ;;                             (:annotations (mm-tokenization/gen-token-annotations document)))))
   ;;                  :label "Token Enchanced"}
;;   })

(def ^:dynamic *engine-map*
  (sorted-map
   :irutils {:func (fn [document] (irutils-normchem/process-document document))
                     :label "Normalized Chemical Match (irutils)"}
   :lucene {:func (fn [document] (lucene-normchem/process-document document))
                     :label "Normalized Chemical Match (Lucene)"}
   :token   {:func (fn [document]
                    (mm-tokenization/gen-token-annotations document))
             :label "Token"}
   :partial  {:func partial/process-document
              :label "Partial Chemical Match"}
   :fragment {:func (fn [document] (partial/fragment-match document))
              :label "Fragment"}
   :combine1 {:func combinelib/combination-1
              :label "Normalized Chemical Match (Lucene) plus Partial Chemical Match"}
   :combine2 {:func combinelib/combination-2
              :label "Normalized Chemical Match (IRUtils) plus Partial Chemical Match"}
   :combine3 {:func combinelib/combination-3
              :label "Normalized Chemical Match (Lucene) plus Mallet NER"}
   :combine4 {:func combinelib/combination-4
              :label "Normalized Chemical Match (Lucene) plus Partial Chemical Match plus Mallet NER"}
   :combine5 {:func combinelib/combination-5
              :label "Normalized Chemical Match (IRUtils) plus Mallet NER"}
   :combine6 {:func combinelib/combination-6
              :label "Normalized Chemical Match (IRUtils) plus Mallet NER (entityrec)"}
   :mallet   {:func mallet-ner/process-document
              :label "Mallet NER"}
   ))

(defn add-engine
  "Add an engine to engine mapping and rebind map *engine-map*."
  ([name func]
     (add-engine name func ""))
  ([name func label]
     (def ^:dynamic *engine-map*
       (assoc *engine-map* (keyword name) (hash-map :func func :label label)))))

(defn process
  "Generate annotations from document text."
  [^String engine ^String document]
  (when (contains? *engine-map* (keyword engine))
    ((-> engine keyword *engine-map* :func) document)))

(defn get-engine-label
 [engine]
 (when (contains? *engine-map* (keyword engine))
   (-> engine keyword *engine-map* :label)))

(defonce ^:dynamic *doc-seq-engine-map*
  {
   ;; :metamap (fn [document-seq]
   ;;            (let [mmapi-inst (mm-api/api-instantiate)]
   ;;              (map (fn [document]
   ;;                     (mm-annot/annotate-document mmapi-inst document))
   ;;                   document-seq)))
   :partial  (fn [document-seq]
               (map partial/match     document-seq))
   ;; :partial-enhanced (fn [document-seq]
   ;;                     (let [mmapi-inst (mm-api/api-instantiate)]
   ;;                       (map  (fn [document]
   ;;                               (partial-enhanced/match mmapi-inst document))
   ;;                             document-seq)))
   ;; :partial-token-enhanced (fn [document-seq]
   ;;                           (let [mmapi-inst (mm-api/api-instantiate)]
   ;;                             (map  (fn [document]
   ;;                                     (partial-enhanced/match2 mmapi-inst document))
   ;;                                   document-seq)))
   :fragment (fn [document-seq] (map partial/fragment-match    document-seq))
   :lucene   (fn [document-seq] (map lucene-normchem/process-document document-seq))
   :irutils  (fn [document-seq] (map irutils-normchem/process-document document-seq))
   :normchem (fn [document-seq] (map lucene-normchem/process-document document-seq))
   :combine1 (fn [document-seq] (map combinelib/combination-1  document-seq))
   :combine2 (fn [document-seq] (map combinelib/combination-2  document-seq))
})

(defn process-document-seq
  "Generate annotations from raw document sequence text."
  [engine document-seq]
  (if (contains? *doc-seq-engine-map* (keyword engine))
    ((*doc-seq-engine-map* (keyword engine)) document-seq)))

;;
;; Chemdner functions
;;

(defn annotate-chemdner-document
  ([annotate-func document]
   (hash-map :title-result    (if (contains? document :title) (annotate-func  (:title document)) #{})
             :abstract-result (if (contains? document :abstract) (annotate-func  (:abstract document)) #{})))
  ([annotate-func engine document]
     (hash-map engine
               (annotate-chemdner-document annotate-func document))))

(defn add-chemdner-gold-annotations [gold-doc-term-map document]
  ^{:doc "Add chemdner gold standard annotation for document using document's docid."}
  (conj document 
        (hash-map :chemdner-gold-standard (gold-doc-term-map (document :docid)))))

(defn add-token-annotations [document]
  ^{:doc "Add token annotations to document." }
    (conj document 
          (hash-map :token (annotate-chemdner-document mm-tokenization/gen-token-annotations document))))

;; (defn add-metamap-annotations
;;   ^{:doc "Annotate document record using MetaMap."}
;;  ([mmapi-inst engine document]
;;   (conj document
;;         (hash-map (keyword engine)
;;                   (hash-map :title-result    (mm-annot/annotate-document mmapi-inst (:title document))
;;                             :abstract-result (mm-annot/annotate-document mmapi-inst (:abstract document))))))
;;  ([mmapi-inst document]
;;     (add-metamap-annotations mmapi-inst "metamap" document)))


;; (defn regenerate-metamap-spans
;;   "Annotate documentmap using MetaMap."
;;  ([documentmap engine]
;;     (conj documentmap
;;           (hash-map (keyword engine)
;;                     (hash-map :title-result 
;;                               (mm-annot/regenerate-spans-and-matched-terms 
;;                                (:annotations (:title-result (documentmap (keyword engine)))))
;;                               :abstract-result 
;;                               (mm-annot/regenerate-spans-and-matched-terms 
;;                                (:annotations (:abstract-result (documentmap (keyword engine)))))))))
;;  ([documentmap]
;;     (regenerate-metamap-spans documentmap "metamap")))


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
        (annotate-chemdner-document lucene-normchem/process-document "normchem" document)))

(defn add-irutils-normchem-annotations 
  ^{:doc "Add normchem annotations to document."}
  [document]
  (conj document 
        (annotate-chemdner-document irutils-normchem/process-document "normchem" document)))

(defn add-lucene-normchem-annotations 
  ^{:doc "Add normchem annotations to document."}
  [document]
  (conj document 
        (annotate-chemdner-document lucene-normchem/process-document "normchem" document)))

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
                                                (lucene-normchem/nmslookup "nmsnormchem" (:text annotation)))]
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
                                                (lucene-normchem/nmslookup "nmsnormchem" (:text annotation)))]
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
          normchem-document (annotate-chemdner-document lucene-normchem/process-document "normchem" document)
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

;; (defn generate-mm-matched-terms 
;;   ([document]
;;      (generate-mm-matched-terms document :metamap))
;;   ([document engine-keyword]
;;      (conj document 
;;            (hash-map engine-keyword 
;;                      (conj (document engine-keyword)
;;                            (hash-map :title-result
;;                                      (conj (:title-result (document engine-keyword))
;;                                            (hash-map :matched-terms 
;;                                                      (clojure.set/union 
;;                                                       (mm-annot/get-matched-words-by-semtypelist
;;                                                        (:annotations (:title-result (engine-keyword document)))
;;                                                        semtypes/chemical-semantic-type-list))))
;;                                      :abstract-result
;;                                      (conj (:abstract-result (document engine-keyword))
;;                                            (hash-map :matched-terms
;;                                                      (mm-annot/get-matched-words-by-semtypelist
;;                                                       (:annotations (:abstract-result (engine-keyword document)))
;;                                                       semtypes/chemical-semantic-type-list)))))))))

;; (defn flow1 [mm-annotated-document]
;;   "Preconditions: document must have been annotated by functions
;;    add-metamap-annotations and add-partial-match-annotations.

;;    1. Apply metamap to documents filtering by semantic types
;;       keep terms
;;       keep User defined acronyms (UDAs)
;;    2. Apply partial match
;;    3. Apply normchem and remove any terms that are UDAs.
;;    "
;;   (merge-matched-terms :flow1-matched-terms [:partial :metamap]
;;                        (generate-matched-terms [:partial :normchem]
;;                                                (-> mm-annotated-document 
;;                                                    collect-acronyms
;;                                                    add-normchem-annotations-removing-udas
;;                                                    generate-mm-matched-terms))))

;; (defn map-flow1
;;   "Use flow1 on documents in document sequence previously annotated by
;;   function add-metamap-annotations."
;;   [mm-annotated-document-seq]
;;   (map flow1 mm-annotated-document-seq))

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

;; (defn add-mm-concepts-to-annotations 
;;   ^{:doc "Augment existing annotations using MetaMap."}
;;   ([mmapi-inst document annotation-keyword new-keyword]
;;      (let [annot-result (document annotation-keyword)]
;;        (conj document 
;;              (hash-map
;;               (keyword new-keyword)
;;               (hash-map
;;                :title-result
;;                (hash-map
;;                 :annotations
;;                 (mm-annot/get-enhanced-annotations
;;                  mmapi-inst    (-> annot-result :title-result :annotations)))
;;                :abstract-result
;;                (hash-map
;;                 :annotations
;;                 (mm-annot/get-enhanced-annotations
;;                  mmapi-inst    (-> annot-result :abstract-result :annotations))))))))
;;   ([mmapi-inst document annotation-keyword]
;;      (add-mm-concepts-to-annotations 
;;       mmapi-inst document
;;       annotation-keyword (keyword (str (name annotation-keyword) "-with-mm-annotations")))))

;; (defn mm-annotate-results 
;;   "Augment annotations with MetaMap."
;;   [token-results]
;;   (let [mmapi-inst (mm-api/api-instantiate)]
;;     (map (fn [result]
;;            (hash-map :docid           (:docid result)
;;                      :title-result    (hash-map :annotations 
;;                                                 (mm-annot/get-enhanced-annotations
;;                                                  mmapi-inst 
;;                                                  (:annotations (:title-result result))))
;;                      :abstract-result (hash-map :annotaions
;;                                                 (mm-annot/get-enhanced-annotations
;;                                                  mmapi-inst
;;                                                  (:annotations (:abstract-result result))))
;;                      :method (str (:method result) "-enhanced")))
         
;;          token-results)))

(defn process-chemdner-document-original
  [engine document]
  (conj document 
        (hash-map engine
                  (case engine
                    ;; :metamap (let [mmapi-inst (mm-api/api-instantiate)]
                    ;;             (hash-map :title-result    (mm-annot/annotate-document mmapi-inst (:title document))
                    ;;                       :abstract-result (mm-annot/annotate-document mmapi-inst (:abstract document))))
                    :token (annotate-chemdner-document mm-tokenization/gen-token-annotations engine document)
                    ;; :token-enhanced (let [mmapi-inst (mm-api/api-instantiate)]
                    ;;                    (annotate-chemdner-document 
                    ;;                     (fn [a-document]
                    ;;                       (mm-annot/get-enhanced-annotations
                    ;;                        mmapi-inst 
                    ;;                        (:annotations (mm-tokenization/gen-token-annotations a-document))))
                    ;;                     document))
                    :partial  (annotate-chemdner-document partial/match document)
                    ;; :partial-enhanced (let [mmapi-inst (mm-api/api-instantiate)]
                    ;;                      (hash-map :title-result    (partial-enhanced/match mmapi-inst (:title document))
                    ;;                                :abstract-result (partial-enhanced/match mmapi-inst (:abstract document))))
                    ;; :partial-token-enhanced (let [mmapi-inst (mm-api/api-instantiate)]
                    ;;                            (hash-map :title-result    (partial-enhanced/match2 mmapi-inst (:title document))
                    ;;                                      :abstract-result (partial-enhanced/match2 mmapi-inst (:abstract document))))
                    ;; :partial-token-filtered (let [mmapi-inst (mm-api/api-instantiate)]
                    ;;                            (hash-map :title-result    (partial-enhanced/filter-match2 mmapi-inst (:title document))
                                                         ;; :abstract-result (partial-enhanced/filter-match2 mmapi-inst (:abstract document))))
                    :fragment (annotate-chemdner-document partial/fragment-match document)
                    :normchem (annotate-chemdner-document lucene-normchem/process-document document)
                    :irutils-normchem (annotate-chemdner-document irutils-normchem/process-document document)
                    :combine1 (annotate-chemdner-document combinelib/combination-1 document)
                    :combine5 (annotate-chemdner-document combinelib/combination-5 document)
                    :combine6 (annotate-chemdner-document combinelib/combination-6 document)
))))

(defn process-chemdner-document-seq
  "Generate annotations from structured document sequence text.
     Document elements contain fields mapped by the keywords :docid, :title, :abstract."
   [engine document-seq]
  (case engine
    ;; :metamap (let [mmapi-inst (mm-api/api-instantiate)]
    ;;             (map (fn [document]
    ;;                    (conj document
    ;;                          (hash-map (keyword engine)
    ;;                                    (hash-map :title-result    (mm-annot/annotate-document mmapi-inst (:title document))
    ;;                                              :abstract-result (mm-annot/annotate-document mmapi-inst (:abstract document))))))
    ;;                  document-seq))
    :token (map #(annotate-chemdner-document mm-tokenization/gen-token-annotations engine %) document-seq)
    ;; :token-enhanced (let [mmapi-inst (mm-api/api-instantiate)]
    ;;                    (map #(conj % (annotate-chemdner-document 
    ;;                                          (fn [document]
    ;;                                            (mm-annot/get-enhanced-annotations
    ;;                                             mmapi-inst 
    ;;                                             (:annotations (mm-tokenization/gen-token-annotations document))))
    ;;                                          engine %)) document-seq))
    :partial  (map #(conj % (annotate-chemdner-document partial/match engine %)) document-seq)
    ;; :partial-enhanced (let [mmapi-inst (mm-api/api-instantiate)]
    ;;                      (map (fn [document]
    ;;                             (conj document
    ;;                                   (hash-map :title-result    (partial-enhanced/match mmapi-inst (:title document))
    ;;                                             :abstract-result (partial-enhanced/match mmapi-inst (:abstract document))
    ;;                                             :method          engine)))
    ;;                           document-seq))
    ;; :partial-token-enhanced (let [mmapi-inst (mm-api/api-instantiate)]
    ;;                            (map (fn [document]
    ;;                                   (conj document
    ;;                                         (hash-map :title-result    (partial-enhanced/match2 mmapi-inst (:title document))
    ;;                                                   :abstract-result (partial-enhanced/match2 mmapi-inst (:abstract document))
    ;;                                                   :method          engine)))
    ;;                                 document-seq))
    ;; :partial-token-filtered (let [mmapi-inst (mm-api/api-instantiate)]
    ;;                            (map (fn [document]
    ;;                                   (conj document 
    ;;                                         (hash-map (keyword engine)
    ;;                                                   (hash-map :title-result    (partial-enhanced/filter-match2 mmapi-inst (:title document))
    ;;                                                             :abstract-result (partial-enhanced/filter-match2 mmapi-inst (:abstract document))))))
    ;;                                 document-seq))
    :fragment (map #(conj % (annotate-chemdner-document partial/fragment-match engine %)) document-seq)
    :normchem (doall (map #(conj % (annotate-chemdner-document lucene-normchem/process-document engine %)) document-seq))
    :combine1 (doall (map #(conj % (annotate-chemdner-document combinelib/combination-1 engine %)) document-seq))
    :combine5 (doall (map #(conj % (annotate-chemdner-document combinelib/combination-5 engine %)) document-seq))
    :combine6 (doall (map #(conj % (annotate-chemdner-document combinelib/combination-6 engine %)) document-seq))
))


;; (def partial-token-filtered-result (process-chemdner-document-seq "partial-token-filtered" training-records))


(defn gen-metamap-partial-subsume-records [metamap-partial-records]
  (map-flow metamap-partial-records subsume-flow))

(def ^:dynamic *chemdner-engine-map*
  {:normchem {:func (fn [document engine]
                      (annotate-chemdner-document lucene-normchem/process-document engine document))}
   :irutils-normchem {:func (fn [document engine]
                      (annotate-chemdner-document irutils-normchem/process-document engine document))}
   :partial  {:func (fn [document engine]
                      (annotate-chemdner-document partial/match engine document))}
   :combine1 {:func (fn [document engine]
                      (annotate-chemdner-document combinelib/combination-1 engine document))}
   :combine2 {:func (fn [document engine]
                      (annotate-chemdner-document combinelib/combination-2 engine document))}
   :combine3 {:func (fn [document engine]
                      (annotate-chemdner-document combinelib/combination-3 engine document))}
   :combine5 {:func (fn [document engine]
                      (annotate-chemdner-document combinelib/combination-5 engine document))}
   :combine6 {:func (fn [document engine]
                      (annotate-chemdner-document combinelib/combination-6 engine document))}
})

(defn process-chemdner-document
 [engine document]
  (when (contains? *chemdner-engine-map* (keyword engine))
    ((-> engine keyword *chemdner-engine-map* :func) document engine)))

(def ^:dynamic *chemdner-index-engine-map*
  {:irutils-normchem {:func (fn [document engine]
                              (annotate-chemdner-document irutils-normchem/index-document engine document))}
   :combine5 {:func (fn [document engine]
                      (annotate-chemdner-document combinelib/combination-5 engine document))}
   :combine6 {:func (fn [document engine]
                      (annotate-chemdner-document combinelib/combination-6 engine document))}
   })

(defn index-chemdner-document
  [engine document]
  (let [result (when (contains? *chemdner-index-engine-map* (keyword engine))
                 ((-> engine keyword *chemdner-index-engine-map* :func) document engine))]
    (hash-map :docid (:docid document)
              :terms (union
                      (->> (get result (keyword engine))
                           :title-result)
                      (->> (get result (keyword engine))
                           :abstract-result)))))

(def ^:dynamic *index-engine-map*
  {:irutils-normchem {:func (fn [document engine]
                              (annotate-chemdner-document irutils-normchem/index-document engine document))}
   :combine5 {:func (fn [document engine]
                      (annotate-chemdner-document combinelib/index-combination-5 engine document))}
   :combine6 {:func (fn [document engine]
                      (annotate-chemdner-document combinelib/index-combination-6 engine document))}
   })

(defn index-document
   [engine document]
  (let [result (when (contains? *index-engine-map* (keyword engine))
                 ((-> engine keyword *index-engine-map* :func) document engine))]
    (hash-map :docid (:docid document)
              :terms (union (-> result (get "combine5") :abstract-result)
                            (-> result (get "combine5") :title-result)))))

(defn write-cdi-results
  "Write result list to file in CDI (Chemdner Document indexing) format."
  [directory engine document-list]
  (dorun
   (map (fn [document]
           (write-cdi-result (format "%s/%s.cdi" directory (:docid document))
                             (index-chemdner-document engine document)))
         document-list)))

(defn process-brat-txt-files
  [dirname]
  (let [filelist (utils/list-of-files dirname)
        txtfilelist (filter #(> (.indexOf % ".txt") 0) filelist)]
    (map
     (fn [infn]
       (let [outfn (str dirname "/" (utils/basename infn ".txt") ".ann")]
         (write-brat (:annotations (process :combine3 (slurp (str dirname "/" infn))))
                          outfn)))
     txtfilelist)))
