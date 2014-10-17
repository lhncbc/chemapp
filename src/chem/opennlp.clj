(ns chem.opennlp
  (:require [opennlp.nlp :as nlp]
            [clojure.string :as string]))

(defonce get-sentences (nlp/make-sentence-detector "models/en-sent.bin"))
(defonce tokenize      (nlp/make-tokenizer "models/en-token.bin"))
(defonce pos-tag       (nlp/make-pos-tagger "models/en-pos-maxent.bin"))

