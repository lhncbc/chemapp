(ns chem.meshxml
  (:require [clojure.data.xml :as xml]
            [clojure.data.zip :as zip]
            [clojure.data.zip.xml :as zx]
            [clojure.java.io]
            [clojure.string :as string])
  (:use [clojure.zip :only [xml-zip node]]
        [clojure.set :only [intersection union]])
  (:import (java.io PrintWriter FileWriter Writer
                    BufferedReader FileReader)))

;; this is old, use chem.mesh-chem (mesh_chem.clj) instead.

(def xml2003samplefn "/export/home/wjrogers/Notes/RTMUpdate/xml2003sample.xml")
(def xml2015samplefn "/export/home/wjrogers/studio/clojure/chem/xml2015sample.txt")
(def xml2015sample2fn "/export/home/wjrogers/studio/clojure/chem/desc2015sample.xml")
(def supp2015sample2fn "/export/home/wjrogers/studio/clojure/chem/scrsample.xml")
(def desc-2012fn "/net/lhcdevfiler/vol/cgsb5/ind/II_Group_WorkArea3aux22/MeSHUpdate/2012/originalmesh/xmlmesh/desc2012.xml")

(def mesh-dir "/net/lhcdevfiler/vol/cgsb5/ind/II_Group_WorkArea/MEDLINE_Baseline_Repository/MeSH")
(def desc-2014fn (str mesh-dir "/2014/desc2014.xml"))
(def supp-2014fn (str mesh-dir "/2014/supp2014.xml"))
(def desc-2015fn (str mesh-dir "/2015/desc2015.xml"))
(def supp-2015fn (str mesh-dir "/2015/supp2015.xml"))
(def desc-2016fn (str mesh-dir "/2016/desc2016.xml"))
(def supp-2016fn (str mesh-dir "/2016/supp2016.xml"))
(def desc-2017fn (str mesh-dir "/2017/desc2017.xml"))
(def supp-2017fn (str mesh-dir "/2017/supp2017.xml"))
(def desc-2018fn (str mesh-dir "/2018/desc2018.xml"))
(def supp-2018fn (str mesh-dir "/2018/supp2018.xml"))

;; Convenience function, first seen at nakkaya.com later in clj.zip src.
;; Equivalent to: (-> responsexml clojure.data.xml/parse-str  clojure.zip/xml-zip)
(defn zip-str [^String s]
  (xml-zip 
      (xml/parse (java.io.ByteArrayInputStream. (.getBytes s)))))

(defn zip-file [^String filename]
  (xml-zip 
   (xml/parse (java.io.FileInputStream. filename))))

(defn dom-file [^String filename]
   (xml/parse (java.io.FileInputStream. filename)))

(defn xml-content
  "Returns the contents of the xml node at location loc."
  [loc] ((node loc) :content))

(defn get-descriptor-info
" what do I want: scname,scui,conceptui,conceptname,umlscui,casn1name,
                  registryno,semtypeset,termset"
  [descriptor-element]
  (let [descriptor-zip (xml-zip descriptor-element)]
    {:name (first (zx/xml-> descriptor-zip  :DescriptorName :String zx/text))
     :meshid (zx/xml1-> descriptor-zip  :DescriptorUI zx/text)
     :casn1name (first (zx/xml-> descriptor-zip 
                                 :ConceptList
                                 :Concept
                                 (zx/attr= :PreferredConceptYN "Y")
                                 :CASN1Name 
                                 zx/text))
     :heading-mapped-to (set (zx/xml-> descriptor-zip
                                       :HeadingMappedToList
                                       :HeadingMappedTo
                                       :DescriptorReferredTo
                                       :DescriptorName
                                       zx/text))
     :unii-registry-number (first (zx/xml-> descriptor-zip 
                                            :ConceptList
                                            :Concept
                                            (zx/attr= :PreferredConceptYN "Y")
                                            :RegistryNumber
                                            zx/text))
     :semantic-type-list (set (zx/xml-> descriptor-zip 
                                        :ConceptList
                                        :Concept
                                        (zx/attr= :PreferredConceptYN "Y")
                                        :SemanticTypeList
                                        :SemanticType
                                        :SemanticTypeName
                                        zx/text))
     :termset (set (zx/xml-> descriptor-zip 
                             :ConceptList
                             :Concept
                             (zx/attr= :PreferredConceptYN "Y")
                             :TermList
                             :Term
                             :String
                             zx/text))
     :conceptui (first (zx/xml-> descriptor-zip 
                                 :ConceptList
                                 :Concept
                                 (zx/attr= :PreferredConceptYN "Y")
                                 :ConceptUI
                                 zx/text))
     :conceptname (first (zx/xml-> descriptor-zip 
                                   :ConceptList
                                   :Concept
                                   (zx/attr= :PreferredConceptYN "Y")
                                   :ConceptName
                                   :String
                                   zx/text))
     }))

