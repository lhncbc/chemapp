(ns chem.english-words
  (:require [clojure.java.io :as io]
            [clojure.string :refer [lower-case]])
  (:import (java.lang System))
  (:gen-class))

(def wordsfilefn "data/corncob_lowercase.txt")
(def wordset (atom #{}))

(defn init
  [wordsfilename]
  (let [wordlist (line-seq (io/reader wordsfilename))]
    (reset! wordset (set wordlist))))

(defn is-real-word?
  "Is this a real english word?  The function is case sensitive."
  [word]
  (contains? @wordset word))

(defn is-real-word-ci?
  "Is this a real english word?  The function is case insensitive."
  [word]
  (contains? @wordset (lower-case word)))
