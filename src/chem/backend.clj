(ns chem.backend
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zx]
            [clojure.tools.logging :as log]
            [chem.process :as process]
            [chem.mallet-ner :as mallet-ner]
            [chem.lucene :as lucene]
            [chem.span-utils :as span-utils]
            [clj-http.client :as client]
            [chem.irutils-normchem :as irutils-normchem]
            [chem.opennlp :as opennlp]
            [chem.english-words :as english-words])
  (:import (java.lang System)
           (java.io BufferedReader FileReader File)
           (java.util Properties)
           (javax.servlet ServletContext))
  (:gen-class))

;; if running in tomcat this will be the full path of server (not ".")
(def ^:dynamic *catalina-home* (System/getProperty "catalina.home" "."))

;; Use JVM option -Dchemapp.properties.file=filename to set location of properties file.
(def ^:dynamic *properties-fn* (System/getProperty "chemapp.properties.file", "config/chem.properties"))
(def ^:dynamic *lucene-mwinormchem-index-fn* "data/lucenedb")

(defonce ivfpath "data/ivf")
(defonce tablepath (str ivfpath "data/ivf/normchem/tables"))
(defonce indexpath (str ivfpath "data/ivf/normchem/indices"))
(defonce indexname "normchem2017")

;; Read properties file for configuration.
(defn read-properties [file-name]
  "Read properties from file, convert to clojure dictionary."
  (let [properties (new Properties)]
    (.load properties (new BufferedReader (new FileReader file-name)))
    (reduce #(assoc %1 (keyword (first %2)) (second %2))
            {} (into {} properties))))

