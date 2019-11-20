(ns chem.opennlp
  (:require [opennlp.nlp :as nlp]
            [clojure.string :as string]))

(defonce get-sentences (nlp/make-sentence-detector "data/models/en-sent.bin"))
(defonce tokenize      (nlp/make-tokenizer "data/models/en-token.bin"))
(defonce pos-tag       (nlp/make-pos-tagger "data/models/en-pos-maxent.bin"))

