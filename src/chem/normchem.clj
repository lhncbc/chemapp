(ns chem.normchem
  (:require [chem.mongodb :as chemdb])
  (:require [chem.metamap-tokenization :as mm-tokenization])
  (:require [chem.span-utils :as span-utils])
  (:require [chem.stopwords :as stopwords]))

(defn tokenize-document [document]
  (let [tokenlist (mm-tokenization/tokenize document 0)]
    (loop [i 0 j 0
           annotlist '()]
      (if (< j (count tokenlist))
        (recur (+ i (count (nth tokenlist j)))
               (inc j)
               (conj annotlist (hash-map 
                                   :start i
                                   :end (+ i (count (nth tokenlist j)))
                                   :text (nth tokenlist j))))
        annotlist))))

(defn suppconcept-lookup [document]
   (filter #(not (nil? %))
           (map (fn [token]
                  (when (> (count (:text token)) 2)
                    (let [result (first (chemdb/lookup :normchem (.toLowerCase (:text token))))]
                      (when result 
                        (hash-map
                         :start (token :start)
                         :end   (token :end)
                         :text  (token :text)
                         :dui   (result :value))))))
                (tokenize-document document))))

(defn suppconcept-lookup-finding-boundary [document]
  "set span to leading and following extent of term"
  (filter #(not (nil? %))
          (map (fn [token]
                 (when (> (count (:text token)) 2)
                   (let [result (first (chemdb/lookup :normchem (.toLowerCase (:text token))))]
                     (when result 
                       (let [bounds (span-utils/find-bounds-of-term document (token :start))]
                         (hash-map
                          :start (bounds :start)
                          :end   (bounds :end)
                          :text  (.substring document (bounds :start) (bounds :end))
                          :dui   (result :value)))))))
               (tokenize-document document))))

(defn get-spans [annotation-list]
  (map #(hash-map :start (% :start)
                    :end  (% :end))
       annotation-list))
           
(defn process-document [document]
  (let [annotation-list (suppconcept-lookup-finding-boundary document)]
    (hash-map
        :annotations annotation-list
        :spans (map #(hash-map :start (% :start)
                               :end  (% :end))
                    annotation-list)
        :matched-terms (map #(% :text)
                            annotation-list))))