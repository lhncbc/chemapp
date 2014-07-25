(ns chem.dictionaries)

;; A really simple example of an annotation engine; engine looks for
;; partial chemical dictionary matches.

;; multipliers ,
;; table 11(IUPAC 1996)
;; (http://en.wikipedia.org/wiki/IUPAC_numerical_multiplier)
(def multipliers
  #{"deca"
    "dodeca"
    "hept"
    "hepta"
    "hex"
    "hexa"
    "nona"
    "octa"
    "pent"
    "penta"
    "tetra"
    "tetrodeca"
    "trideca"
    "undeca"
    ;; "di"
    ;; "mono"
    })

;; element names
(def elements
  #{
   "carbon"
   "deuterium"
   "helium"
   "hydrogen"
   "iron"
   "nitrogen"
   "oxygen"
   })

;; trival names
(def trival-names
  #{
    "benzene"
    "butene"
    "fluorine"
    "furan"
    "lerythrose"
    "pyridine"
    "ribose"
    "thiophene"
    "threose"
    })    

(def fragment-suffix-dictionary
  #{
    "dioxide"
    "hexose"
    "iose"
    "trioxide"
    "saccharide"
    "actate"
    "kinase"
    "drogen"
    "ocaine"
    "idine"
    "crylate"
    "citric"
    })

;; sugar lexicon entries
(def sugar-lexicon #{ "deoxy" "amino" "thio"})

;; nonsugar lexicon entries
(def non-sugar-lexicon #{})

;; class name
(def class-names #{"alkene" "alcohol" "acid"})


;; trival ring names
(def trival-ring-names #{"oxirose" "oxetose" "furanose"})

(def fragment-dictionary
  (clojure.set/union multipliers elements trival-names 
                     sugar-lexicon non-sugar-lexicon 
                     class-names trival-ring-names
                     fragment-suffix-dictionary
                     
  #{ 
    "ethyl"
    "methi"
    "methane"
    "thane"
    "hexo"
    "nitr"
    "phenyl"
    "acetyl"
    "propyl"
    "oxyl"

    ;; "trate"
    "glycol"

;; a-terms/replacement prefixes
    "oxa"
    "phospha"
    "hydroxy"

;; monomers
;;  "ox"
;;  "phosph"
;;  "ars"


    }))


(def prefix-dictionary 
  (clojure.set/union multipliers elements trival-names
  #{
    ;; cyclo - prefix
    "cyclo"
    
    ;; stereo prefixes
    "cis"
    "(z)"

    ;; configurational prefixes
    "glycero"
    "erythro"
    "threo"
    
    }))

;; functional group suffixes ,
;; table 5(IUPAC 1996)
(def functional-group-suffixes
  #{"ol" "one" "aldehyde"})

;; saturation parent suffix
;; "an"
(def saturation-parent-suffix #{"ane"} )

    ;; unsaturation parent suffix
(def unsaturation-parent-suffix
  #{"ene" "yne"})

;; ring stem suffix
(def ring-stem-suffix #{"anose" "anos"})

;; parent structure stem suffixes
(def parent-structure-stem-suffixes
  #{
    ;; "ose"
    ;; "os"
    "aldose"
    "ulose"
    "ketose"

    "lidine"
    "iminium"})

(def suffix-dictionary
  (clojure.set/union functional-group-suffixes
                     saturation-parent-suffix
                     unsaturation-parent-suffix
                     ring-stem-suffix
                     parent-structure-stem-suffixes
                     fragment-suffix-dictionary
  #{
     ;; -ine is a suffix used in chemistry to denote two kinds of
;; -substance. The first is a chemically basic and alkaloidal
;; -substance
    "ine"

    }))

(defn corpus-targetset [records]
  (set (flatten (map #(into [] (:chemdner-gold-standard %)) records))))
