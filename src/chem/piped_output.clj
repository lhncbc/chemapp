(ns chem.piped-output
  (:require [clojure.string :as string]))

(defn consolidate-terms
  "Consolidate information about terms into mesh id and associated spans."
  [resultmap]
  (reduce (fn [newmap annotation]
            (let [key (:text annotation)]
              (if (contains? newmap key)
                (assoc newmap key (hash-map :meshid (:meshid annotation) 
                                            :spans (conj (:spans (newmap key))
                                                         (:span annotation))))
                (assoc newmap key (hash-map :meshid (:meshid annotation)
                                            :spans (vector (:span annotation)))))))
          {} (:annotations resultmap)))

(defn piped-output
  "Output contents of resultmap in piped separated form."
  [resultmap]
  (if (empty? (:spans resultmap))
    ""
    (let [idmap (reduce (fn [newmap annot]
                          (if (not (empty? (:meshid annot)))
                            (assoc newmap (string/lower-case (:text annot))
                                   (:meshid annot))
                            newmap))
                        {} (:annotations resultmap))]
      (apply str
             (map (fn [[k v]]
                    (if (nil? k)
                      ""
                      (string/join "|" (list k (idmap (string/lower-case k))
                                             (string/join ";"
                                                          (map #(format "%d,%d" 
                                                                        (:start %)
                                                                        (:end %))
                                                               (:spans v)))
                                             "\n"))))
                  (consolidate-terms resultmap))))))
