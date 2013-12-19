(ns chem.mti-filtering
  (:require [chem.rrf-mongodb :as rrfdb])
  (:require [somnium.congomongo :as m]))

;; keep concepts with these semantic types
(def valid-semtypes 
  #{ "T109"                             ;Organic Chemical
     "T115"                             ;Organophosphorus Compound
     "T121"                             ;Pharmacologic Substance
     "T126"                             ;Enzyme
     })

;; exclude concepts with these semantic types
(def exclude-semtypes
  #{ "T116"                             ;Amino Acid, Peptide, or Protein
     ;; "T125"                             ;Hormone
     })

;; Elements
;; A single chemical structure diagram: single atoms, ions, isotopes,
;; pure elements and molecules:

(def element-cuis
  #{ "C0016330"                         ; Fluorine
     "C0302583"                         ; Iron
     "C0011744"                         ; Deuterium
     "C0005036"                         ; Benzene
     "C0034255"                         ; Pyridine
     "C0030054"                         ; Oxygen
     "C0040302"                         ; Titanium
     "C0006632"                         ; Cadmium
     "C0007009"                         ; Carbon
     "C0028158"                         ; Nitrogen
     "C0018880"                         ; Helium
     "C0020275"                         ; Hydrogen
     "C0037473"                         ; Sodium
     "C0597484"                         ; Sodium+ (isotope?)
     "C0025424"                         ; Mercury
     "C2347108"                         ; mercury+
     "C2348272"                         ; Mercury
     "C0004749"                         ; Barium
     "C0018026"                         ; Gold


                                        ; Others?
     })

(defn is-element? [cui] (contains? element-cuis cui))

;; P3. Small Biochemicals
;; -Sacharids: monosaccharides, disaccharides and trisaccharides should be tagged.

;; object side of relation in MRREL "cui1" (I believe)
(def small-biochemicals-isa-object-cuis
  ^{:doc "concepts for monosaccharides, disaccharides, or aminoglycosides (aminoglycoside trisaccharides)." }
  #{ "C0026492"                         ; "monosaccharide"
     "C0012611"                         ; "disaccharide"
     "C0002556"                         ; "aminoglycoside" (aminoglycoside trisaccharide)
    })

(def small-biochemical-cuis 
  ^{:doc "cuis that are marked as monosaccharides, disaccharides, or aminoglycosides (aminoglycoside trisaccharides)." }
  #{"C0977110" "C0978023" "C0019494" "C0977122" "C0977112" "C0977113"
    "C0027780" "C0978026" "C0776374" "C0013283" "C0776375" "C0041080"
    "C0977115" "C2698010" "C0007630" "C2825830" "C0789653" "C0776387"
    "C1572491" "C1300274" "C0689741" "C0977106" "C0981451" "C2337157"
    "C0977118" "C0001055" "C1621993" "C0781671" "C0687830" "C0016786"
    "C0001056" "C0776402" "C0977109" "C0982873" "C0546866" "C0019229"
    "C0787348" "C0037688" "C0038425" "C0002499" "C0982874" "C0067762"
    "C0043375" "C0022487" "C0016943" "C2339901" "C0001268" "C0872534"
    "C0052231" "C0024742" "C0027603" "C0002556" "C0038636" "C0017725"
    "C0008947" "C0982887" "C0980368" "C0040341" "C0067995" "C0016945"
    "C0785911" "C0978928" "C0688958" "C0030894" "C0370101" "C0024658"
    "C0060991" "C0030576" "C0035417" "C2981338" "C0039707" "C0980746"
    "C0017718" "C0061444" "C1572745" "C0021547" "C3555616" "C2323726"
    "C0687829" "C0023726" "C0022637" "C0682934" "C0022957" "C0262967"
    "C0022949" "C0040815"})

(defn is-small-biochemical? [cui] (contains? small-biochemical-cuis cui))

(def peptide-cuis
  ^{:doc "concepts that are peptides (cui: C0030956)" }
  #{"C1512030" "C1513311" "C0211011" "C1997653" "C2585265" "C0036774"
    "C0017953" "C1305923" "C0582246" "C0304925" "C0012512" "C0486843"
    "C0991769" "C0033603" "C1291124" "C2986708" "C0001924" "C1291125"
    "C0293227" "C0251515" "C0030957" "C1519061"})

(defn is-peptide? [cui] (contains? peptide-cuis cui))

(def nucleotide-cuis
  ^{:doc "concepts that are nucleotides (cui: C0028630)" }
  #{"C0207164" "C0027270" "C0018340" "C0018353" "C0057470" "C0001480"
    "C0009226" "C2827108" "C0028953" "C0027303" "C0032550" "C0001465"
    "C0035548" "C0011529"})

