(ns chem.opennlp
  (:require [opennlp.nlp :as nlp]
            [clojure.string :as string]))

(def get-sentences (atom nil))
(def tokenize      (atom nil))
(def pos-tag       (atom nil))

(defn init
  [root-path]
  (reset! get-sentences (nlp/make-sentence-detector (str root-path "data/models/en-sent.bin")))
  (reset! tokenize      (nlp/make-tokenizer (str root-path "data/models/en-token.bin")))
  (reset! pos-tag       (nlp/make-pos-tagger (str root-path "data/models/en-pos-maxent.bin"))))
