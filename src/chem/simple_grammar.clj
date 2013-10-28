(ns chem.simple-grammar
  (:require [instaparse.core :as insta])
  (:gen-class))

;; A simple grammar for handling pre-seqmented sentences,  tokens only version of grammar.
(def simple-grammar
  "sentencelist = sentencelist | sentence
   sentence = term ((space | punctuation)* term)* space* 
   term = uc | ic | an | nu | polytermrule
   polytermrule = nexus (minus nexus)+ 
   nexus = nu | (ic space*)+ | nu comma nu | lparen polytermrule rparen | an
   ic = #'[A-Za-z]+'
   uc = #'[A-Z]+'
   an = #'[A-Z][A-Za-z0-9]+'
   nu = #'[0-9]+'
   stop = period | semicolon
   punctuation = '-' | ',' | '+' | '-' | ':' | period
   space = #'\\s+'
   plus = '+' 
   minus = '-'
   lparen = '('
   rparen = ')' 
   period = '.'
   semicolon = ';'
   comma = ','
  ")

(def simple-parser (insta/parser simple-grammar))

