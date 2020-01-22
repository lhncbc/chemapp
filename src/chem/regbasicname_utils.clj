(ns chem.regbasicname-utils
  (:require [clojure.edn :as edn]
            [clojure.string :as cljstring]
            [clj-diff.core]
            [skr.tokenization]
            [utils.utils :as utils]
            [chem.extract-abbrev :as extract-abbrev]
            [chem.sentences :as sentences]
            [chem.stopwords :as stopwords]))

;; [["ABIET"]
;;  ["ABIETIN"]
;;  ["ACANTHIN"]
;;  ["ACANTHINE"]
;;  ["ACEANTHRYLEN"]
;;  ["ACEANTHRYLENE"]
;;  ["ACENAPHTH"]
;;  ["ACENAPHTHEN"]
;;  ["ACENAPHTHENE"]
;;  ["ACENAPHTHYLEN"]
;;  ["ACENAPHTHYLENE"]
;;  ["ACEPHENANTHRYLEN"]
;;  ["CEPHENANTHRYLENE"]
;;  ["ACET"]
;;  ["ACETAL"]
;;  ["ACETALDAZIN" "ACET ALDAZIN"]
;;  ["ACETALDEHYD" "ACET ALDEHYD"]
;;  ["ACETALDEHYDE" "ACET ALDEHYDE"]
;;  ["ACETIC"]]

