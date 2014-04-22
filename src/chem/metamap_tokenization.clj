(ns chem.metamap-tokenization
  (:require [chem.span-utils]))

;; This module contains functions which emulate MetaMap's tokenization
;; with extra functions for dealing with chemicals.

(defn tokenize-original
  "An attempt to replicate tokenization regime in lexicon/function/tokenize.c
     text:  text, possible multi-word to be tokenized.
     tokenize-style: style of tokenization 
             2 : strip whitespace
             0 : keep whitespace"
  ([text] (tokenize-original text 0))
  ([text tokenize-style]
     (if (= tokenize-style 2)
       (re-seq #"\w+|[\(\)!@#$%^&*\+\=\-\_\[\]\{\}\.\,\?\/\']" text)
       (re-seq #"\w+|\W" text))))

(defn tokenize 
  "An attempt to replicate tokenization regime in lexicon/function/tokenize.c
     text:  text, possible multi-word to be tokenized.
     tokenize-style: style of tokenization 
       0 : keep whitespace
       2 : strip whitespace
       3 : strip whitespace, don't break on dashes '-'
       4 : strip whitespace, don't break on dashes '-' or parens
       5 : strip whitespace, preserve IUPAC chemicals (aggressive)
       6 : strip whitespace, preserve IUPAC chemicals, remove trailing commas. (aggressive)"
  ([text] (tokenize text 0))
  ([text tokenize-style]
     (cond
         (= tokenize-style 0) (re-seq #"\w+|\W" text)
         (= tokenize-style 2) (re-seq #"\w+|[\(\)!@#$%^&*\+\=\-\_\[\]\{\}\.\,\?\/\']" text)
         (= tokenize-style 3) (re-seq #"[\w\-]+|[\(\)!@#$%^&*\+\=\\_\[\]\{\}\.\,\?\/\']" text)
         (= tokenize-style 4) (re-seq #"[\w\-\(\)]+|[!@#$%^&*\+\=\\_\[\]\{\}\.\,\?\/\']" text)
         (= tokenize-style 5) (re-seq #"[\w*\-\(\)\,]+|[!@#$%^&\+\=\\_\[\]\{\}\.\?\/\']" text)
         (= tokenize-style 6) (reduce (fn [newtokenlist token]
                                        (if (= (last token) \,)
                                          (conj (conj newtokenlist
                                                      (subs token 0 (dec (count token))))
                                                      ",") ; split out trailing commas
                                          (conj newtokenlist token)))
                                        [] (re-seq #"[\w*\-\(\)\,]+|[!@#$%^&\+\=\\_\[\]\{\}\.\?\/\']" text)))))

(defn tokenize-no-ws [text]
  (tokenize text 2))

(defn tokenize-no-ws-keep-dashes [text]
  (tokenize text 3))

(defn tokenize-chemicals [text]
  (tokenize text 4))

(defn tokenize-chemicals-aggressive [text]
  (tokenize text 6))

;; compiled regular expressions
(def wspattern (re-pattern #"^\s$"))
(def anpattern (re-pattern #"^[A-Za-z]+[0-9]+$"))
;; (def pnpattern (re-pattern pattern))       ; using string.punctuation
(def ucpattern (re-pattern #"^[A-Z][A-Z0-9]+$"))
(def lcpattern (re-pattern #"^[a-z]+$"))
(def icpattern (re-pattern #"^[A-Za-z]+$"))
(def nupattern (re-pattern #"^[0-9]+$"))
(def pnpattern (re-pattern #"^[\(\)!@#$%^&*\+\=\-\_\[\]\{\}\.\,\?\/\']+$"))

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
   (re-find wspattern token) "ws"
   (re-find anpattern token) "an"
   (re-find ucpattern token) "uc"
   (re-find lcpattern token) "lc"
   (re-find icpattern token) "ic"
   (re-find nupattern token) "nu"
   (re-find pnpattern token) "pn"
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

(defn add-spans [atokenlist]
  (loop [i 0 
         tokenlist atokenlist 
         newtokenlist []]
    (if (empty? tokenlist)
        newtokenlist
        (let [token    (first tokenlist)
              tokenend (+ i (count (:text token)))]
          (recur tokenend
                 (rest tokenlist) 
                 (conj newtokenlist (conj token {:span {:start i, :end tokenend}})))))))

(defn classify-tokens [tokenlist]
  (map (fn [token]
         (conj token {:class (classify-token (:text token))}))
       tokenlist))

(defn analyze-text [text]
  "Perform lexical analysis of input text."
  (-> text
      tokenize
      make-map-tokenlist
      classify-tokens
      add-spans))

(defn analyze-text-no-ws [text]
  "Perform lexical analysis of input text."
  (-> text
      tokenize-no-ws
      make-map-tokenlist
      classify-tokens
      add-spans))

(defn analyze-text-no-ws-keep-dashes [text]
  "Perform lexical analysis of input text."
  (-> text
      tokenize-no-ws-keep-dashes
      make-map-tokenlist
      classify-tokens
      add-spans))

(defn analyze-text-chemicals [text]
  "Perform lexical analysis of input text."
  (-> text
      tokenize-chemicals
      make-map-tokenlist
      classify-tokens
      add-spans))

(defn analyze-text-chemicals-aggressive [text]
  "Perform lexical analysis of input text."
  (-> text
      tokenize-chemicals-aggressive
      make-map-tokenlist
      classify-tokens
      add-spans))

(defn generate-position-map-of-targetset [document-text targetset]
  (reduce
   (fn [newtargetmap targettext]
     (let [targetindex (.indexOf document-text targettext)]
       (if (>= targetindex 0)
         (if (contains? newtargetmap targetindex)
           (when (< (count (newtargetmap targetindex)) (count targettext))
             (assoc newtargetmap targetindex targettext)) ;replace string if new string is longer
           (assoc newtargetmap targetindex targettext)))))
   {} targetset))

(defn generate-spanlist-of-targetset [document-text targetset]
  (map
   (fn [targettext]
     (let [start (.indexOf document-text targettext)
           end (+ start (count targettext))]
           {:span {:start start
                   :end end}
            :text targettext}))
   targetset))

(defn subsume-tokens-in-targetset [document-text tokenlist targetset]
  ^{:doc "Merge target list spans with tokenlist, remove any subsumed tokens."}
  (let [targetset-spanlist (generate-spanlist-of-targetset document-text targetset)
        spanlist (map #(select-keys (:span %) [:start :end])
                      (concat targetset-spanlist tokenlist))
        subsumed-spanlist (chem.span-utils/subsume-spans spanlist)]
    (sort-by #(-> % :span :start)
             (filter #(not (nil? %))
                     (map (fn [span]
                            (if (> (:start span) -1)
                              {:span (select-keys span [:start :end])
                               :text (subs document-text (:start span) (:end span))}))
                          subsumed-spanlist)))))

(defn expand-token [token]
  ^{:doc "Expand text of token to tokenlist"}
  (let [tokenlist (tokenize-no-ws (:text token))]
    (if (> (count tokenlist) 1)
      (conj token (hash-map :text-tokens tokenlist))
      token)))

(defn expand-tokens [tokenlist]
  (map expand-token tokenlist))
  
(defn analyze-text-chemicals-using-targetset [text targetset]
  "Perform lexical analysis of input text."
  (expand-tokens
   (subsume-tokens-in-targetset text (-> text
                                         tokenize
                                         make-map-tokenlist
                                         classify-tokens
                                         add-spans)
                                targetset)))

(defn gen-token-annotations [text]
  (let [tokens (analyze-text text)]
    { :annotations tokens }))

(defn tokenize-text-utterly [fieldtext tokfields]

  )

(defn tokenize-one-field-utterly [] )

