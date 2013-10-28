(ns chem.stats)

;; (def entities (chem.chemdner-tools/load-training-document-entity-gold-standard paths/training-entities))
;; (def suffix-stat-map (chem.stats/suffix-stats (map #(second %) entities)))
;; (def suffix-term-map (chem.stats/suffix-terms (map #(second %) entities)))
;; 

(def prefixes [
;; multipliers
;; table 11 (IUPAC 1996)
               "penta"
               "pent"
               "hexa"
               "hex"
               "hepta"
               "hept"
               "mono"
               "di"
               "tri"
               "tetra"
               "octa"
               "nona"
               "deca"
               "undeca"
               "dodeca"
               "trideca"
               "tetrodeca"
;;  a-terms/replacement prefixes
               "oxa"
               "phospha"
               "hydroxy"
;; monomers
               "ox"
               "phosph"
               "ars"
;; cyclo -- prefix
               "cyclo"
;; stereo prefixes
               "cis"
               "(z)"
               ])

(def suffixes  [
                "oxide"
                "dide"
                "nide"
                "mide"
                "ride"
                "side"
                "ide"
                "hyde"
                "ium"
                "dide"
                "mide"
                "ride"
                "ide"
                "phate"
                "trate"
                "mate"
                "rate"
                "tate"
                "ate"
                "tin"
                "ane"
                "ene"
                "enes"
                "fen"
                "en"
                "ic"
                "cine"
                "dine"
                "eine"
                "line"
                "mine"
                "nine"
                "rine"
                "sine"
                "ine"
                "lin"
                "cin"
                "nin"
                "oin"
                "pin"
                "rin"
                "sin"
                "tin"
                "vin"
                "xin"
                "zin"
                "in"
                "trile"
                "cil"
                "nil"
                "il"
                "hite"
                "nite"
                "rite"
                "tite"
                "fite"
                "lite"
                "kite"
                "mite"
                "zite"
                "ite"
                "-one"
                "cone"
                "done"
                "fone"
                "gone"
                "ione"
                "lone"
                "mone"
                "none"
                "pone"
                "rone"
                "sone"
                "hone"
                "tone"
                "vone"
                "xone"
                "zone"
                "-ol"
                "aol"
                "hol"
                "iol"
                "nol"
                "rol"
;; functional group suffixes
;; table 5 (IUPAC 1996)
                "ol"
                "one"
                "aldehyde"
;; ring stem suffix
                "anose"
                "anos"
;; parent structure stem suffixes
                "ose"
                "os"
                "aldose"
                "ulose"
                "acid"
                ])

;; element names
(def elements [
               "oxygen"
               "hydrogen"
               "carbon"
               "nitrogen"
               "helium"
               "titanium"
               "cadmium"
               "nickel"
               ])



(defn suffix-stats [termlist]
  (reduce (fn [newmap term]
            (let [suffix (some #(re-find (java.util.regex.Pattern/compile (format "%s$" %)) (.toLowerCase term))
                               suffixes)]
              (if suffix 
                (assoc newmap suffix (if (contains? newmap suffix)
                                       (+ (newmap suffix) 1)
                                       1))
                newmap)))
          {} termlist))

(defn suffix-terms [termlist]
  (reduce (fn [newmap term]
            (let [suffix (some #(re-find (java.util.regex.Pattern/compile (format "%s$" %)) (.toLowerCase term))
                               suffixes)]
              (if suffix 
                (assoc newmap suffix (if (contains? newmap suffix)
                                       (conj (newmap suffix) term)
                                       (list term)))
                newmap)))
          {} termlist))
                   

(defn element-stats [termlist]
  (reduce (fn [newmap term]
            (let [suffix (some #(re-find (java.util.regex.Pattern/compile (format "%s" %)) (.toLowerCase term))
                               elements)]
              (if suffix 
                (assoc newmap suffix (if (contains? newmap suffix)
                                       (+ (newmap suffix) 1)
                                       1))
                newmap)))
          {} termlist))