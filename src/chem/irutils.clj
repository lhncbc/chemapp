(ns chem.irutils
  (:require [clojure.string :refer [split]])
  (:import (irutils InvertedFileContainer InvertedFile IFBuild IFQuery))
  (:gen-class))

(defn get-container [tablepath indexpath]
  (new InvertedFileContainer tablepath indexpath))

(defn get-index [^InvertedFileContainer container dbname]
  (let [^InvertedFile index (.get container dbname)]
    (.update index)
    (.setup index)
    index))

(defn init-index
  [tablepath indexpath indexname]
  (let [^InvertedFileContainer container (get-container tablepath indexpath)]
    (.get container indexname)))

(defn record-to-map
  "convert piped-separated normchem record to clojure mapped structure."
  [record]
  (into {}
        (conj (map #(vector %1 %2)
                   [:keyterm :meshid :pubchemid :smiles :synonym0 :synonym1] (split record #"\|"))
              (vector :record record))))

(defn lookup
  "lookup term"
  [^InvertedFile index term]
  (mapv record-to-map
        (.getValue (.lookup index term))))

(defn nmslookup 
  "lookup term using normalized form of term"
  [^InvertedFile index term]
  (mapv record-to-map
        (.getValue (.lookup index term))))

;; Add loader similar to loader/indexer supplied with metamaplite:
;; gov.nih.nlm.nls.metamap.dfbuilder.CreateIndexes

(defn create-index
  [tablepath indexpath indexname]
  (let [^InvertedFileContainer container (get-container tablepath indexpath)
        index (.get container indexname)]
    (if (nil? index)
      (do 
        (println (str "error creating index for " indexname "."))
        (println (str "missing entry in config file: ifconfig for "
                      indexname ".")))
      (do
        (.update index)
        (.setup index)
        index))))
      