(defn is-nucleotide? [cui] (contains? nucleotide-cuis cui))

(def polymer-cuis
  ^{:doc "concepts that are Polymers (cui: C0032521)"}
  #{"C0596383" "C0005554" "C0005533" "C0032483" "C0032167" "C0440257"
    "C1720305" "C1563732" "C1881413" "C0068943" "C0071443" "C0032602"
    "C0032856" "C1881507" "C0175905" "C0616192"})

(defn is-polymer? [cui] (contains? polymer-cuis cui))

(def mineral-cuis
  #{ } )


(def organic-nitrogen-compound-cuis
  #{ "C0042523" } )

(def laboratory-reagents-cuis #{ })
(def dye-and-indicator-name-cuis  #{ })


(defn semantic-types-records-for-cui [cui]
  (m/fetch :mrsty :where {:cui cui}))

(defn tuilist-for-cui [cui]
  (map #(:tui %)
       (semantic-types-records-for-cui cui)))

(defn stylist-for-cui [cui]
  (map #(:sty %)
       (semantic-types-records-for-cui cui)))

(defn list-stylist-for-record [record]
  (stylist-for-cui (:cui record)))

(defn list-valid-semtypes-for-record [record]
  (let [cui (:cui record)]
    (filter #(contains? valid-semtypes (:tui %))
            (m/fetch :mrsty :where {:cui cui}))))

(defn list-exclude-semtypes-for-record [record]
  (let [cui (:cui record)]
    (filter #(contains? exclude-semtypes (:tui %))
            (m/fetch :mrsty :where {:cui cui}))))

(defn is-valid-chemical-by-semtype [record]
  (< 0 (count (list-valid-semtypes-for-record record))))

(defn is-excluded-chemical [record]
  (< 0 (count (list-exclude-semtypes-for-record record))))

(defn is-small-biochemical [record]
  (contains? small-biochemical-cuis (:cui record)))

(defn is-from-mmi [record]
  (contains? (set (:paths record)) "MM"))

(defn is-from-mmi-only [record]
  (= (:paths record) ["MM"]))

(defn is-from-mmi-rc [el]
  (= (set (:paths el)) (set ["RC" "MM"])))

(defn is-chemical? [record]
  (and (is-from-mmi record)
       (or (is-small-biochemical record)
           (contains? peptide-cuis (:cui record))
           (contains? nucleotide-cuis (:cui record))
           (contains? polymer-cuis (:cui record))
           (contains? element-cuis (:cui record))
           )))

;; (and (not (is-excluded-chemical record))
;;                (is-valid-chemical-by-semtype record))

(defn split-records [mti-result-lineseq]
  (map #(vec (.split % "\\|" -1)) mti-result-lineseq))

(defn convert-detailed-mti-format [mti-split-records]
  (map (fn [fields]
         {:docid (nth fields 0)
          :term (nth fields 1)
          :cui (nth fields 2)
          :score (nth fields 3)
          :type  (nth fields 4)
          :misc (vec (.split (nth fields 5) "\\;" -1))
          :location (nth fields 4)
          :paths (vec (.split (nth fields 4) "\\;" -1)) })
       mti-split-records))

(defn convert-full-detailed-mti-format  [mti-split-records]
  (filter #(not (nil? %))
          (map (fn [fields]
                 (when (>= (.indexOf (nth fields 1) ":") 0)
                   {:docid (nth fields 0)
                    :rank (vec (.split (nth fields 1) "\\:" -1))
                    :term (nth fields 2)
                    :cui (nth fields 3)
                    :score (nth fields 4)
                    :type (nth fields 5)
                    :misc (vec (.split (nth fields 6) "\\;" -1))
                    :location (nth fields 7)
                    :paths (vec (.split (nth fields 8) "\\;" -1)) }))
               mti-split-records)))

(defn remove-nonuseful-records [mti-resultseq]
  (filter #(> (count %) 4) mti-resultseq))

(defn gen-mti-detailed-resultseq [filename]
  (-> filename
      chem.utils/line-seq-from-file
      split-records
      remove-nonuseful-records
      convert-detailed-mti-format))

(defn gen-mti-full-detailed-resultseq [filename]
  (-> filename
      chem.utils/line-seq-from-file
      split-records
      remove-nonuseful-records
      convert-full-detailed-mti-format))

(defn filter-mti-results [mti-resultseq]
  (filter is-chemical? mti-resultseq))

(defn convert-mti-score [score]
  "convert score between 100000-0 to 1.0-0.0"
   (/ (max 1
           (min 100000
                (float (java.lang.Integer/parseInt score))))
    100000.0))

(defn mti-to-chemdner-result [idx result]
  [(:docid result) (:term result) (inc idx) (format "%8f" (convert-mti-score (:score result)))])

(defn gen-chemdner-result-list [mti-resultseq]
  (map-indexed mti-to-chemdner-result mti-resultseq))

(defn gen-training-annotations-map [annotation-list]
  "build map of mti or training annotations by docid"
  (reduce (fn [newmap el]
            (let [docid (:docid el)
                  term  (:term el)]
              (if (contains? newmap docid)
                (assoc newmap docid (conj (newmap docid) term))
                (assoc newmap docid (set (list term))))))
          {} annotation-list))

(defn gen-mti-resultseq-map [mti-resultseq]
  (reduce (fn [newmap el]
            (let [docid (:docid el)]
              (if (contains? newmap docid)
                (assoc newmap docid (conj (newmap docid) el))
                (assoc newmap docid (set (list el))))))
          {} mti-resultseq))

(defn count-rc-only-results [mti-resultseq]
  "count number of results from Related Citations only."
  (count (filter #(= (:paths %) ["RC"])
                 mti-resultseq)))

(defn count-rc-results [mti-resultseq]
  "count number of results from Related Citations."
  (count (filter #(contains? (set (:paths %)) "RC")
                 mti-resultseq)))

(defn count-mm-only-results [mti-resultseq]
  "count number of results from MetaMap Indexing only."
  (count (filter is-from-mmi-only mti-resultseq)))

(defn count-mm-results [mti-resultseq]
  "count number of results from MetaMap Indexing"
  (count (filter is-from-mmi mti-resultseq)))

(defn count-mm-rc-results [mti-resultseq]
  "count number of results that include both Related Citations and MetaMap Indexing"
  (count (filter is-from-mmi-rc mti-resultseq)))

;; user> (def mti-full-detailed-resultseq (chem.mti-filtering/gen-mti-full-detailed-resultseq "chemdner_abs_training.sldiwi.ascii.mti.noAddForced.out"))
;; 'user/mti-full-detailed-resultseq
;; user> (nth mti-full-detailed-resultseq 0)
;; {:rank ["1" "0"], :score "83403", :cui "C0039601", :location "TI", :misc ["RtM via: Testosterone" "RtM via: Serum testosterone measurement"], :term "Testosterone", :paths ["MM" "RC"], :type "MH", :docid "23104419"};
;; user> (def chemdner-gold (chem.chemdner-tools/load-training-document-entity-gold-standard "/nfsvol/nlsaux16/II_Group_WorkArea/Lan/projects/BioCreative/2013/CHEMDNER_TRAIN_V01/cdi_ann_training_13-07-31.txt"))
;; #'user/chemdner-gold
;; user> (count chemdner-gold)
;; 15886
;; user> (first chemdner-gold)
;; {:docid "21826085", :term "haloperidol"}

;; user> (def mti-filtered-resultseq (chem.mti-filtering/filter-mti-results mti-full-detailed-resultseq))
;; #'user/mti-filtered-resultseq
;; user> (first mti-full-detailed-resultseq)
;; {:rank ["1" "0"], :score "83403", :cui "C0039601", :location "TI", :misc ["RtM via: Testosterone" "RtM via: Serum testosterone measurement"], :term "Testosterone", :paths ["MM" "RC"], :type "MH", :docid "23104419"}
;; user> (count mti-filtered-resultseq)
;; user> (count mti-filtered-resultseq)
;; 314506
;; user> (def chemdner-gold-map (chem.mti-filtering/gen-training-annotations-map chemdner-gold))
;; #'user/chemdner-gold-map
;; user> (def mti-filtered-term-map (chem.mti-filtering/gen-training-annotations-map mti-filtered-resultseq))
;; #'user/mti-filtered-term-map
;; user> (def mti-term-map (chem.mti-filtering/gen-training-annotations-map mti-resultseq))
;; #'user/mti-term-map
;; user> (chemdner-gold-map "23104419")
;; #{"lactate" "estradiol" "creatine" "testosterone" "androgen" "Androgen" "methimazole"}
;; user> (mti-filtered-term-map "23104419")
;; #{"*Methimazole"}
;; user> (mti-term-map "23104419")
;; #{"*Hypothyroidism" "*Hexokinase" "*mRNA" "*Creatine Kinase" "*Lactation" "*Testosterone" "AR protein, human" "Humans" "*Muscle, Skeletal" "*Drinking Water" "*Muscle, Cardiac" "Female" "*Androgens" "Pregnancy" "*Estradiol" "Rats" "*Glucose Transporter Type 4" "*Lactate Dehydrogenase" "*Phenotypes" "*Methimazole" "*Androgen Receptor" "Animals" "*Forelimb" "Adult"}
;; user> 


;; running MTI:
;;
;; $ /nfsvol/nls/bin/MTI -doNoAddForced /tmp/23550066.txt
;; 00000000|Oxidoreductases, N-Demethylating|C0030017|44326|MH|RtM via: CYP2B6 protein, human|TX|MM;RC
;; 00000000|Microsomes, Liver|C0026030|31422|MH|RtM via: Microsomes, Liver|TX|MM;RC
;; 00000000|Aryl Hydrocarbon Hydroxylases|C0003927|30101|MH|RtM via: CYP2B6 protein, human|TX|MM;RC
;; 00000000|Cytochrome P-450 Enzyme System|C0010762|10633|MH|||RC
;; 00000000|Thiotepa|C0039871|7474|MH|RtM via: Thiotepa|TX|MM;RC
;; 00000000|Mixed Function Oxygenases|C0020364|5449|MH|||RC
;; 00000000|Triethylenephosphoramide|C0039533|4781|MH|||RC
;; 00000000|Cytochrome P-450 CYP3A|C0059563|4571|MH|||RC
;; 00000000|Steroid Hydroxylases|C0038311|4280|MH|||RC
;; 00000000|Steroid 16-alpha-Hydroxylase|C0072084|3328|MH|||RC
;; 00000000|Hydroxylation|C0020365|3140|MH|||RC
;; 00000000|Troleandomycin|C0041165|2943|MH|RtM via: Troleandomycin|TX|MM;RC
;; 00000000|Mephenytoin|C0025381|2896|MH|||RC
;; 00000000|Ketamine|C0022614|1284|MH|RtM via: Ketamine;RtM via: norketamine|TX|MM;RC
;; 00000000|Cytochrome P-450 CYP1A2|C0207509|1235|MH|||RC
;; 00000000|Kinetics|C0022702|1208|MH|RtM via: Kinetics;RtM via: Clearance [PK]|TX|MM;RC
;; 00000000|Ketoconazole|C0022625|878|MH|||RC
;; 00000000|Oxygenases|C0030065|704|MH|||RC
;; 00000000|Biotransformation|C0005576|699|MH|||RC
;; 00000000|Enzyme Inhibitors|C0014432|533|MH|||RC
;; 00000000|Midazolam|C0026056|498|MH|||RC
;; 00000000|Methylation|C0025723|467|MH|RtM via: demethylation|TX|MM;RC
;; 00000000|Isoenzymes|C0022173|464|MH|||RC
;; 00000000|Enzymes|C0014442|414|MH|RtM via: Enzymes|TX|MM
;; 00000000|Sertraline|C0074393|361|MH|||RC
;; ------
;;
;;
;; Using the scheduler with  doNoAddForced Full 
;;
;; 23550066|1:0|Microsomes, Liver|C0026030|62953|MH|RtM via: Microsomes, Liver|TI|MM;RC
;; 23550066|2:1|Oxidoreductases, N-Demethylating|C0030017|52821|MH|RtM via: CYP2B6 protein, human|TI|MM;RC
;; 23550066|3:2|Thiotepa|C0039871|39016|MH|RtM via: Thiotepa|TI|MM;RC
;; 23550066|4:3|Aryl Hydrocarbon Hydroxylases|C0003927|36102|MH|RtM via: CYP2B6 protein, human|TI|MM;RC
;; 23550066|5:4|Troleandomycin|C0041165|17880|MH|RtM pvia: Troleandomycin|TI|MM
;; 23550066|6:5|Cytochrome P-450 Enzyme System|C0010762|14174|MH|||RC
;; 23550066|7:6|Ketamine|C0022614|13817|MH|RtM via: Ketamine;RtM via: norketamine|TI|MM;RC
;; 23550066|8:7|Kinetics|C0022702|9959|MH|RtM via: Kinetics;RtM via: Clearance [PK]|TI|MM;RC
;; 23550066|9:8|Cytochrome P-450 CYP3A|C0059563|5637|MH|||RC
;; 23550066|10:9|Mixed Function Oxygenases|C0020364|5619|MH|||RC
;; 23550066|11:10|norketamine|C0068996|5488|NM||TI|MM