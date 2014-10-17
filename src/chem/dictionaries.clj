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
    "1,1"
    "1,2"
    "1,3"
    "1,4"
    "1,5"
    "1,6"
    "1,7"
    "1,8"
    
    "7,8"
    })

;; element names
(def elements
  #{
    "aluminium"
    "argon"
    "arsenic"
    "astatine"
    "beryllium"
    "bismuth"
    "boron"
    "bromine"
    "cadmium"
    "calcium"
    "carbon"
    "chlorine"
    "chromium"
    "cobalt"
    "copper"
    "deuterium"
    "flourine"
    "gallium"
    "germanium"
    "helium"
    "hydrogen"
    "indium"
    "iodine"
    "iridium"
    "iron"
    "krypton"
    "lithium"
    "magnesium"
    "manganese"
    "mercury"
    "neon"
    "nickel"
    "nitrogen"
    "oxygen"
    "phosphorus"
    "platinum"
    "potassium"
    "radon"
    "scandium"
    "selenium"
    "silver"
    "sodium"
    "sulfur"
    "tim"
    "titanium"
    "vanadium"
    "zinc"
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
    "urea"

    "benzo"                             ;does this really belong here?
    "chloro"                            ;does this really belong here?

    "isochromanyl"
    "chromanyl"
    "pyrrolidinyl"
    "pyrrolinyl"
    "imidazolidinyl"
    "imidazolinyl"
    "pyrazolidinyl"
    "pyrazolinyl"
    "piperidyl"
    "piperazinyl"
    "indolinyl"
    "isoindolinyl"
    "quinuclidinyl"
    "morpholinyl"
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
    "hydratase"
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
    "teine"
    "osine"
    "metha"
    "cryl"

    "glutaryl"

;; monomers
;;  "ox"
;;  "phosph"
;;  "ars"

    "ferrous"
    "sulfate"
    "acetate"
    "lactate"
    "decone"
    "kepone"
    "cyclo"
    "lidene"
    "phosph"
    "tidyl"
    "choline"
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

    ;; uncategorized
    "imida"
    "thia"
    
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


(def infix-dictionary
 #{
   "azol"
   "yl"
})
