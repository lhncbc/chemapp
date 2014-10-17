(ns chem.normchem
  (:import (java.io BufferedReader FileReader FileWriter))
  (:require [monger.core :as mg])
  (:require [monger.collection :as mc])
  (:require [chem.annotations :as annot])
  (:require [skr.tokenization :as mm-tokenization])
  (:require [chem.span-utils :as span-utils])
  (:require [chem.stopwords :as stopwords])
  (:require [skr.mwi-utilities :as mwi-utilities]))

(defn setup
  "Initialize connection to mongo db database on specified hostname
  and port, defaults to using chem database."
  [hostname port databasename]
  (mg/connect! { :host hostname :port port })
  (mg/set-db! (mg/get-db databasename)))

(defn init 
  "Initialize connection to mongo db database, defaults to using chem database."
  ([] (setup "127.0.0.1" 27017 "chem"))
  ([dbname] (setup "127.0.0.1" 27017 dbname)))

(defn load-normalized-chem-records [filename tablename]
  (dorun
   (map (fn [lines-partition]
          (mc/insert-batch
           tablename
           (vec 
            (map (fn [record]
                   (let [fields (.split record "\t" 5)]
                     {:cstring   (nth fields 0)
                      :meshid    (nth fields 1)
                      :pubchemid (nth fields 2)
                      :smiles    (nth fields 3)
                      :ncstring  (nth fields 4)}))
                 lines-partition))))
        (partition-all 100 (line-seq (BufferedReader. (FileReader. filename)))))))

(defn lookup
  "Something like: (lookup \"normchem\" \"benzene\")"
  [collection term]
  (mc/find-one-as-map collection {:cstring term}))

(defn nmslookup
  "Something like: (lookup \"normchem\" (normalize-meta-string \"benzene\"))"
  [collection term]
  (mc/find-one-as-map collection {:ncstring (mwi-utilities/normalize-meta-string term)}))

(defn tokenize-document 
  "Convert text to tokens containing starting position, ending
  position, and text content."
  [document]
  (let [tokenlist (mm-tokenization/tokenize document 0)]
    (loop [i 0 j 0
           annotlist '()]
      (if (< j (count tokenlist))
        (recur (+ i (count (nth tokenlist j)))
               (inc j)
               (conj annotlist (hash-map 
                                :start i
                                :end (+ i (count (nth tokenlist j)))
                                :text (nth tokenlist j))))
        annotlist))))

(defn suppconcept-lookup [document]
  "Lookup supplementary concept, restricting to chemicals."
  (filter #(not (nil? %))
          (map (fn [token]
                 (when (and (> (count (:text token)) 2) (not (contains? stopwords/stopwords (clojure.string/lower-case (:text token)))))
                   (let [result (nmslookup "nmsnormchem" (clojure.string/lower-case (:text token)))]
                     (when result
                       (hash-map
                        :span {:start (token :start)
                               :end   (token :end)}
                        :text  (token :text)
                        :dui   (get result :meshid))))))
               (tokenize-document document))))

(defn suppconcept-lookup-finding-boundary
  "Lookup supplementary concept, restricting to chemicals,
   set span to leading and following extent of term."
  [document]
  (filter #(not (nil? (:dui %)))
          (filter #(not (nil? %))
                  (map (fn [token]
                         (let [text (:text token)
                               lc-text (clojure.string/lower-case text)]
                           (when (and (> (count text) 2) 
                                      (not (contains? stopwords/stopwords lc-text)))
                             (let [result (into {}  (nmslookup "nmsnormchem" lc-text))]
                               (when result 
                                 (let [bounds (span-utils/check-span
                                               (span-utils/find-bounds-of-string document (token :start))
                                               document)]
                                   (when (not (nil? bounds))
                                     (hash-map
                                      :span {:start (bounds :start)
                                             :end   (bounds :end)}
                                      :text      (.substring document (bounds :start) (bounds :end))
                                      :dui       (get result :meshid)
                                      :pubchemid (get result :pubchemid)))))))))
                       (tokenize-document document)))))

(defn get-spans [annotation-list]
  (map #(hash-map :start (-> % :span :start)
                  :end   (-> % :span :end))
       annotation-list))

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

(defn gen-normalized-keys-in-normchem-file [infilename outfilename]
  (with-open [br  (java.io.BufferedReader. (java.io.FileReader. infilename))
              fw (java.io.FileWriter. outfilename)]
    (dorun 
     (map (fn [line]
            (let [fields (clojure.string/split line #"\t" 4)
                  cstr   (nth fields 0)
                  id     (nth fields 1)
                  pcid   (nth fields 2)
                  smiles (nth fields 3)]
              (.write fw 
                      (format "%s\n"
                              (clojure.string/join "\t" 
                                                   (list cstr id pcid smiles 
                                                         (mwi-utilities/normalize-meta-string cstr))))) ))
          (line-seq br)) )))

(defn process-document [document]
  (let [annotation-list (suppconcept-lookup-finding-boundary document)]
    (hash-map :spans (sort-by :start (map #(:span %) annotation-list))
              :annotations annotation-list)))

(defn filter-partial-match-using-normchem [record]
  ^{:doc "Keep partial-match annotations that are represented in
         normalized supplementary chemical database." }
  (conj record 
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
                                                 (nmslookup "nmsnormchem" (:text annotation)))]
                                            (if result 
                                              (merge annotation {:meshid (result "meshid")
                                                                 :pubchemid (result "pubchemid")
                                                                 :cstring (result "cstring")
                                                                 :ncstring (result "ncstring")})
                                              nil)))
                                        (annot/list-annotations :partial :title-result record))))
                            :abstract-result
                            (hash-map :annotations
                                      (filter
                                       #(not (nil? (% :cstring)))
                                       (map
                                        (fn [annotation] 
                                          (let [result 
                                                (into
                                                 {} 
                                                 (nmslookup "nmsnormchem" (:text annotation)))]
                                            (if result 
                                              (merge annotation {:meshid (result "meshid")
                                                                 :pubchemid (result "pubchemid")
                                                                 :cstring (result "cstring")
                                                                 :ncstring (result "ncstring")})
                                              nil)))
                                        (annot/list-annotations :partial :abstract-result record))))))))

(defn make-pairs [normchem-record]
  (pmap #(list '(:text %) '(:probability 1.0))
        (filter #(not (nil? (:dui %)))
                (:annotations (:abstract-result (:normchem normchem-record))))))
