(ns chem.token-partial
  (:use [clojure.string :only (lower-case)])
  (:require [skr.tokenization :as mm-tokenization]
            [chem.dictionaries :as dictionaries]
            [chem.partial :as partial]
            [chem.stopwords :as stopwords]))

(defn has-prefix [text target]
  (= (.indexOf text target) 0))

(defn has-suffix [text target]
  (= (+ (.indexOf text target) (count target)) (count text)))

(defn has-infix [text target]
  (>= (.indexOf text target) 0))

(defn has-chemical-prefix [text]
  (reduce (fn [prefixset prefix]
            (if (has-prefix text prefix)
              (conj prefixset prefix)
              prefixset))
          #{} dictionaries/prefix-dictionary))

(defn has-chemical-suffix [text]
  (reduce (fn [suffixset suffix]
            (if (has-suffix text suffix) 
              (conj suffixset suffix 
                    suffixset)))
          #{} dictionaries/suffix-dictionary))

(defn has-chemical-infix [text]
  (reduce (fn [infixset infix]
             (if (has-infix text infix)
               (conj infixset infix)
               infixset))
          #{} dictionaries/infix-dictionary))

(defn get-annotations-using-dictionary [dictionary text]
  (map (fn [token]
         (partial/get-fragment-annotations-using-dictionary dictionary token))
       (mm-tokenization/analyze-text-chemicals-aggressive text)))

(defn list-possible-chemicals [text]
  (map (fn [token]
         (assoc token
           :stopword (if (contains? stopwords/stopwords (lower-case (:text token))) "yes" "no")
           :has-prefix (has-chemical-prefix (lower-case (:text token)))
           :has-infix (has-chemical-infix (lower-case (:text token)))
           ))
       (mm-tokenization/analyze-text-chemicals-aggressive text)))