(defn init-using-config
  [root-dir]
  (let [config (read-properties (str root-dir *properties-fn*))]
  ;; (chemdb/remote-init "chem")
    (if (contains? config :lucene.mwinormchem.index.fn)
      (def ^:dynamic *lucene-mwinormchem-index-fn* (config :lucene.mwinormchem.index.fn))
      (log/info (str "lucene.mwinormchem.index.fn not defined in " *properties-fn* 
                    "using default: " *lucene-mwinormchem-index-fn*)))
    (lucene/init (str root-dir *lucene-mwinormchem-index-fn*))
    (opennlp/init root-dir)
    (english-words/init (str root-dir english-words/wordsfilefn))

    (when (and (contains? config :irutils.normchem.index.tablepath)
               (contains? config :irutils.normchem.index.indexpath)
               (contains? config :irutils.normchem.index.indexname))
      (irutils-normchem/init (str root-dir (config :irutils.normchem.index.tablepath))
                             (str root-dir (config :irutils.normchem.index.indexpath))
                             (config :irutils.normchem.index.indexname)))
    
    (if (contains? config :mallet.model.file)
      (mallet-ner/init (str root-dir (config :mallet.model.file)))
      (do (println (str "mallet.model.file not defined in " *properties-fn* 
                    "using default: " mallet-ner/*crf-model-default-filename*))
          (mallet-ner/init)))))

(defn init
  "Initialize any external resource adaptors."
  ([]
   (init-using-config "./"))
  ([servlet-context]
   (let [root-path (.getRealPath ^ServletContext servlet-context "/")]
        (init-using-config root-path))
   ))

(comment (defn list-docids 
  ([len] (list-docids "chem"))
  ([dbname len]
     (chemdb/list-docids dbname))))

(comment (defn list-documents
  ([len] (list-documents "chem"))
  ([dbname len]
     (chemdb/list-documents dbname))))

(comment (defn get-document [docid]
  (chemdb/get-document docid)))

(defn gen-engine-option-list
  " From clojure map chem.process/*engine-map* generate list of form:
      [:select {:name \"engine\"}
       [:option {:value \"metamap\"}  \"Metamap\"]
       [:option {:value \"partial\"}  \"Partial Chemical Match\"]
       [:option {:value \"partial-enhanced\"}  \"Partial Chemical Match Enhanced\"]
       [:option {:value \"fragment\"} \"Chemical Fragment Match\"]
       [:option {:value \"normchem\"} \"Normalized Chemical Match\"]
       [:option {:value \"combine1\"} \"Combination 1 (partial+norm)\"]]"
  ([] (gen-engine-option-list "combine5"))
  ([default-name]
     (vec (concat (vector :select {:name "engine"})
                  (vec (map (fn [entry]
                              (if (= default-name (name (first entry)))
                                (vector :option {:selected "true" :value (name (first entry))}
                                        (:label (second entry)))
                                (vector :option {:value (name (first entry))}
                                        (:label (second entry)))))
                            process/*engine-map*))))))

(defn process-chemdner-document
 [document engine]
  (when (contains? process/*chemdner-engine-map* (keyword engine))
    (get ((-> engine keyword process/*chemdner-engine-map* :func) document engine)
         (keyword engine))))

;; Convenience function, first seen at nakkaya.com later in clj.zip src.
;; Equivalent to: (-> responsexml clojure.data.xml/parse-str  clojure.zip/xml-zip)
(defn zip-str [s]
  (zip/xml-zip 
   (xml/parse (java.io.ByteArrayInputStream. (.getBytes s)))))

(defn pubmed-doc-to-map
  [pubmed-xml-str]
  "Convert XML PubMed document to a map containing pmid,
  title, and abstract."
  (let [doc-root (zip-str pubmed-xml-str)]
       (if (not (nil? doc-root))
         {:pmid (-> (zx/xml-> doc-root
                              :PubmedArticle
                              :MedlineCitation
                              :PMID)
                    first first :content first)
          :title (-> (zx/xml-> doc-root
                               :PubmedArticle
                               :MedlineCitation
                               :Article
                               :ArticleTitle)
                     first first :content first)
          :abstract (-> (zx/xml-> doc-root
                                  :PubmedArticle
                                  :MedlineCitation
                                  :Article
                                  :Abstract
                                  :AbstractText) 
                        first first :content first)}
          {:error {:status "doc-root is nil."}})))

(defn get-pubmed-doc-xml
  "Get PubMed document as XML"
 [pmid]
 (let [response (client/get 
                 (format 
                  "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id=%d" 
                  pmid))
       responsexml (:body response)]
   (hash-map :response response
             :status (:status response)
             :xml (:body response))))

(defn get-pubmed-doc-xml-zip
  "Get PubMed document as XML Zip structure"
 [pmid]
 (let [result (get-pubmed-doc-xml pmid)]
   (assoc result :doc-zip (zip-str (:xml result)))))

(defn get-pubmed-doc
  "Get PubMed document as XML and convert it to a map containing pmid,
  title, and abstract."
 [pmid]
 (let [response (client/get 
                 (format 
                  "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id=%d" 
                  pmid))
       responsexml (:body response)
       doc-root (if (= (:status response) 200)
              (zip-str responsexml)
              nil)]

       (if (not (nil? doc-root))
         {:pmid pmid
          :title (-> (zx/xml-> doc-root
                               :PubmedArticle
                               :MedlineCitation
                               :Article
                               :ArticleTitle)
                     first first :content first)
          :abstract (-> (zx/xml-> doc-root
                                  :PubmedArticle
                                  :MedlineCitation
                                  :Article
                                  :Abstract
                                  :AbstractText) 
                        first first :content first)}
         {:pmid pmid
          :error {:status (response :status)}})))

(defn get-pubmed-doc-sa
  "Get PubMed document as XML and convert it to a map containing pmid,
  title, and abstract.  If document is a structured abstract, then add
  labels for each section and then concatenate the sections."
 [pmid]
 (let [response (client/get 
                 (format 
                  "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id=%d" 
                  pmid))
       responsexml (:body response)
       doc-root (if (= (:status response) 200)
                  (zip-str responsexml)
                  nil)]
       (if (not (nil? doc-root))
         {:pmid pmid
          :title (-> (zx/xml-> doc-root
                               :PubmedArticle
                               :MedlineCitation
                               :Article
                               :ArticleTitle)
                     first first :content first)
          :abstract (apply str 
                           (map #(if (-> % first :attrs :Label) ;; is this a structured abstract?
                                   (str (-> % first :attrs :Label) ": " (-> % first :content first) " ")
                                   (-> % first :content first))
                                (zx/xml-> doc-root
                                          :PubmedArticle
                                          :MedlineCitation
                                          :Article
                                          :Abstract
                                          :AbstractText)))}
         {:pmid pmid
          :error {:status (response :status)}})))

(defn process-pubmed-document 
  "Get PubMed document and then annotate it."
  ([pmid]
     (process-pubmed-document "combine5" pmid)) ; mallet + lucene-normchem
  ([method pmid]
     (let [record (get-pubmed-doc-sa pmid)]
       (if (contains? record :error)
         record
         (let [title-annotations (sort-by :start 
                                       (:annotations 
                                        (process/process method (:title record))))
               abstract-annotations (sort-by :start 
                                             (:annotations 
                                              (process/process method (:abstract record))))]
           (assoc record
                  :title_spans (map #(:span %) title-annotations)
                  :title_annotations title-annotations 
                  :abstract_spans (map #(:span %) abstract-annotations)
                  :abstract_annotations abstract-annotations))))))
  

(defn index-pubmed-document
  [method pmid]
  (let [record (get-pubmed-doc-sa pmid)]
    
    (if (contains? record :error)
      record
      (hash-set :docid pmid
                :terms (set (concat (process/process method (:title record))
                                    (process/process method (:abstract record)))))
      )))

(defn get-engine-label [enginename]
  (process/get-engine-label enginename))


(defn get-document [docid]
  (get-pubmed-doc-sa docid))

