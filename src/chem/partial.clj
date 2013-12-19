(ns chem.partial
  (:require [chem.dictionaries :as dictionaries])
  (:require [chem.span-utils :as span-utils])
  (:require [chem.rules :as rules])
)

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

(defn consolidate-adjacent-annotations [first-annot second-annot]
  "Consolidate adjacent chemical annotations

   I.E. {:text \"polyethylene\"} {:text \"glycol\"} 
        -> {:text \"polyethylene glycol\"}"
  (hash-map :span {:start (:start (:span first-annot))
                   :end   (:end   (:span second-annot))}
            :text  (str (:text first-annot) " "
                        (:text second-annot))
            :fragmentlist [(:fragment first-annot) 
                           (:fragment second-annot)]
            :origin [first-annot second-annot]))

(defn consolidate-annotations [annotation-list]
  "find occurences of chemicals separated by one space and then consolidate them.
   I.E. ... chemical chemical ... becomes ... chemical ..."
  (if (> (count annotation-list) 1)
    (loop [oldlist (vec (sort-by #(:start (:span %)) annotation-list))
           newlist []]
      (let [first-annot (first oldlist)
            second-annot (second oldlist)]
        (cond 
         (<= (count oldlist) 1) newlist
         :else (if (= (inc (:end (:span first-annot))) (:start (:span second-annot)))
                 (recur (subvec oldlist 2) (conj newlist (consolidate-adjacent-annotations first-annot second-annot)))
                 (recur (subvec oldlist 1) (conj newlist first-annot))))))
    annotation-list))

(defn get-annotations-using-dictionary [dictionary text]
  (sort-by #(:start (:span %))
   (reduce (fn [newset fragment]
             (clojure.set/union newset
                                (set 
                                 (remove-terms-with-badendings
                                   (make-annotations
                                    fragment text
                                    (span-utils/find-term-spanlist-with-target text fragment))))))
           #{} dictionary)))

(defn get-fragment-annotations-using-dictionary [dictionary text]
  (sort-by first 
   (reduce (fn [newset fragment]
             (clojure.set/union newset (set 
                                        (span-utils/find-fragment text fragment))))
           #{} dictionary)))

(defn get-spans-from-annotations [annotationlist]
  (sort-by #(:start %)
           (set
            (flatten
             (map (fn [annotation]
                    (annotation :span))
                  annotationlist)))))

(defn get-terms-from-annotations [annotationlist]
  (set
   (map (fn [annotation]
          (annotation :text))
        annotationlist)))

(defn get-fragments-from-spans [document spanlist]
  (set (map (fn [span]
              (.substring document (span :start) (span :end)))
            spanlist)))

(defn partial-match [document]
  (let [annotations
        (consolidate-annotations
         (get-annotations-using-dictionary
          dictionaries/fragment-dictionary document))]
    {:spans (get-spans-from-annotations annotations)
     :annotations annotations
     :matched-terms (get-terms-from-annotations annotations)}))

(defn fragment-match [document]
  (let [annotations (get-fragment-annotations-using-dictionary
                     dictionaries/fragment-dictionary document)]
    {:spans annotations
     :annotations annotations
     :matched-terms (get-fragments-from-spans document annotations)}))
