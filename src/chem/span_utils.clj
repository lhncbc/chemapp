(ns chem.span-utils
  (:require [chem.dictionaries :as dictionaries])
  (:require [chem.stopwords :as stopwords])
  (:gen-class))

(defn is-punctuation [ch]
  (contains? #{ \- \. \| \,} ch))

;;  (let [spantext (.toLowerCase (.substring text (span :start) (span :end)))]

(defn remove-encasulating-parens [spaninfo] 
 "if span text is '(text)' -> 'text' (compute new span)"
 (let [text (spaninfo :text)
       span (spaninfo :span)]
   (if (and (= (nth text (span :start)) \()
            (= (nth text (dec (span :end))) \)))
     (hash-map :span (hash-map :start (inc (span :start))
                               :end (dec (span :end)))
               :text text)
     spaninfo)))

(defn remove-leading-punctuation [spaninfo]
  "if span text is '<punctuation>text' -> 'text'"
  (let [text (spaninfo :text)
        span (spaninfo :span)]
    (if (is-punctuation (nth text (span :start)))
      (hash-map :span (hash-map :start (inc (span :start))
                                :end (span :end))
                :text text)
      spaninfo)))

(defn remove-trailing-punctuation [spaninfo]
  "if span text is 'text<punctuation>' -> 'text'"
  (let [text (spaninfo :text)
        span (spaninfo :span)]
  (if (is-punctuation (nth text (dec (span :end))))
    (hash-map :span (hash-map :start (span :start)
                              :end (dec (span :end)))
              :text text)
    spaninfo)))

(defn remove-leading-paren [spaninfo]
  "if span text is '(text' -> 'text'"
  (let [text (spaninfo :text)
        span (spaninfo :span)]
    (if (= \( (nth text (span :start)))
      (if (= (.indexOf (.substring text (:start span) (:end span)) ")") -1)
        (hash-map :span (hash-map :start (inc (span :start))
                                  :end (span :end))
                  :text text)
        spaninfo)
      spaninfo)))

(defn remove-trailing-paren [spaninfo]
  "if span text is 'text)' -> 'text'"
  (let [text (spaninfo :text)
        span (spaninfo :span)]
  (if (= \) (nth text (dec (span :end))))
    (if (= (.indexOf (.substring text (:start span) (:end span)) "(") -1)
      (hash-map :span (hash-map :start (span :start)
                                :end (dec (span :end)))
                :text text)
      spaninfo)
    spaninfo)))

(defn remove-trailing-component-stopword [spaninfo] 
  " if span text is 'text-<component stopword' -> 'text'"
 (let [text (spaninfo :text)
       span (spaninfo :span)
       spantext (.toLowerCase (.substring text (span :start) (span :end)))
       fields (vec (.split spantext "\\-"))
       targettext (last fields)]
   (if (and (> (count fields) 1)
            (contains? stopwords/component-stopwords targettext))
     (hash-map :span (hash-map :start (span :start) 
                               :end (- (span :end) (count targettext)))
               :text text)
     spaninfo )))

(defn remove-stopwords [spaninfo]
  "if span text is '<stopword>' -> nil"
  (let [text (spaninfo :text)
        span (spaninfo :span)]
  (if (contains? stopwords/stopwords (.toLowerCase (.substring text (span :start) (span :end))))
    (hash-map :span nil :text text)
    spaninfo)))

(defn check-span [span text]
  (:span 
   (let [spaninfo (hash-map :span span :text text)]
     (-> spaninfo
         remove-encasulating-parens
         remove-trailing-component-stopword
         remove-trailing-paren
         remove-leading-paren
         remove-trailing-punctuation
         remove-leading-punctuation
         remove-stopwords))))

(defn check-spanlist [spanlist text]
  (map (fn [span]
         (check-span span text))
       spanlist))

(defn find-bounds-of-string [text index]
  "Find begining and end of term that has a character at index using
   whitespace as delimitors, punctuation other that whitespace is
   considered part of term.  Return begining and ending indexes as a
   vector."
  (hash-map
      :start (loop [i index]
               (cond
                (< i 0) 0
                (= (nth text i) \space) (+ i 1)
                (= (nth text i) \return) (+ i 1)
                (= (nth text i) \newline) (+ i 1)
                :else (recur (dec i) )))
      :end (loop [i index]
             (cond
              (>= i (count text)) (count text)
              (= (nth text i) \space) i
              (= (nth text i) \return) i
              (= (nth text i) \newline) i
              :else (recur (inc i) )))))

(defn find-term-span-with-target [text target]
  (let [index (.indexOf text target)]
    (when (> index -1)
      (find-bounds-of-string text index))))

(defn find-term-spanlist-with-target [text target]
  "Find begining and end of terms that has a character at index using
   whitespace as delimitors, punctuation other that whitespace is
   considered part of term.  Return list of spans for terms as a
   vector of vector pairs."
  (loop [spans []
         index (.indexOf text target)]
      (if (>= index 0)
        (let [newspan (check-span (find-bounds-of-string text index) text)]
          (if (nil? newspan)
            (recur spans (.indexOf text target (+ index (count target))))
            (recur (conj spans newspan) (.indexOf text target (:end newspan)))))
        spans)))


(defn find-infix-spanlist-with-target [text target]
  "Find begining and end of terms that has a character at index using
   whitespace as delimitors, punctuation other that whitespace is
   considered part of term.  Return list of spans for terms as a
   vector of vector pairs."
  (loop [spans []
         index (.indexOf text target)]
      (if (>= index 0)
        (let [newspan (check-span (find-bounds-of-string text index) text)]
          (if (nil? newspan)
            (recur spans (.indexOf text target (+ index (count target))))
            (recur (conj spans newspan) (.indexOf text target (:end newspan)))))
        spans)))

(defn find-prefix-spanlist-with-target [text target]
  "Find begining and end of terms that has a character at index using
   whitespace as delimitors, punctuation other that whitespace is
   considered part of term.  Return list of spans for terms as a
   vector of vector pairs."
  (loop [spans []
         index (.indexOf text target)]
      (if (>= index 0)
        (let [newspan (check-span (find-bounds-of-string text index) text)]
          (if (nil? newspan)
            (recur spans (.indexOf text target (+ index (count target))))
            (recur (conj spans newspan) (.indexOf text target (:end newspan)))))
        spans)))

(defn find-suffix-spanlist-with-target [text target]
  "Find begining and end of terms that has a character at index using
   whitespace as delimitors, punctuation other that whitespace is
   considered part of term.  Return list of spans for terms as a
   vector of vector pairs."
  (loop [spans []
         index (.indexOf text target)]
      (if (>= index 0)
        (let [newspan (check-span (find-bounds-of-string text index) text)]
          (if (nil? newspan)
            (recur spans (.indexOf text target (+ index (count target))))
            (recur (conj spans newspan) (.indexOf text target (:end newspan)))))
        spans)))

(defn find-fragment [text target]
  "find target-text (a fragment) in supplied text."
  (loop [spans []
         index (.indexOf text target)]
      (if (and (>= index 0) (< index (count text)))
        (let [newspan (hash-map :start index :end (+ index (count target)))]
          (recur (conj spans newspan) (.indexOf text target (:end newspan))))
        spans)))