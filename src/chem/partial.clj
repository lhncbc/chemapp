(ns chem.partial
  (:require [chem.dictionaries :as dictionaries])
  (:require [chem.span-utils :as span-utils]))

(defn make-annotations [fragment text spanlist]
  (map (fn [span]
         (hash-map
          :fragment fragment
          :term (.substring text (span :start) (span :end))
          :span span))
       spanlist))

(defn get-annotations-using-dictionary [dictionary text]
  (sort-by first 
   (reduce (fn [newset fragment]
             (clojure.set/union newset (set 
                                        (make-annotations
                                         fragment text
                                          (span-utils/find-term-spanlist-with-target text fragment)))))
           #{} dictionary)))

(defn get-fragment-annotations-using-dictionary [dictionary text]
  (sort-by first 
   (reduce (fn [newset fragment]
             (clojure.set/union newset (set 
                                        (span-utils/find-fragment text fragment))))
           #{} dictionary)))

(defn get-spans-from-annotations [annotationlist]
  (set
   (flatten
    (map (fn [annotation]
           (annotation :span))
         annotationlist))))

(defn get-terms-from-annotations [annotationlist]
  (set
   (map (fn [annotation]
          (annotation :term))
        annotationlist)))

(defn get-fragments-from-spans [document spanlist]
  (set (map (fn [span]
              (.substring document (span :start) (span :end)))
            spanlist)))

(defn partial-match [document]
  (let [annotations (get-annotations-using-dictionary
                     dictionaries/fragment-dictionary document)]
    {:spans (get-spans-from-annotations annotations)
     :annotations annotations
     :matched-terms (get-terms-from-annotations annotations)}))

(defn fragment-match [document]
  (let [annotations (get-fragment-annotations-using-dictionary
                     dictionaries/fragment-dictionary document)]
    {:spans annotations
     :annotations annotations
     :matched-terms (get-fragments-from-spans document annotations)}))