(defn get-supplemental-record-info
" what do I want: scname,scui,conceptui,conceptname,umlscui,casn1name,
                  registryno,semtypeset,termset"
  [supplemental-record-element]
  (let [supplemental-record-zip (xml-zip supplemental-record-element)]
    {:name (first (zx/xml-> supplemental-record-zip  :SupplementalRecordName :String zx/text))
     :meshid (zx/xml1-> supplemental-record-zip  :SupplementalRecordUI zx/text)
     :casn1name (first (zx/xml-> supplemental-record-zip 
                                 :ConceptList
                                 :Concept
                                 (zx/attr= :PreferredConceptYN "Y")
                                 :CASN1Name
                                 zx/text))
     :heading-mapped-to (set (zx/xml-> supplemental-record-zip
                                       :HeadingMappedToList
                                       :HeadingMappedTo
                                       :DescriptorReferredTo
                                       :DescriptorName
                                       zx/text))
     :unii-registry-number (first (zx/xml-> supplemental-record-zip 
                                            :ConceptList
                                            :Concept
                                            (zx/attr= :PreferredConceptYN "Y")
                                            :RegistryNumber
                                            zx/text))
     :semantic-type-list (set (zx/xml-> supplemental-record-zip 
                                        :ConceptList
                                        :Concept
                                        (zx/attr= :PreferredConceptYN "Y")
                                        :SemanticTypeList
                                        :SemanticType
                                        :SemanticTypeName
                                        zx/text))

     :termset (set (zx/xml-> supplemental-record-zip 
                             :ConceptList
                             :Concept
                             (zx/attr= :PreferredConceptYN "Y")
                             :TermList
                             :Term
                             :String
                             zx/text))
     :conceptui (first (zx/xml-> supplemental-record-zip
                                 :ConceptList
                                 :Concept
                                 (zx/attr= :PreferredConceptYN "Y")
                                 :ConceptUI
                                 zx/text))
     :conceptname (first (zx/xml-> supplemental-record-zip 
                                   :ConceptList
                                   :Concept
                                   (zx/attr= :PreferredConceptYN "Y")
                                   :ConceptName
                                   :String
                                   zx/text))
     }))

