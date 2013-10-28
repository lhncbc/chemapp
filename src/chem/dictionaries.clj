(ns chem.dictionaries)

;; A really simple example of an annotation engine; engine looks for
;; partial chemical dictionary matches.

(def fragment-dictionary
  #{ 
    "dioxide"
    "ethyl"
    "hexose"
    "iose"
    "trioxide"
    "saccharide"
    "methi"
    "methane"
    "thane"
    "hexo"
    "kinase"
    "drogen"
    "ocaine"
    "idine"
    "nitr"
    "phenyl"
    "acetyl"
    "propyl"
    "oxyl"
    "trate"

;; multipliers ,
;; table 11(IUPAC 1996)
;; (http://en.wikipedia.org/wiki/IUPAC_numerical_multiplier)
    "penta"
    "pent"
    "hexa"
    "hex"
    "hepta"
    "hept"
    "mono"
;;  "di"
    "tri"
    "tetra"
    "octa"
    "nona"
    "deca"
    "undeca"
    "dodeca"
    "trideca"
    "tetrodeca"


;; nonsugar lexicon entries

;; a-terms/replacement prefixes
    "oxa"
    "phospha"
    "hydroxy"

;; monomers
;;  "ox"
;;  "phosph"
;;  "ars"
   
;; trival names
   "benzene"
   "thiophene"
   "furan"
   "butene"
   "ribose"

;; class name
   "alkene"
   "alcohol"
   "acid"

;; element names
   "oxygen"
   "hydrogen"
   "carbon"
   "nitrogen"
   "helium"

;; sugar lexicon entries
   "deoxy"
   "amino"
   "thio"

;; trival ring names
   "oxirose"
   "oxetose"
   "furanose"



   


    })


(def prefix-dictionary 
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
    
    ;; trival names
    "lerythrose"
    "threose"
    "ribose"

    })

(def suffix-dictionary
  #{
    ;; functional group suffixes ,
    ;; table 5(IUPAC 1996)
    ;; "ol"
    ;; "one"
    "aldehyde"
    
    ;; saturation parent suffix
    ;; "an"
    ;; "ane"
    
    ;; unsaturation parent suffix
    "ene"
    "yne"
    
    ;; ring stem suffix
    "anose"
    "anos"

    ;; parent structure stem suffixes
    ;; "ose"
    ;; "os"
    "aldose"
    "ulose"
    "ketose"

    

    })