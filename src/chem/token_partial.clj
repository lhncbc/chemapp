(ns chem.token-partial
  (:require [chem.metamap-tokenization :as mm-tokenization])
  (:require [chem.dictionaries :as dictionaries]))

(defn has-prefix [text target]
  (= (.indexOf text target) 0))

(defn has-suffix [text target]
  (= (+ (.indexOf text target) (count target)) (count text)))

(defn has-infix [text target]
  (>= (.indexOf text target) 0))

(defn has-chemical-prefix [text]
  (reduce (fn [status prefix]
            (or (has-prefix text prefix) status))
          false dictionaries/prefix-dictionary))

(defn has-chemical-suffix [text]
  (reduce (fn [status prefix]
            (or (has-suffix text prefix) status))
          false dictionaries/suffix-dictionary))




(defn get-annotations-using-dictionary [dictionary text]
  (map (fn [token]
         
         token )
       (mm-tokenization/analyze-text text)))
       

