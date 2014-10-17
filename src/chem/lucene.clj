(ns chem.lucene
  (:import (org.apache.lucene.index DirectoryReader)
           (org.apache.lucene.store FSDirectory)
           (org.apache.lucene.document Document)
           (java.io File)
           (org.apache.lucene.search IndexSearcher Query ScoreDoc)
           (org.apache.lucene.queryparser.classic QueryParser)
           (org.apache.lucene.util Version)
           (org.apache.lucene.analysis.en EnglishAnalyzer)))

(def ^:dynamic *nindex* (FSDirectory/open (File. "/rhome/wjrogers/lucenedb/example")))
(def ^:dynamic *ireader* (DirectoryReader/open *nindex*))
(def ^:dynamic *query-parser* (QueryParser. Version/LUCENE_CURRENT, "term", (EnglishAnalyzer.)))
(def ^:dynamic *norm-query-parser* (QueryParser. Version/LUCENE_CURRENT, "nterm", (EnglishAnalyzer.)))
(def ^:dynamic *isearcher* (IndexSearcher. *ireader*))

(defn reinit 
  ([] (reinit "/rhome/wjrogers/lucenedb/example"))
  ([dir-name]
     (def ^:dynamic *nindex* (FSDirectory/open (File. dir-name)))
     (def ^:dynamic *ireader* (DirectoryReader/open *nindex*))
     (def ^:dynamic *query-parser* (QueryParser. Version/LUCENE_CURRENT, "term", (EnglishAnalyzer.)))
     (def ^:dynamic *norm-query-parser* (QueryParser. Version/LUCENE_CURRENT, "nterm", (EnglishAnalyzer.)))
     (def ^:dynamic *isearcher* (IndexSearcher. *ireader*))  ))

(defn lookup 
  "lookup term"
  ([term] (lookup term 100))
  ([term len]
     (let [query (.parse *query-parser* term)
           result (.search *isearcher* query nil len)
           hit-list (.scoreDocs result)]
       (map (fn [hit]
              (let [hit-doc (.doc *isearcher* (.doc hit))]
                (hash-map :text (.get hit-doc "term")
                          :cstring (.get hit-doc "term")
                          :meshid (.get hit-doc "meshid")
                          :cid (.get hit-doc "cid")
                          :ncstring (.get hit-doc "nterm"))))
            hit-list))))

(defn lookup-one [term]
  (lookup term 1))

(defn nmslookup 
  "lookup term using normalized form of term"
  ([term] (lookup term 100))
  ([term len]
     (let [query (.parse *norm-query-parser* term)
           result (.search *isearcher* query nil len)
           hit-list (.scoreDocs result)]
       (map (fn [hit]
              (let [hit-doc (.doc *isearcher* (.doc hit))]
                (hash-map :text (.get hit-doc "term")
                          :cstring (.get hit-doc "term")
                          :meshid (.get hit-doc "meshid")
                          :cid (.get hit-doc "cid")
                          :ncstring (.get hit-doc "nterm"))))
            hit-list))))

