(ns medline.medline-xml-format
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zx])
  (:import (java.io ByteArrayInputStream)))

;; Convenience function, first seen at nakkaya.com later in clj.zip src.
;; Equivalent to: (-> responsexml clojure.data.xml/parse-str  clojure.zip/xml-zip)
(defn zip-str [^String s]
  (zip/xml-zip 
      (xml/parse (ByteArrayInputStream. (.getBytes s)))))

(defn reader
    [doc-str]
    (let [doc-root (zip-str doc-str)]
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
      ))
