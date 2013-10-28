(ns chem.rules
  (:require [instaparse.core :as insta])
  (:require [chem.mongodb :as mongodb])
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
;; 




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


;; if word-type is rule<n> and one of the chemical rules
(defn chemical-rule? [word-type]
  (contains? #{:rule1 :rule2 :rule3 :rule4 :rule5 :rule7} word-type))

(defn chemical? [word]
  (> (count (mongodb/lookup :normchem word)) 0))


(def badending-set #{"ing", "mic", "tion", "ed", "sis", "ism",
     "coccus"})

(defn has-badending-v1? [word]
  (and (not (or (= "glutamic" word) (= "polychlorinated" word)))
       (> (.length word) 6)
       (or (contains? badending-set (.substring word (- (.length word) 3)))
           (contains? badending-set (.substring word (- (.length word) 4)))
           (contains? badending-set (.substring word (- (.length word) 6))))))

(defn has-badending-v2? [word]
  (not (every? #(nil? %)
               (map (fn [ending]
                      (re-find (java.util.regex.Pattern/compile (format "%s$" ending)) word))
                      badending-set))))
    
(defn has-badending? [word]
  (some #(re-find (java.util.regex.Pattern/compile (format "%s$" %)) word)
            badending-set))

(defn concat-token-text [token-list]
  (clojure.string/join (map #(second %) token-list)))

(defn handle-word-1 [item]
  (let [word-type (first item)
        value     (second item)]
      (if (has-badending? value)
        item
        (if (chemical? value)
          [:chemical [word-type value]]
          item))))

(defn handle-word-0
  [item]
  [:handle-word {:item item}])

(defn handle-rule2 [item]
  (let [result (handle-word-1 item)]
    (if (= (first result) :chemical)
      [:chemical [:rule2 item]]
      [:word item])))

(defn handle-rule3 [item]
  [:handle-rule3 {:item item}])

(defn handle-rule4 [item]
  [:handle-rule4 {:item item}])

(defn handle-terms [parse-tree]
  (map (fn [el] 
         (let [term-class (first el)
               item      (second el)]
           (case term-class 
             :word  (handle-word-1 item)
             :rule2 (handle-rule2 item)
             :rule3 (handle-rule3 item)
             :rule4 (handle-rule4 item)
             :stop el
             :ws el)))
         (rest (second parse-tree))))
   

(defn process-parse-tree [tree] )
