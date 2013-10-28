(ns chem.grammars
  (:require [instaparse.core :as insta])
  (:gen-class))

;;
;; SKR Chemical Identification Rules
;;
;; legend:
;;   <ws> - whitespace
;;   <an> - alphanumeric
;;   <pn> - punctuation
;;   <uc> - uppercase
;;   <ic> - ignore case
;;   <nu> - numeric
;;

;; This grammar embodies chemical identification rules 1-5 and 7.
(def complex-grammar 
  "sentencelist = sentence*
   sentence = termlist stop ws*
   termlist = term ((ws | punctuation)* term)*
   <term> = rule1 | rule2 | rule3 | rule4 | rule5 | rule7 | polyterm | bracketedterm | nu | word | percentage
   word = uc | ic | an
   rule1 = an ( plus | minus )+
   rule2 = ( ic | uc ) ( plus | minus )+
   rule3 = word ws lparen ( word period* ) rparen 
   rule4 = word minus uc nu
   rule5 = ( ic | uc ) lparen ( nu ( plus | minus )+ ) rparen
   rule7 = ic minus nu
   bracketedterm = lbracket termlist rbracket
   polyterm = polysubterm ( minus | comma ) polysubterm | lparen polysubterm rparen 
   polysubterm = word | nu | polyterm | polysubterm ( minus | comma | ws ) polysubterm | word polysubterm | polysubterm period 
   percentage = nu percent
   measurement = nu word
   uc = #'[A-Z]+'
   ic = #'[A-Za-z]+'
   nu = #'[0-9]+'
   an = #'[A-Za-z0-9]+'
   stop = period | semicolon
   punctuation = comma | ':' | fslash
   comma = ','
   plus = '+' 
   minus = '-'
   lparen = '('
   rparen = ')' 
   lbracket = '['
   rbracket = ']'
   fslash = '/' 
   percent = '%'
   ws = #'\\s+'
   period = '.'
   semicolon = ';'
  ")

(def complex-parser (insta/parser complex-grammar))

(def complex-grammar2
  "sentencelist = sentence*
   sentence = (nu | word) ((ws | punctuation)* (nu | word))* stop ws*
   word = rule1 | rule2 | rule3 | rule4 | rule5 | rule7 | ic | an | polytermrule 
   rule1 = an ( plus | minus )+
   rule2 = ( ic | uc ) ( plus | minus )+
   rule3 = word ws lparen ( polytermrule ) rparen
   rule4 = word minus uc nu
   rule5 = ( ic | uc ) lparen ( nu ( plus | minus )+ ) rparen
   rule7 = ic minus nu
   percentage = nu percent
   polytermrule = nexus (minus nexus)*
   nexus = nu | (ic ws*)+ | nu comma nu | lparen polytermrule rparen | an
   uc = #'[A-Z]'
   an = #'[A-Za-z0-9]+'
   ic = #'[A-Za-z]+'
   nu = #'[0-9]+'
   stop = period | semicolon
   punctuation = comma | ':'
   comma = ','
   plus = '+' 
   minus = '-'
   lparen = '('
   rparen = ')' 
   percent = '%'
   ws = #'\\s+'
   period = '.'
   semicolon = ';'
  ")

(def complex-parser2 (insta/parser complex-grammar2))

(def complex-grammar3
  "termlist = term ((ws | punctuation)* term)*
   <term> = rule1 | rule2 | rule3 | rule4 | rule5 | rule7 | polyterm | bracketedterm | nu | word | percentage
   word = uc | ic | an
   rule1 = an ( plus | minus )+
   rule2 = ( ic | uc ) ( plus | minus )+
   rule3 = word ws lparen ( word period* ) rparen 
   rule4 = word minus uc nu
   rule5 = ( ic | uc ) lparen ( nu ( plus | minus )+ ) rparen
   rule7 = ic minus nu
   bracketedterm = lbracket termlist rbracket
   polyterm = polysubterm ( minus | comma ) polysubterm | lparen polysubterm rparen 
   polysubterm = word | nu | polyterm | polysubterm ( minus | comma | ws ) polysubterm | word polysubterm | polysubterm period 
   percentage = nu percent
   measurement = nu word
   uc = #'[A-Z]+'
   ic = #'[A-Za-z]+'
   nu = #'[0-9]+'
   an = #'[A-Za-z0-9]+'
   stop = period | semicolon
   punctuation = comma | ':' | fslash
   comma = ','
   plus = '+' 
   minus = '-'
   lparen = '('
   rparen = ')' 
   lbracket = '['
   rbracket = ']'
   fslash = '/' 
   percent = '%'
   ws = #'\\s+'
   period = '.'
   semicolon = ';'
  ")

(def complex-parser3 (insta/parser complex-grammar3))


;; Chemical Rules

;;   * Rule #0 Bad ending words are removed from contention before
;;     being checked for chemical validity - here is the current list
;;     of bad endings: "ing", "mic", "tion", "ed", "sis", "ism",
;;     "coccus"

;;     * Following exception words: glutamic, polychlorinated
;;       In this rule, we also remove from further checking any words
;;       found to be in the stopword list database table.

;;   * Rule #1 [50.0] <ws> <an> <pn = '+' or '-'>
;;     e.g., Ca2+ or 13C- 

;;   * Rule #2 [30.0] <ws> <ic> || <uc> <pn = '+' or '-'> <ws>||<+/->||<pn>
;;     e.g., K+ or Na+ or K++ or K+,

;;   * Rule #3 [15.0 / 40.0] <chemical> <ws> <pn = '('> .... <pn = ')'>
;;     e.g., nitric oxide (NO.)

;;     NOTE: This rule only tags potential abbreviations. The
;;     potential abbreviation is than validated during the
;;     abbreviation validation pass which determines whether is stays
;;     and the score is upgraded to 40.0 or it is removed.

;;   * Rule #4 [35.0] <chemical> <pn = '-'> .... <ws> where ... is not <lc> 
;;     e.g., leukotriene-C4

;;   * Rule #5 [45.0] <ws> <ic> || <uc> <pn = '('> <nu> <pn = '+' or '-'> <pn = ')'>
;;     e.g., Ca(2+) 
;;     if match then function returns new token and number tokens used.

;;   * Rule #6 [33.0] <ws> <mc>
;;     e.g., ICa or mRNA *** Still a little weak!!

;;   * Rule #7 [47.0] <ws> <ic>||<uc> <pn = '-'> <nu>
;;     e.g., Leu-4 or LYS-345




;; can we implement handle-terms using transform-options instead?
;;
(def transform-options
  {:sentencelist (comp vec list)
   :sentence (comp vec list)
   :term  identity
   :word  identity
   :rule1 identity
   :rule2 identity
   :rule3 identity
   :rule4 identity
   :rule5 identity
   :rule7 identity
})

(defn complex-parse [input]
  (->> (complex-parser input) (insta/transform transform-options)))

;; do a pre-parse? for chemical terms?

;; graph of parse tree 
;; (insta/visualize (complex-parser input))