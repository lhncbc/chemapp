(ns chem.new-metamap-api
  (:import (gov.nih.nlm.nls.metamap AcronymsAbbrevs
                                    ConceptPair
                                    Ev
                                    MatchMap
                                    MetaMapApi
                                    MetaMapApiImpl
                                    Mapping
                                    Negation
                                    PCM
                                    Phrase
                                    Position
                                    Result
                                    Utterance))
  (:gen-class))

;; A incomplete Clojure wrapper around the MetaMap Java API.
;;
;; metamap_api.clj, Fri Mar 28 16:43:56 2014, edit by Will Rogers
;;
;; really trival example of use:
;; (ns chem.metamap-api)
;; (def mmapi (api-instantiate))
;; (clojure.pprint/pprint (handle-result-list (process-string mmapi "calcium")))
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
  ([^String hostname] (new MetaMapApiImpl hostname)))

;; One way to convert an UTF-8 string to ascii 
;;    String utf = "Some UTF-8 String";  
;;    byte[] data = utf.getBytes("ASCII");  
;;    String ascii = new String(data);  

(defn utf8-to-ascii
  [^String text]
  (String. (.getBytes text "ASCII")))

(defn process-string
  ([^MetaMapApi mmapi ^String text]  (.processCitationsFromString mmapi text))
  ([^MetaMapApi mmapi ^String text ^String options]
     (when options 
       (.setOptions mmapi options))
     (.processCitationsFromString mmapi text)))

(defn process-utf8-string 
  "Convert string to ASCII before sending it to MetaMap."
  ([mmapi text]         (process-string mmapi(utf8-to-ascii text)))
  ([mmapi text options] (process-string mmapi (utf8-to-ascii text) options)))

(defn gen-restrict-to-semtype-option [semtype-list]
  (format "--restrict_to_sts %s" (clojure.string/join "," semtype-list)))

(defn handle-match-map
  [^MatchMap match-map] 
  (hash-map
   :concept-match-start (.getConceptMatchStart match-map)
   :concept-match-end (.getConceptMatchEnd match-map)
   :phrase-match-start (.getPhraseMatchStart match-map)
   :phrase-match-end (.getPhraseMatchEnd match-map)
   :lexical-variation (.getLexMatchVariation match-map) ))

(defn handle-positional-info
  [^Position position]
  (hash-map
   :start (.getX position)
   :length (.getY position) ))

(defn handle-ev
  [^Ev ev-inst]
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

(defn handle-utterance
  "Get Phrases and 
   Mappings Evaluation List (EvList)"
   [^Utterance utterance]
  (doall
   (hash-map :id (.getId utterance)
             :utterance (.getString utterance)
             :position  (handle-positional-info (.getPosition utterance))
             :phrase-mappings-list
             (map (fn [^PCM pcm]
                    (hash-map :phrase (hash-map :phrase-text (.getPhraseText (.getPhrase pcm))
                                                :mincoman (.getMincoManAsString (.getPhrase pcm))
                                                :position (handle-positional-info (.getPosition (.getPhrase pcm))))
                              :mappings (map (fn [^Mapping mappings]
                                               (hash-map :ev-list
                                                         (map handle-ev (.getEvList mappings))))
                                             (.getMappings pcm))))
                  (.getPCMList utterance)))))

(defn handle-acronym-abbrev
  [^AcronymsAbbrevs acronym-abbrev]
  (hash-map 
   :acronym (.getAcronym acronym-abbrev)
   :expansion (.getExpansion acronym-abbrev)
   :countlist (vec (.getCountList acronym-abbrev))
   :cuilist (vec (.getCUIList acronym-abbrev))))

(defn handle-result-list [result-list]
  (flatten
   (map (fn [result]
          (map (fn [^Utterance utterance]
                 (reduce (fn [newulist ^PCM pcm]
                           (concat newulist 
                                   (reduce (fn [newlist ^Mapping mappings]
                                             (concat newlist 
                                                     (map handle-ev (.getEvList mappings))))
                                           [] (.getMappings pcm))))
                         [] (.getPCMList utterance)))
               (.getUtteranceList result)))
        result-list)))

(defn get-aa-and-mappings-only
  "return only acronyms and mappings"
   [result-list]
  (vec
   (map (fn [^Result result]
          (hash-map :acronym-abbrev-list (map handle-acronym-abbrev
                                              (.getAcronymsAbbrevs result))
                    :mappings-list (map handle-utterance
                                        (.getUtteranceList result))

                    ))
        result-list)))

(defn get-ev-from-resultlist
  [result-list]
  (map (fn [result]
         (map (fn [utterance]
                (map (fn [pcm] 
                       (map (fn [mapping] 
                              (map (fn [ev-list]
                                     ev-list)
                                   (:ev-list mapping)))
                            (:mappings pcm))
                       (:phrase-mappings-list utterance))))
                     (result :utterance-list)))
       result-list))

