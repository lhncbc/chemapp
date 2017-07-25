(ns chem.greek-convert
  (:require [clojure.string :as string :refer [join]]))

;; ΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩΪΫάέήίΰαβγδεζηθικλμνξοπρςστυφχψωϊϋόύώϏϐϑϒϓϔϕϖϗϘϙϚϛϜϝϞϟϠϡϢϣϤϥϦϧϨϩϪϫϬϭϮϯϰϱϲϳϴϵ϶ϷϸϹϺϻϼϽϾϿ

(def greek-conversion-map
  {
   \Κ "kappa"
   \Λ "lambda"
   \Μ "mu"
   \Ν "nu"
   \Ξ "xi"
   \Ο "omicron"
   \Π "pi"
   \Ρ "rho"
   \Σ "sigma"
   \Τ "tau"
   \Υ "upsilon"
   \Φ "phi"
   \Χ "chi"
   \Ψ "psi"
   \Ω "omega"
   \α "alpha"
   \β "beta"
   \γ "gamma"
   \δ "delta"   
   \ε "epsilon"
   \ζ "zeta"
   \η "eta"
   \θ "theta"
   \ι "iota"
   \κ "kappa"
   \λ "lambda"
   \μ "mu"
   \ν "nu"
   \ξ "xi"
   \ο "omicron"
   \π "pi"
   \ρ "rho"
   \ς "sigma"
   \σ "sigma"
   \τ "tau"
   \υ "upsilon"
   \φ "phi"
   \χ "chi"
   \ψ "psi"
   \ω "omega"
   })

(defn convert-greek-chars
  "Convert any UTF-8 greek characters into their expanded forms."
  [term]
  (join 
   (mapv (fn [ch]
           (if (contains? greek-conversion-map ch)
             (greek-conversion-map ch)
             ch))
         term)))

