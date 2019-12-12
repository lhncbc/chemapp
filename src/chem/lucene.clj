(ns chem.lucene
  (:require [clojure.tools.logging :as log])
  (:import (org.apache.lucene.index DirectoryReader)
           (org.apache.lucene.store FSDirectory)
           (org.apache.lucene.document Document)
           (java.io File)
           (org.apache.lucene.search IndexSearcher Query ScoreDoc)
           (org.apache.lucene.queryparser.classic QueryParser)
           (org.apache.lucene.util Version)
           (org.apache.lucene.analysis.en EnglishAnalyzer))
  (:gen-class))

(def nindex (atom nil)) 
(def ireader (atom nil))
(def query-parser (atom nil))
(def norm-query-parser (atom nil))
(def isearcher (atom nil))

(defn init 
  ([] (init "data/lucenedb/mwinormchem2015"))
  ([^String dir-name]
   (reset! ^FSDirectory nindex (FSDirectory/open ^File (File. dir-name)))
   (reset! ^DirectoryReader ireader (DirectoryReader/open @nindex))
   (reset! ^QueryParser query-parser (QueryParser. Version/LUCENE_CURRENT, "term", (EnglishAnalyzer.)))
   (reset! ^QueryParser norm-query-parser (QueryParser. Version/LUCENE_CURRENT,
                                                        "nterm",
                                                        ^EnglishAnalyzer (EnglishAnalyzer.)))
   (reset!^IndexSearcher isearcher (IndexSearcher. @ireader))  ))

(defn lookup 
  "lookup term"
  ([term] (lookup term 100))
  ([^String term len]
     (let [query (.parse @query-parser term)
           result (.search @isearcher query nil len)
           hit-list (.scoreDocs result)]
       (log/info "hit-list: " (map bean hit-list))
       (map (fn [hit]
              (let [hit-doc (.doc @isearcher (.doc hit))]
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
  ([^String term len]
     (let [query (.parse @norm-query-parser term)
           result (.search @isearcher query nil len)
           hit-list (.scoreDocs result)]
       (map (fn [hit]
              (let [hit-doc (.doc @isearcher (.doc hit))]
                (hash-map :text (.get hit-doc "term")
                          :cstring (.get hit-doc "term")
                          :meshid (.get hit-doc "meshid")
                          :cid (.get hit-doc "cid")
                          :ncstring (.get hit-doc "nterm"))))
            hit-list))))