(defn load-dictionary-0 [aseq]
   (reduce (fn [newmap line]
             (let [fields (cljstring/split line #"->")
                   key (cljstring/trim (first fields))
                   value (cljstring/split 
                          (if (> (count fields) 1)
                            (cljstring/trim (second fields))
                            (first fields))
                          #" ")]
               (assoc newmap 
                 key value
                 (cljstring/lower-case key) value)))
           {} aseq))

(defn load-dictionary [filename]
  (-> filename utils/line-seq-from-file load-dictionary-0))

(def ^:dynamic *regbasename-dictionary* {})

;; Partial implementation of Registry File Basic Name Segment
;; Dictionary lookup algorithm.
;;
;; from: paper: Registry File Basic Name Segment Dictionary, June
;; 1993, American Chemical Society.
;; (https://www.cas.org/sites/default/files/documents/regbasicname.pdf)
;;
;; See Also:
;;
;; Batista-Navarro R, Rak R, Ananiadou S. Optimising chemical named
;; entity recognition with pre-processing analytics, knowledge-rich
;; features and heuristics. J Cheminform. 2015;7(Suppl 1 Text mining
;; for chemistry and the CHEMDNER track):S6. Published 2015 Jan
;; 19. doi:10.1186/1758-2946-7-S1-S6 ;
;;
;; 
;; A different approach is used to create the basic segments from the
;; natural name segments. The steps involved are illustrated with the
;; natural segment "dicyclopenta" from the example above.
;;
;; 1. The system starts by scanning the segment from left to right trying to
;; find an equivalent in the Basic Name Segment Dictionary.
;; Looks for dicyclopenta in the dictionary - no match
;;
;; 2. If it does not find a match, it looks for the next longest segment in the
;; Basic Segment Dictionary by reducing the length of the remaining character
;; string until it finds a match. It reduces the length by removing the last
;; character from the string.
;;
;; Given chemical name: "dicyclopenta"
;; 
;;    Looks for dicyclopenta
;;              dicyclopent
;;              dicyclopen
;;              dicyclope
;;              dicyclop
;;              dicyclo
;;              ...
;;              ...
;;              ...
;;              di      - finds a match and posts di to Basic Index
;;
;;
;; It repeats this process on the remaining character string.
;; 
;;    Looks for cyclopenta - no match
;;              cyclopent  - no match
;;              ...
;;              ...
;;              ...
;;              cyclo      - finds a match and posts 'cyclo' to the Basic Index
;;
;; It repeats this process on the remaining character string.
;;
;; Looks for penta - finds a match and posts 'penta' to the Basic lndex

(def sample-term "dicyclopenta[def,pqr]tetraphenylene-1,8-dione")
(def sample-term2 "dicyclopenta")
(def sample-termlist
  ["dicyclopenta[def,pqr]tetraphenylene-1,8-dione"
   "dicyclopenta"
   "fluorophosphate" "hydrogen peroxide"
   "hydrogen peroxidase" "bis-(4-nitrophenyl)" "o-toluidine" "4-hydroxy-2,6-xylidine" 
   "4- and 6-hydroxy-o-toluidine" "hydrogen dioxide" "xylidine"
   "ascorbic acid"
   "citric acid" "sucrose" "amine" "amide"
   "nicotine" "nornicotine"
   "(S)-nicotine" "(S)-nornicotine" "(R)- and (S)-nicotine" "(S)-nornicotine"
   "hydrogen" "Chlordecone" "organochlorine"
   "Polyvinylidene fluoride" "vanadium" "Nafion" "1,2-Benzisothiazolin-3-one"
   "glucose" "ribose" "deoxyribose" "saline" "zinc sulfate" "Calcium" "ferrous sulfate"
   "δ-aminolevulinic acid dehydratase" "protoporphyrins" "erythrocyte"
   "oxidative" "lipid" "peroxidation" "glycated" "hemoglobin" "hypoglycemia"
   "N7-(2-carbamoyl-2-hydroxyethyl)guanine"
   "N-acetyl-S-(2-carbamoylethyl)cysteine"
   "N-acetyl-S-(2-hydroxy-2-carbamoylethyl)cysteine" "glutathione" "Acrylamide" "glutamate"
   "neostigmine" "acetylcholinesterase inhibitors" "acetic acid"
   "phosphatidylcholine" "albumins" "diacylglycerol ether" "triacylglycerols"
   "monoacylglycerol ethers" "diacylglycerol ethers" "fatty acid ethyl esters"
   "Colesevelam" "metformin" "bile acid" "glucagon-like peptide"
   ])

(defn init
  "Initialize Basic Segment Dictionary from file or default
  location (data/regbasicname.dat)."
  ([]
   (load-dictionary "data/regbasicname.dat"))
  ([filename]
   (load-dictionary filename)))

(defn list-components-debug
  [term]
  (let [termlen (count term)]
    (loop [start 0
           end termlen
           components []]
      (let [segment (subs term start end)]
        (if (>= start termlen)
          components
          (if (or (contains? *regbasename-dictionary* segment)
                  (contains? #{"0" "1" "2" "3" "4" "5" "6" "7" "8" "9" "δ"} segment))
            (do (println (format "segment: %-15s - match" segment))
                (if (= segment "") 
                   (recur (inc end) termlen components)
                   (recur end termlen (if (contains? *regbasename-dictionary* segment)
                                        (concat components (*regbasename-dictionary* segment))
                                        (conj components  segment)))))
            (do (println (format "segment: %-15s - no match" segment))
                (recur start (dec end) components))))))))
       

 (defn list-components
   [term]
   (let [termlen (count term)]
     (loop [start 0
            end termlen
            components []]
       (let [segment (subs term start end)]
         (if (>= start termlen)
           components
           (if (or (contains? *regbasename-dictionary* segment)
                    (contains? #{"0" "1" "2" "3" "4" "5" "6" "7" "8" "9" "δ"} segment))
                 (if (= segment "") 
                   (recur (inc end) termlen components)
                   (recur end termlen (if (contains? *regbasename-dictionary* segment)
                                        (vec (concat components (*regbasename-dictionary* segment)))
                                        (conj components segment))))
                 (recur start (dec end) components)))))))

(defn tokenize-without-punctuation-and-whitespace
  [term]
  (filter #(not (or (re-find skr.tokenization/pnpattern %)
                    (re-find skr.tokenization/wspattern %))) (skr.tokenization/tokenize term)))

(defn remove-punctuation-and-whitespace
  [term]
  (apply str (tokenize-without-punctuation-and-whitespace term)))
       
(defn coverage 
  [term]
(let [lterm (cljstring/lower-case term)
      components (list-components term)
      lcomponentstring (cljstring/lower-case (apply str components))
      normterm (remove-punctuation-and-whitespace term)]
  (cond 
    (or (re-find skr.tokenization/pnpattern term)
        (re-find skr.tokenization/nupattern term)) 0.0
    (= (cljstring/trim term) "") 0.0
    (> (count (cljstring/trim term)) 1) 0.0
    (contains? stopwords/stopwords term) 0.0
    (not (empty? (filter #(re-find (re-pattern (str % "$")) lterm)
                  ["ing", "mic", "tion", "ed", "sis", "ism", "coccus"])))
    0.0
    (= lterm lcomponentstring) 1.0
    (= (remove-punctuation-and-whitespace lterm) lcomponentstring) 1.0
    (or (= (.indexOf lterm lcomponentstring) 0)
        (= (+ (.lastIndexOf lterm lcomponentstring) (count lcomponentstring))
           (count (remove-punctuation-and-whitespace lterm)))) (float (/ (count lcomponentstring) (count normterm)))
    (>= (.indexOf lterm lcomponentstring) 0) (* 0.75 (/ (count lcomponentstring) (count normterm)))
    (> (count components) 1) (float (/ (count lcomponentstring) (count normterm)))
    :else (* 0.5 (/ (count lcomponentstring) (count normterm)))
    )))

(defn apply-coverage
  [termlist]
  (map (fn [term]
         {:term term
          :coverage (coverage term)
          :components (list-components term) 

})
       termlist))


(defn get-token-matches
  [tokenlist] 
  (filter #(> (:coverage %) 0.65)
          (map #(assoc % :coverage (coverage (:text %)))
                 tokenlist)))

(defn consolidate-matches 
  [token-match-list tokenlist]
  )


(defn process-document
  [document]
  (let [tagged-sentence-list (-> document
                                 sentences/make-sentence-list
                                 sentences/tokenize-sentences
                                 sentences/pos-tag-sentence-list
                                 sentences/enhance-sentence-list-pos-tags)
        sentence-tokenlist (vec (apply concat (map #(:pos-tags-enhanced %) tagged-sentence-list)))
        doc-tokenlist (skr.tokenization/analyze-text document)
        tokenlist (sentences/add-pos-tags-to-document-tokenlist doc-tokenlist sentence-tokenlist)
        abbrev-list (extract-abbrev/extract-abbr-pairs-string document)
        token-match-list (get-token-matches tokenlist)
        annotation-list (consolidate-matches token-match-list tokenlist)
        enhanced-annotation-list (sentences/add-valid-abbreviation-annotations document
                                                                     annotation-list
                                                                     abbrev-list)]
    (hash-map :spans (sort-by :start (map #(:span %) enhanced-annotation-list))
              :annotations (sort-by #(-> % :span :start) (set enhanced-annotation-list))
              :sentence-list tagged-sentence-list
              :tokenlist tokenlist
              :token-match-list token-match-list 
              :abbrev-list abbrev-list)))

