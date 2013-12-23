(ns chem.metamap-tokenization)

(defn tokenize [term tokenize-style]
  "An attempt to replicate tokenization regime in lexicon/function/tokenize.c
     term:  term, possible multi-word to be tokenized.
     tokenize-style:  style of tokenization currently strip white
     is tokenize-style = 2"
  (if (= tokenize-style 2)
    (re-seq #"\w+|[\(\)!@#$%^&*\+\=\-\_\[\]\{\}\.\,\?\/\']" term)
    (re-seq #"\w+|\W+" term)))

;; compiled regular expressions
(def wsprog (re-pattern #"^\s$"))
(def anprog (re-pattern #"^[A-Za-z]+[0-9]+$"))
;; (def pnprog (re-pattern pattern))       ; using string.punctuation
(def ucprog (re-pattern #"^[A-Z][A-Z0-9]+$"))
(def lcprog (re-pattern #"^[a-z]+$"))
(def icprog (re-pattern #"^[A-Za-z]+$"))
(def nuprog (re-pattern #"^[0-9]+$"))


(defn classify-token [token]
  "What are the classes?:
    <ws> - whitespace
    <an> - alphanumeric
    <pn> - punctuation
    <uc> - uppercase
    <ic> - ignore case
    <lc> - lowercase
    <nu> - numeric
    <un> - unknown"
  (cond 
   (re-find wsprog token) "ws"
   (re-find anprog token) "an"
   (re-find ucprog token) "uc"
   (re-find lcprog token) "lc"
   (re-find icprog token) "ic"
   (re-find nuprog token) "nu"
   :else "unknown")
  )

(defn make-map-tokenlist [tokenlist]
  "Convert string tokens to map tokens, each token of the form:
    {:text <text>}"
  (map #(hash-map :text %) tokenlist))

(defn add-positional-info [atokenlist]
  (loop [i 0 
         tokenlist atokenlist 
         newtokenlist []]
    (let [token  (first tokenlist)
          tokentext {:text token}]
      (if (empty? tokenlist)
        newtokenlist
        (recur (+ i (count tokentext)) 
               (rest tokenlist) 
               (conj newtokenlist (conj token {:position i})))))))

(defn classify-tokens [tokenlist]
  (map (fn [token]
         (conj token {:class (classify-token (:text token))}))
       tokenlist))

(defn tokenize-text-utterly [fieldtext tokfields]

  )

(defn tokenize-one-field-utterly [] )




