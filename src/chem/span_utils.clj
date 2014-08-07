(ns chem.span-utils
  (:require [chem.annotations :as annot])
  (:require [chem.dictionaries :as dictionaries])
  (:require [chem.stopwords :as stopwords])
  (:gen-class))

(defn is-punctuation [ch]
  (contains? #{ \- \. \| \, \;} ch))

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
  "Return valid span or nil if span does not meet criteria."
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

(defn is-at-beginning-of-span? [target span text]
  "Is the supplied target at the beginning of the span."
  (= 0 (.indexOf (subs text (:start span) (:end span)) target)))

(defn is-at-end-of-span? [target span text]
  "Is the supplied target at the end of the span."
  (let [spantext (subs text (:start span) (:end span))
        pos      (.indexOf spantext target)]
    (and (>= pos 0) (= (+ (count target) pos) (count spantext)))))

(defn non-chemical-delimiter [ch]
  (or (is-punctuation ch)
      (= ch \space) 
      (= ch \return)
      (= ch \newline)))

(defn is-not-chemical-punctuation [ch]
  (contains? #{ \[ \] \. \| \;} ch))  

(defn chemical-delimiter [ch]
  (or (is-not-chemical-punctuation ch)
      (= ch \space) 
      (= ch \return)
      (= ch \newline)))

(defn find-bounds-of-string
  "Find begining and end of term that has a character at index using
   punctuation and whitespace as delimitors by default.  Return
   begining and ending indexes as a vector.  One can supply a
   delimiter function to change the behavior of this function."
  ([text index delimiter-fn]
     (hash-map
      :start (loop [i index]
                    (cond
                     (< i 0) 0
                     (delimiter-fn (nth text i)) (inc i)
                     :else (recur (dec i) )))
      :end (loop [i index]
             (cond
              (>= i (count text)) (count text)
              (delimiter-fn (nth text i)) i
              :else (recur (inc i) )))))
  ([text index]
     (find-bounds-of-string text index chemical-delimiter)))

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
          (if (or (nil? newspan) (not (is-at-beginning-of-span? target newspan text)))
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
          (if (or (nil? newspan) (not (is-at-end-of-span? target newspan text)))
            (recur spans (.indexOf text target (+ index (count target))))
            (recur (conj spans newspan) (.indexOf text target (:end newspan)))))
        spans)))

(defn find-prefix-or-suffix-spanlist-with-target [text target]
  "Find begining and end of terms that has a character at index using
   whitespace as delimitors, punctuation other that whitespace is
   considered part of term.  Return list of spans for terms as a
   vector of vector pairs."
  (loop [spans []
         index (.indexOf text target)]
      (if (>= index 0)
        (let [newspan (check-span (find-bounds-of-string text index) text)]
          (if (or (not (nil? newspan))
                  (is-at-beginning-of-span? target newspan text)
                  (is-at-end-of-span? target newspan text))
            (recur (conj spans newspan) (.indexOf text target (:end newspan)))
            (recur spans (.indexOf text target (+ index (count target))))))
        spans)))

(defn find-fragment [text target]
  "find target-text (a fragment) in supplied text."
  (loop [spans []
         index (.indexOf text target)]
      (if (and (>= index 0) (< index (count text)))
        (let [newspan (hash-map :start index :end (+ index (count target)))]
          (recur (conj spans newspan) (.indexOf text target (:end newspan))))
        spans)))

(defn realize-spans [text spanlist]
  (map (fn [span] (subs text (:start span) (:end span))) spanlist))

(defn realize-spans-with-spaninfo [text spanlist]
  (map (fn [span] (list (subs text (:start span) (:end span)) span)) spanlist))

(defn concat-spans [record annotator-keyword-list result-keyword]
  (sort-by :start 
           (into [] 
                 (set (apply concat 
                             (map (fn [annotator-keyword]
                                    (annot/get-spans-from-annotations
                                     (-> record annotator-keyword result-keyword :annotations)))
                                  annotator-keyword-list))))))

;; if (start_1 >= start_n) and (end_1 <= end_n)
(defn is-span-subsumed? [span spanlist]
  (> (count 
      (filter (fn [tspan]
                (and (>= (:start span) (:start tspan))
                     (<= (:end span) (:end tspan))))
              spanlist))
     0))
  
;; If span is inside a span in newspanlist, discard it, else add it to
;; new spanlist.
;; Note: does not deal with non-subsuming overlapping spans.
(defn subsume-spans [spanlist]
  (if (empty? spanlist) 
    spanlist
    (loop [aspan (second spanlist)
           restspanlist (rest (rest spanlist))
           newspanlist [(first spanlist)]]
      (if (empty? restspanlist) 
        newspanlist
        (if (is-span-subsumed? aspan newspanlist)
          (recur (first restspanlist) (rest restspanlist) newspanlist)
          (recur (first restspanlist) (rest restspanlist) (conj newspanlist aspan)))))))
  
;; {:start 59, :end 67}
;; {:start 273, :end 284}
;; {:start 499, :end 507}
;; {:start 815, :end 823}
;; {:start 1169, :end 1179}
;; {:start 1184, :end 1205}
;; {:start 1192, :end 1205}  <-subsumed by previous span.
;; {:start 1260, :end 1266}
;; {:start 1380, :end 1391}
