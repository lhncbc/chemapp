(ns chem.opsin
  (:require [chem.annotations :as annot])
  (:require [chem.metamap-tokenization :as mm-tokenization])
  (:import (uk.ac.cam.ch.wwmm.opsin NameToStructure)))

;; Convert a chemical name to SMILES
;; chem.core> (import uk.ac.cam.ch.wwmm.opsin.NameToStructure)
;; uk.ac.cam.ch.wwmm.opsin.NameToStructure
;; chem.core> (def nts (NameToStructure/getInstance))
;; #'chem.core/nts
;; chem.core> (def smiles (.parseToSmiles nts "acetonitrile"))
;; #'chem.core/smiles
;; chem.core> smiles
;; "C(C)#N"
;; chem.core> (def cml (.parseToCML nts "acetonitrile"))
;; #'chem.core/cml
;; chem.core> cml
;; #<Element [nu.xom.Element: cml]>
;; chem.core> 

(defn name-to-smiles [cstring]
  (let [nts (NameToStructure/getInstance)]
    (.parseToSmiles nts cstring)))

(defn name-to-cml [cstring]
  (let [nts (NameToStructure/getInstance)]
    (.parseToCML nts cstring)))

(defn add-smiles-if-exists
  "add smiles representation to annotation if it exists."
  [annotation]
  (let [result (if (nil? (:text annotation))
                 nil
                 (name-to-smiles (:text annotation)))]
    (if (not (nil? result ))
      (conj annotation {:smiles result})
      annotation)))

(defn filter-using-engine-keyword
  [document engine-keyword] 
  ^{:doc "Keep engine annotations that are represented in OPSIN
          chemical database." }
  (conj document 
        (hash-map 
         (keyword (str (name engine-keyword) "-opsin"))
         (hash-map :title-result
                   (hash-map 
                    :annotations
                    (filter #(contains? % :smiles)
                            (map add-smiles-if-exists
                                 (annot/list-annotations engine-keyword :title-result document))))
                   :abstract-result
                   (hash-map 
                    :annotations
                    (filter #(contains? % :smiles)
                            (map add-smiles-if-exists
                                 (annot/list-annotations engine-keyword :abstract-result document))))))))

(defn filter-partial-match-using-opsin [document]
  ^{:doc "Keep partial-match annotations that are represented in
          opsin chemical database." }
  (filter-using-engine-keyword document :partial))

(defn filter-token-match-using-opsin [document]
  ^{:doc "Keep token-match annotations that are represented in
          opsin chemical database." }
  (filter-using-engine-keyword document :token))

; nREPL 0.1.8-preview
;; user> (import uk.ac.cam.ch.wwmm.opsin.NameToStructure)
;; uk.ac.cam.ch.wwmm.opsin.NameToStructure
;; user> (def nts (NameToStructure/getInstance))
;; #'user/nts
;; user> (def smiles (.parseToSmiles nts "acetonitrile"))
;; #'user/smiles
;; user> smiles
;; "C(C)#N"
;; user> (def cml (.parseToCML nts "acetonitrile"))
;; #'user/cml
;; user> cml
;; #<Element [nu.xom.Element: cml]>
;;
;;

;; (import uk.ac.cam.ch.wwmm.opsin.NameToStructureConfig)
;; uk.ac.cam.ch.wwmm.opsin.NameToStructureConfig
;; user> (def ntsconfig (new NameToStructureConfig))
;; #'user/ntsconfig
;; user> (.setAllowRadicals ntsconfig true)
;; nil
;; user> (def result (.parseChemicalName nts "acetonitrile" ntsconfig))
;; #'user/result
;; user> (def smiles (.getSmiles result))
;; #'user/smiles
;; user> smiles
;; "C(C)#N"

;; user> (import uk.ac.cam.ch.wwmm.opsin.NameToInchi)
;; uk.ac.cam.ch.wwmm.opsin.NameToInchi