(defn traverse-descriptors
  "Apply function (default: get-descriptor-info) to descriptor
   elements in meshxml dom tree"
  ([meshdom]
     (traverse-descriptors meshdom get-descriptor-info))
  ([meshdom descriptor-fn]
     (->> meshdom
          :content
          (filter #(= :DescriptorRecord (:tag %)))
          (map descriptor-fn)))
  ([meshdom descriptor-writer-fn wtr]
     (->> meshdom
          :content
          (filter #(= :DescriptorRecord (:tag %)))
          (map #(descriptor-writer-fn wtr %)))))

(def ^:dynamic *chemical-semtypes* #{"Chemical"
                                     "Element, Ion, or Isotope"
                                     "Inorganic Chemical"
                                     "Organic Chemical"
                                     "Organophosphorus Compound"})

(defn format-tabbed-info
" what do I want to write:
  scname,scui,conceptui,conceptname,casn1name,registryno,string.join(semtypeset,'|'),string.join(termset,'|')"
  [record-info]
  (format "%s\n"
          (string/join "\t" [(:name record-info)
                             (:meshid record-info)
                             (:conceptui record-info)
                             (:conceptname record-info)
                             (if (:casn1name record-info)
                               (:casn1name record-info) "")
                             (:unii-registry-number record-info)
                             (string/join "|" (:semantic-type-list record-info))
                             (string/join "|" (:termset record-info))
])))

(defn write-tabbed-descriptor-info
  "Write descriptor information in tabbed-separated format to writer."
  [^Writer wtr descriptor-element]
  (let [record-info (get-descriptor-info descriptor-element)]
    (when (or (seq (:casn1name record-info))
              (seq (intersection (:semantic-type-list record-info)
                                         *chemical-semtypes*)))
      (.write wtr ^String (format-tabbed-info record-info)))))

(defn write-edn-descriptor-info
  "Write descriptor information in tabbed-separated format to writer."
  [^Writer wtr descriptor-element]
  (let [record-info (get-descriptor-info descriptor-element)]
    (when (or (seq (:casn1name record-info))
              (seq (intersection (:semantic-type-list record-info)
                                         *chemical-semtypes*)))
      (.write wtr (pr-str record-info))
      (.write wtr "\n"))))

(defn write-tabbed-supplemental-record-info
  "Write supplemental record information in tabbed-separated format to writer."
  [^Writer wtr supplemental-record-element]
  (let [record-info (get-supplemental-record-info supplemental-record-element)]
    (when (or (seq (:casn1name record-info))
              (seq (intersection (:semantic-type-list record-info)
                                         *chemical-semtypes*)))
      (.write wtr ^String (format-tabbed-info record-info)))))

(defn write-edn-supplemental-record-info
  "Write supplemental record information in tabbed-separated format to writer."
  [^Writer wtr supplemental-record-element]
  (let [record-info (get-supplemental-record-info supplemental-record-element)]
    (when (or (seq (:casn1name record-info))
              (seq (intersection (:semantic-type-list record-info)
                                         *chemical-semtypes*)))
      (.write wtr (pr-str record-info))
      (.write wtr "\n"))))

(defn write-descriptor-info-to-file 
  "Write descriptor information to filename."
  ([meshdom filename]
     (write-descriptor-info-to-file meshdom filename write-tabbed-descriptor-info))
  ([meshdom filename descriptor-writer-fn]
     (with-open [wtr (clojure.java.io/writer filename)]
       (dorun 
        (traverse-descriptors meshdom descriptor-writer-fn wtr)))))
    
(defn round-trip 
  [xmlfilename outfilename record-writer-fn]
  (with-open [rdr (clojure.java.io/reader xmlfilename)
              wtr (clojure.java.io/writer outfilename)]
    (dorun
     (->> rdr
          xml/parse
          :content
          (filter #(or (= :DescriptorRecord (:tag %))
                       (= :SupplementalRecord (:tag %))))
          (map #(record-writer-fn wtr %))))))

(defn get-recordlist-info [elementlist]
  (map (fn [element]
         (cond
          (= :DescriptorRecord (:tag element)) (get-descriptor-info element)
          (= :SupplementalRecord (:tag element)) (get-supplemental-record-info element)
          :else []))
       elementlist))


(defn list-concepts 
  [xmlfilename]
  (with-open [rdr (clojure.java.io/reader xmlfilename)]
    (doall
     (->> rdr
          xml/parse
          :content
          (filter #(or (= :DescriptorRecord (:tag %))
                       (= :SupplementalRecord (:tag %))))
          get-recordlist-info
          ))))

(defn list-chemical-concepts 
  [xmlfilename]
  (->> xmlfilename 
       list-concepts
       (filter #(or (seq (:casn1name %))
                    (seq (intersection (:semantic-type-list %)
                                       *chemical-semtypes*))))))
  
;;    chem.core> (meshxml/round-trip meshxml/desc-2015fn  "desc2015.dat" meshxml/write-tabbed-descriptor-info)
;;    chem.core> (meshxml/round-trip meshxml/desc-2015fn  "desc2015.edn" meshxml/write-edn-descriptor-info)
;;    chem.core> (meshxml/round-trip meshxml/supp-2015fn "supp2015.dat" meshxml/write-tabbed-supplemental-record-info)
;;    chem.core> (meshxml/round-trip meshxml/supp-2015fn "supp2015.edn" meshxml/write-edn-supplemental-record-info)
