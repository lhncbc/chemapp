(ns chem.metamap-api
  (:import (gov.nih.nlm.nls.metamap MetaMapApiImpl PCM Phrase))
  (:gen-class))

;; really trival example of use:
;; (ns chem.metamap-api)
;; (def mmapi (api-instantiate))
;; (clojure.pprint/pprint ((handle-result-list (process-string mmapi "calcium")))
;; ({:position ({:y 7, :x 0}),
;;   :matchmaplist ({:concept-match-end 1, :phrase-match-start 1, :phrase-match-end 1,
;;                   :lexical-variation 0, :concept-match-start 1}), :score -1000, 
;;   :preferredname "Calcium", 
;;   :head true, :conceptid "C0006675", :matchedwords ["calcium"], 
;;   :sources ["AOD" "CHV" "CSP" "LCH" "LNC" "MEDLINEPLUS" "MSH" "MTH" "MTHSPL"
;;             "NCI" "NDFRT" "RXNORM" "SNM" "SNMI" "SNOMEDCT" "USPMG" "VANDF"],
;;   :overmatch false, :semtypes ["bacs" "elii" "phsu"], :conceptname "Calcium"} 
;;  {:position ({:y 7, :x 0}),
;;   :matchmaplist ({:concept-match-end 1, :phrase-match-start 1, :phrase-match-end 1,
;;                   :lexical-variation 0, :concept-match-start 1}), :score -1000,
;;   :preferredname "Calcium, Dietary", :head true, :conceptid "C0006726", :matchedwords ["calcium"],
;;   :sources ["CHV" "CSP" "MSH" "MTH" "NCI" "NDFRT"], :overmatch false,
;;   :semtypes ["inch"], :conceptname "Calcium"})


(defn api-instantiate 
  ([] (comment (if (bound? #'*mmserver-hostname*)
        (do
          (print (str "mmserver hostname: " (deref #'*mmserver-hostname*)))
          (new MetaMapApiImpl (deref #'*mmserver-hostname*)))))
        (new MetaMapApiImpl))
  ([hostname] (new MetaMapApiImpl hostname)))

(defn process-string
  ([mmapi text]  (.processCitationsFromString mmapi text))
  ([mmapi text options]
     (when options 
       (.setOptions mmapi options))
     (.processCitationsFromString mmapi text)))


(defn gen-restrict-to-semtype-option [semtype-list]
  (format "--restrict_to_sts %s" (clojure.string/join "," semtype-list)))

(defn handle-match-map [match-map] 
  (hash-map
   :concept-match-start (.getConceptMatchStart match-map)
   :concept-match-end (.getConceptMatchEnd match-map)
   :phrase-match-start (.getPhraseMatchStart match-map)
   :phrase-match-end (.getPhraseMatchEnd match-map)
   :lexical-variation (.getLexMatchVariation match-map) ))

(defn handle-positional-info [position]
  (hash-map
   :start (.getX position)
   :length (.getY position) ))

(defn handle-ev [ev-inst]
  (hash-map
   :conceptid (.getConceptId ev-inst)
   :conceptname (.getConceptName ev-inst)
   :preferredname (.getPreferredName ev-inst)
   :matchedwords (vec (.getMatchedWords ev-inst))
   :head (.isHead ev-inst)
   :overmatch (.isOvermatch ev-inst)
   :position (map handle-positional-info (.getPositionalInfo ev-inst))
   :score (.getScore ev-inst)
   :semtypes (vec (.getSemanticTypes ev-inst))
   :sources (vec (.getSources ev-inst))
   :matchmaplist (map handle-match-map (.getMatchMapList ev-inst))))

(defn handle-utterance [utterance]
  (doall
  (map (fn [pcm]
         (map (fn [mappings]
                (map handle-ev (.getEvList mappings)))
              (.getMappings pcm)))
       (.getPCMList utterance))))

(defn handle-acronym-abbrev [acronym-abbrev]
  (hash-map 
   :acronym (.getAcronym acronym-abbrev)
   :expansion (.getExpansion acronym-abbrev)
   :countlist (vec (.getCountList acronym-abbrev))
   :cuilist (vec (.getCUIList acronym-abbrev))))

(defn handle-result-list [result-list]
  (flatten
   (map (fn [result]
          (list 
           (map handle-acronym-abbrev
                (.getAcronymsAbbrevs result))
           (map handle-utterance
                (.getUtteranceList result))))
        result-list)))

