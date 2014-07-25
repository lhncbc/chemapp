(ns chem.partial
  (:require [clojure.set])
  (:require [chem.dictionaries :as dictionaries])
  (:require [chem.span-utils :as span-utils])
  (:require [chem.rules :as rules])
  (:use [chem.utils])
  (:use [chem.annotations])
  (:gen-class))

;; Using IUPAC prefix, infix, and suffix dictionaries find chemical names in input text

(defn remove-terms-with-badendings [annotation-list]
  (filter 
   #(not (rules/has-badending? (:text %)))
   annotation-list))

(defn make-annotations [fragment text spanlist]
  (map (fn [span]
         (hash-map
          :fragment fragment
          :text (.substring text (span :start) (span :end))
          :span span))
       spanlist))

(defn consolidate-adjacent-annotations 
  ^{:doc "Consolidate adjacent chemical annotations

   I.E. {:text \"polyethylene\"} {:text \"glycol\"} 
        -> {:text \"polyethylene glycol\"}" }
  ([first-annot second-annot fragment-keyword]
     (hash-map :span {:start (:start (:span first-annot))
                      :end   (:end   (:span second-annot))}
               :text  (str (:text first-annot) " "
                           (:text second-annot))
               :fragmentlist [(fragment-keyword first-annot) 
                              (fragment-keyword second-annot)]
               :origin [first-annot second-annot]))
  ([first-annot second-annot]
     (consolidate-adjacent-annotations first-annot second-annot :fragment)))

(defn consolidate-annotations
  ^{:doc "find occurences of chemicals separated by one space and then consolidate them.
   I.E. ... chemical chemical ... becomes ... chemical ..." }
  ([annotation-list fragment-keyword]
     (if (> (count annotation-list) 1)
       (loop [oldlist (vec (sort-by #(:start (:span %)) annotation-list))
              newlist []]
         (let [first-annot (first oldlist)
               second-annot (second oldlist)]
           (cond 
            (<= (count oldlist) 1) newlist
            :else (if (= (inc (:end (:span first-annot))) (:start (:span second-annot)))
                    (recur (subvec oldlist (min 3 (count oldlist)))
                           (conj newlist (consolidate-adjacent-annotations
                                          first-annot second-annot fragment-keyword)))
                    (recur (subvec oldlist 1)
                           (conj newlist first-annot))))))
    annotation-list))
  ([annotation-list]
     (consolidate-annotations annotation-list :fragment)))

(defn get-fragment-annotations-using-dictionary [dictionary text]
  (sort-by first 
   (reduce (fn [newset fragment]
             (clojure.set/union newset (set 
                                        (span-utils/find-fragment text fragment))))
           #{} dictionary)))

(defn get-fragments-from-spans [document spanlist]
  (set (map (fn [span]
              (.substring document (span :start) (span :end)))
            spanlist)))

(defn get-annotations-using-dictionary
  "Generate annotations using supplied dictionary."
  ([dictionary text]
     (get-annotations-using-dictionary 
      dictionary span-utils/find-term-spanlist-with-target text))
  ([dictionary spanlist-fn text]
     (sort-by #(:start (:span %))
              (reduce (fn [newset span]
                        (clojure.set/union newset (set (make-annotations
                                                        (subs text (span :start) (span :end)) 
                                                        text
                                                        (list (span-utils/find-bounds-of-string text (span :start)))))))
                      #{} (get-fragment-annotations-using-dictionary dictionary text)))))

(defn get-annotations-using-dictionary-v1
  "Generate annotations using supplied dictionary."
  ([dictionary text]
     (get-annotations-using-dictionary dictionary span-utils/find-term-spanlist-with-target text))
  ([dictionary spanlist-fn text]
     (sort-by #(:start (:span %))
              (reduce (fn [newset fragment]
                        (clojure.set/union newset
                                           (set 
                                            (remove-terms-with-badendings
                                              (make-annotations
                                               fragment text
                                               (spanlist-fn text fragment))))))
                      #{} dictionary))))

(defn match
  "Partial match using fragments with bounds expansion"
  ([document dictionary]
     (let [annotations (consolidate-annotations
                        (get-annotations-using-dictionary
                         dictionary document))]
       { :annotations annotations }))
  ([document]
     (match document dictionaries/fragment-dictionary)))

(defn prefix-match [document]
  "Partial match using fragments with bounds expansion"
  (let [annotations (consolidate-annotations
                     (get-annotations-using-dictionary
                      dictionaries/prefix-dictionary
                      span-utils/find-prefix-spanlist-with-target
                      document)
                     :prefix)]
    { :annotations annotations }))

(defn suffix-match [document]
  "Partial match using fragments with bounds expansion"
  (let [suffix-dictionary (clojure.set/union dictionaries/fragment-suffix-dictionary 
                                             dictionaries/suffix-dictionary)
        annotations (consolidate-annotations
                     (get-annotations-using-dictionary
                      suffix-dictionary
                      span-utils/find-suffix-spanlist-with-target
                      document)
                     :suffix)]
    { :annotations annotations }))

(defn prefix-or-suffix-match [document]
  "Partial match using fragments with bounds expansion"
  (let [suffix-dictionary (clojure.set/union
                           dictionaries/fragment-suffix-dictionary 
                           dictionaries/suffix-dictionary)
        annotations 
        (sort-by #(:start (:span %))
                 (concat         
                  (get-annotations-using-dictionary
                   dictionaries/prefix-dictionary
                   span-utils/find-prefix-spanlist-with-target
                   document)
                  (get-annotations-using-dictionary
                   suffix-dictionary
                   span-utils/find-suffix-spanlist-with-target
                   document)
                  ))
        ]
    { :annotations annotations}))

(defn fragment-match
  ([document dictionary]
     (let [annotations (get-fragment-annotations-using-dictionary
                        dictionary document)]
       { :annotations annotations }))
  ([document]
     (fragment-match document dictionaries/fragment-dictionary)))

(defn record-to-sldiwi [record engine-keyword]
  "Convert annotations for engine specified by keyword to single line demlimited input with docid (sldiwi)."
  (set 
   (concat 
    (map (fn [annotation]
           (list (:docid record) (utf8-to-ascii (:text annotation))))
         (-> record engine-keyword :title-result :annotations))
    (map (fn [annotation]
           (list (:docid record) (utf8-to-ascii (:text annotation))))
         (-> record engine-keyword :abstract-result :annotations)))))

