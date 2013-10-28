(ns chem.mmdfb
  (:import (java.io BufferedReader FileReader))
  (:require [clojure.string :as string])
  (:require [chem.mongodb :as chemdb])
  (:require [chem.utils :as utils])
  (:gen-class))

;; Generate MRCONSO from dui, cid, and chemical name
;;
;; ## Functions for the `RRF` version of `MRCONSO`
;;
;; These functions operate on `RRF MRCONSO` records or entire tables
;; of records.
;;
;; Format of `RRF MRCONSO` in column order:
;; 
;;  01. CUI  Unique identifier for concept
;;  02. LAT  Language of term
;;  03. TS   Term status
;;  04. LUI  Lexical unique identifier
;;  05. STT  String type
;;  06. SUI  String unique identifier
;;  07. ISPREF Atom status - preferred (Y) or not (N) for this string within this concept
;;  08. AUI  Unique identifier for atom
;;  09. SAUI Source asserted atom identifier [optional]
;;  10. SCUI Source asserted concept identifier [optional]
;;  11. SDUI Source asserted descriptor identifier [optional]
;;  12. SAB  Abbreviated source name
;;  13. TTY  Term type
;;  14. CODE Source asserted identifier
;;  15. STR  String
;;  16. SRL  Source restriction level
;;  17. SUPPRESS  Values = O, E, Y, or N
;;  18. CVF  Content View Flag
;; 
;; A record is suppressed if the value of the SUPPRESS field is "O",
;; "E", or "Y"; if it is not suppressed, the value is "N".
;;
;; O: All obsolete content, whether they are obsolesced by the source
;; or by NLM. These will include all atoms having obsolete TTYs, and
;; other atoms becoming obsolete that have not acquired an obsolete
;; TTY (e.g. RxNorm SCDs no longer associated with current drugs, LNC
;; atoms derived from obsolete LNC concepts).
;;
;; E: Non-obsolete content marked suppressible by an editor. These do
;; not have a suppressible SAB/TTY combination.
;;
;; Y: Non-obsolete content deemed suppressible during inversion. These
;; can be determined by a specific SAB/TTY combination explicitly
;; listed in MRRANK.
;;
;; N: None of the above
;;
(defn get-duis-for-chemnames [chemnamelist]
  (filter #(not (nil? %))
          (map (fn [chemname]
                 (let [key (.toLowerCase chemname)
                       result (first (chemdb/lookup :normchem key))]
                   (when (> (count result) 0)
                     (hash-map :key key
                               :value (:value result)))))
               chemnamelist)))

;; Generate lui map from file generated using luinorm on namelist
;; (def nameluinormlist (map #(vec (.split % "\\|")) (chem.utils/line-seq-from-file "luinormfile")))
;; (def luimap (generate-lui-map nameluinormlist))
;;

(defn generate-term-normterm-map [nameluinormlist]
  (reduce (fn [newmap el]
            (let [term (first el)
                  normterm (second el)]
              (assoc newmap term (if (newmap term)
                                   (conj (newmap term) normterm)
                                   (list normterm)))))
          {} nameluinormlist))

(defn generate-lui-map [nameluinormlist]
  " Given alist of (term normalized-term) pairs, generate map, mapping
    both original and normalized forms to generated lui."
  (reduce (fn [newmap el]
            (let [id (first el)
                  term (first (second el))
                  normterm (second (second el))]
            (assoc newmap
              term     (format "L%07d" id) 
              normterm (format "L%07d" id))))
          {} (map-indexed (fn [idx item] [idx item]) nameluinormlist)))

;; generate sui map from lui map keys 
;;  (def suimap (generate-sui-map (sort (keys luimap))))
;;
(defn generate-sui-map [stringlist]
  (reduce (fn [newmap el]
            (let [id (first el)
                  term (second el)]
            (assoc newmap term (format "S%07d" id))))
          {} (map-indexed (fn [idx item] [idx item]) stringlist)))

(defn generate-chem-cui-key [cui key] (format "%s|%s" cui key))

;; (def chemduipairlist (get-duis-for-chemnames chemnames))
;; (def auimap (generate-aui-map chemduipairlist)) 
(defn generate-aui-map [chemduipairlist] 
  (reduce (fn [newmap el]
            (let [id (first el)
                  key (:key (second el))
                  cui (:value (second el))] ; supplementary Concept ID or Descriptor ID is our Concept ID
              (assoc newmap (generate-chem-cui-key cui key) (format "A%07d" id))))
          {} (map-indexed (fn [idx item] [idx item]) chemduipairlist)))


(defn generate-mrconso [chemduipairlist srcname suimap luimap auimap]
  "Generate MRCONSO.RRF from chemname -> MeSH Descriptor id list and
   previously generated sui, lui, and aui dictionaries."
  (filter #(not (nil? %))
          (map (fn [pair] 
                 (when (contains? luimap (pair :key))
                   (list (pair :value) "ENG" "P"
                         (luimap (pair :key))
                         "PF"
                         (suimap  (pair :key))
                         "Y"
                         (auimap (generate-chem-cui-key (pair :value) (pair :key)))
                         (auimap (generate-chem-cui-key (pair :value) (pair :key)))
                         (if (= (nth (pair :value) 0) \C) (pair :value) "")
                         (if (= (nth (pair :value) 0) \D) (pair :value) "")
                         srcname
                         ""
                         (pair :value)
                         (pair :key)
                         ""
                         "N"
                         "")))
               chemduipairlist)))
   
(defn write-mrconso [outfilename mrconsocoll]
  "write mrconso to file in pipe-separated fields format."
  (chem.utils/write-elements-piped outfilename mrconsocoll))


;; See UMLS Reference Manual, http://www.ncbi.nlm.nih.gov/books/NBK9685/
;; 3.3.7 Semantic Types (File = MRSTY.RRF)
(defn generate-mrsty [chemduipairlist luimap]
  (filter #(not (nil? %))
          (map (fn [pair] 
                 (when (contains? luimap (pair :key))
                   (list (pair :value)
                         "T103"
                         "A1.4.1"
                         "Chemical"
                         "Not used"
                         "Not used"
                         )))
               chemduipairlist)))

;; See UMLS Reference Manual, http://www.ncbi.nlm.nih.gov/books/NBK9685/
;; Table 11. Source Information (File = MRSAB.RRF)

(defn generate-mrsab []
  (list ["C4000000" "C4000000" "PUBCHEM" "PUBCHEM" 
         "http://pubchem.ncbi.nlm.nih.gov/" "" "" 
         "" "" "" "" "" "0" "1" "1" "" "" "" "ENG" "ascii" "Y" "Y" ""]))

(defn generate-mrrank []
  (list ["0400" "PUBCHEM" "PT" "N"]
        ["0400" "PUBCHEM" "SY" "N"]))